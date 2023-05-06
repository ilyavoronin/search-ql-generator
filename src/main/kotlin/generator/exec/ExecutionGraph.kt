package generator.exec

import generator.GeneratedObject
import generator.lang.ast.*
import generator.scheme.ExtendedDefField
import generator.scheme.GeneratorScheme
import generator.scheme.ast.*
import java.lang.IllegalStateException
import kotlin.reflect.KCallable

fun interface ObjRetriever {
    fun retrieve(obj: GeneratedObject, intersectWith: Set<GeneratedObject>?): Set<GeneratedObject>
}

public interface BoolBuiltIn {
    public fun getBool(): Boolean
}

public interface StringBuiltIn {
    public fun getString(): String
}

public interface IntBuiltIn {
    public fun getInt(): Int
}

class ExecutionGraph(val scheme: GeneratorScheme, val genObjects: GeneratedObjects, val findQuery: FindQuery) {

    fun execute(order: ExecutionOrder): List<GeneratedObject> {
        val order = order.genExecOrder(root, sobj)
        for (ordElem in order) {
            idToNode[ordElem.nodeId]!!.execute(ordElem.type)
        }

        return (executionResult[root.id] as PathExecutionResult).bottomLevelObjects!!.toList()
    }

    fun interface ObjFilter {
        fun accepts(obj: GeneratedObject): Boolean
    }

    class ObjRevFilterBuilder(val f: (objf: ObjFilter) -> ObjFilter) {
        fun build(objf: ObjFilter): ObjFilter {
            return f(objf)
        }

        fun buildFinal(): ObjFilter {
            return f { true }
        }
    }

    class CombinedObjFilter(val objFilter: ObjRevFilterBuilder?, val objRetriever: ObjRetriever?) {
        fun filterObjects(objs: Set<GeneratedObject>?): Pair<Set<GeneratedObject>?, ObjRetriever?> {
            val resObjs = objFilter?.let { filterB ->
                val filter = filterB.buildFinal()
                objs?.filter { filter.accepts(it) }
            }?.toSet()
            return Pair(resObjs ?: setOf(), objRetriever)
        }

        companion object {
            fun buildFromAnd(c1: CombinedObjFilter, c2: CombinedObjFilter): CombinedObjFilter {
                val newFilter = if (c1.objFilter != null && c2.objFilter != null) {
                    ObjRevFilterBuilder {objf ->
                        val filterLeft = c1.objFilter.build(objf)
                        val filterRight = c2.objFilter.build(objf)

                        ObjFilter {
                            filterLeft.accepts(it) && filterRight.accepts(it)
                        }
                    }
                } else {
                    null
                }

                val newRetriever = if (c1.objRetriever != null || c2.objRetriever != null) {
                    ObjRetriever { obj, intersectWith ->
                        val left = c1.objRetriever?.retrieve(obj, intersectWith)
                        val right = c2.objRetriever?.retrieve(obj, intersectWith)

                        val addResLeft = c1.objFilter?.let {filterb ->
                            val filter = filterb.buildFinal()
                            right?.filter { filter.accepts(it) }
                        }
                        val addResRight = c2.objFilter?.let {filterb ->
                            val filter = filterb.buildFinal()
                            left?.filter { filter.accepts(it) }
                        }

                        val res = if (left != null && right != null) {
                            left.intersect(right).toMutableSet()
                        } else {
                            mutableSetOf()
                        }

                        addResRight?.let { res.addAll(it) }
                        addResLeft?.let { res.addAll(it) }

                        res
                    }
                } else {
                    null
                }

                return CombinedObjFilter(newFilter, newRetriever)
            }

            fun buildFromOr(c1: CombinedObjFilter, c2: CombinedObjFilter): CombinedObjFilter {
                val newFilter = if (c1.objFilter != null && c2.objFilter != null) {
                    ObjRevFilterBuilder {objf ->
                        val filterLeft = c1.objFilter.build(objf)
                        val filterRight = c2.objFilter.build(objf)

                        ObjFilter { obj -> filterLeft.accepts(obj) || filterRight.accepts(obj) }
                    }
                } else if (c1.objFilter != null || c2.objFilter != null) {
                    (c1.objFilter ?: c2.objFilter)!!
                } else {
                    null
                }

                val newRetriever = ObjRetriever { obj, intersectWith ->
                    val obj1 = c1.objRetriever?.retrieve(obj, intersectWith)
                    val obj2 = c2.objRetriever?.retrieve(obj, intersectWith)
                    obj1?.let { o1 -> obj2?.union(o1) ?: o1 }?: obj2 ?: setOf()
                }

                return CombinedObjFilter(newFilter, newRetriever)
            }
        }
    }


    sealed interface ExecutionResult

    private data class ObjExecutionResult(val objFilter: ObjFilter?, val objs: Set<GeneratedObject>?): ExecutionResult
    data class PathExecutionResult(val objRetriever: ObjRetriever?, val bottomLevelObjects: Set<GeneratedObject>?, val combinedObjFilter: CombinedObjFilter): ExecutionResult

    private val executionResult: MutableMap<Int, ExecutionResult> = mutableMapOf()

    val root: PathExecutionNode
    val sobj: Object
    val idToNode: MutableMap<Int, ExecutionNode>
    init {
        idToNode = mutableMapOf()
        sobj = scheme.getDefinition(findQuery.sobject.capitalize())!! as Object
        root = if (findQuery.inCond != null) {
            buildPathGraph(findQuery.inCond, null, sobj, findQuery.withCond?.let {buildCondGraph(sobj, it) }, listOf())
        } else {
            PathSubExecNode(sobj,
                ExtendedDefField(),
                mapOf(),
                buildCondGraph(sobj, findQuery.withCond!!),
                null,
                listOf()
            )
        }
    }

    fun getNode(id: Int): ExecutionNode? {
        return idToNode[id]
    }

    fun getResult(id: Int): ExecutionResult {
        return executionResult[id]!!
    }

    private var _last_id = 0
    abstract inner class ExecutionNode() {
        val id: Int
        private val children = mutableListOf<ExecutionNode>()
        init {
            id = _last_id
            idToNode[id] = this
            _last_id += 1
        }

        fun getChildren(): List<ExecutionNode> {
            return children
        }

        protected fun addChild(node: ExecutionNode) {
            children.add(node)
        }

        protected fun getChildResult(childNum: Int): ExecutionResult {
            return executionResult[children[childNum].id]!!
        }

        internal fun execute(t: ExecType) {
            executionResult[id] = executeInternal(t)
        }

        protected abstract fun executeInternal(t: ExecType): ExecutionResult
    }

    abstract inner class PathExecutionNode: ExecutionNode()

    abstract inner class ObjCondExecutionNode: ExecutionNode()

    inner class PathAndExecutionNode(l: PathExecutionNode, r: PathExecutionNode) : PathExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val left = getChildResult(0) as PathExecutionResult
            val right = getChildResult(1) as PathExecutionResult

            var commonIntersection = if (left.bottomLevelObjects != null && right.bottomLevelObjects != null) {
                left.bottomLevelObjects.intersect(right.bottomLevelObjects).toMutableSet()
            } else {
                mutableSetOf()
            }

            val (additionalLeftObjects, leftRetriever) = left.combinedObjFilter?.filterObjects(right.bottomLevelObjects) ?: Pair(null, null)
            val (additionalRightObjects, rightRetriever) = right.combinedObjFilter?.filterObjects(left.bottomLevelObjects) ?: Pair(null, null)

            additionalLeftObjects?.let { commonIntersection.addAll(it) }
            additionalRightObjects?.let { commonIntersection.addAll(it) }

            val rightIntersection = right.bottomLevelObjects?.minus(commonIntersection)
            val leftIntersection = left.bottomLevelObjects?.minus(commonIntersection)

            val newObjRetriever = if (leftRetriever == null && rightRetriever == null) {
                null
            } else {
                ObjRetriever { obj, intersectWith ->
                    val rightIntersection = if (rightIntersection == null || rightRetriever != null) {
                        null
                    } else {
                        intersectWith?.let { rightIntersection.intersect(it) } ?: rightIntersection
                    }
                    val objs1 = leftRetriever?.retrieve(obj, rightIntersection)

                    val leftIntersection = if (leftIntersection == null || leftRetriever != null) {
                        null
                    } else {
                        intersectWith?.let { leftIntersection.intersect(it) }
                    }
                    val objs2 = rightRetriever?.retrieve(obj, leftIntersection)

                    val res = if (objs1 != null && objs2 != null) {
                        objs1.intersect(objs2)
                    } else {
                        objs1 ?: objs2!!
                    }

                    res
                }
            }


            val newCombinedObjFilter = CombinedObjFilter.buildFromAnd(left.combinedObjFilter, right.combinedObjFilter)

            return PathExecutionResult(newObjRetriever, commonIntersection, newCombinedObjFilter)
        }
    }
    inner class PathOrExecutionNode(l: PathExecutionNode, r: PathExecutionNode) : PathExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val left = getChildResult(0) as PathExecutionResult
            val right = getChildResult(1) as PathExecutionResult

            val newObjs = left.bottomLevelObjects?.union(right.bottomLevelObjects ?: listOf())
            val newRetriever = ObjRetriever {obj, intersectWith ->
                val objs1 = left.objRetriever?.retrieve(obj, intersectWith) ?: setOf()
                val objs2 = right.objRetriever?.retrieve(obj, intersectWith) ?: setOf()

                objs1.union(objs2)
            }

            return PathExecutionResult(newRetriever, newObjs, CombinedObjFilter.buildFromOr(left.combinedObjFilter, right.combinedObjFilter))
        }
    }

    data class RootPathElem(
        val nodeId: Int,
        val method: GeneratedObjects.GetRevMethod?,
        val extField: ExtendedDefField,
        val modifiers: Map<String, ModValueType>,
    )

    private fun interface SeqObjFilter {
        fun filterAndAscend(obj: GeneratedObject): Set<GeneratedObject>
    }

    private fun filterObjectsBottomUp(objs: Collection<GeneratedObject>, filters: List<RootPathElem>): Set<GeneratedObject> {
        var finalFilter = SeqObjFilter { setOf(it) }
        for ((i, filterInfo) in filters.asReversed().withIndex()) {
            val calculatedRes = if (filterInfo.nodeId != -1) {
                getResult(filterInfo.nodeId) as ObjExecutionResult
            } else {
                ObjExecutionResult(null, null)
            }
            val copyFinalFilter = finalFilter
            finalFilter = SeqObjFilter {obj ->
                copyFinalFilter.filterAndAscend(obj).flatMap {obj ->
                    if (i == 0 || filterInfo.nodeId == -1 || calculatedRes.objs?.contains(obj) == true || calculatedRes.objFilter?.accepts(obj) == true) {
                        (filterInfo.method?.call(obj, filterInfo.extField, filterInfo.modifiers) ?: listOf(obj)) as List<GeneratedObject>
                    } else {
                        emptySet()
                    }
                }.toSet()
            }
        }

        return objs.filter { finalFilter.filterAndAscend(it).isNotEmpty() }.toSet()
    }

    inner class PathSubExecNode(
        val obj: Definition,
        val contextParent: ExtendedDefField,
        val modifiers: Map<String, ModValueType>,
        val cond: ObjCondExecutionNode?,
        val subPath: PathExecutionNode?,
        val rootPath: List<RootPathElem>?
    ) : PathExecutionNode() {
        init {
            cond?.let{ addChild(it) }
            subPath?.let { addChild(it) }
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            return when {
                cond == null && subPath == null -> {
                    if (obj.name != sobj.name) {
                        throw IllegalStateException()
                    }

                    val kmethod = genObjects.getDefMethod(contextParent.parent!!.name, contextParent.memName)

                    val retriever = if (!contextParent.isMany) {
                        ObjRetriever{ obj, intersectWith ->
                            val res = kmethod.call(obj, contextParent, modifiers)
                            val currObj = res as GeneratedObject
                            if (intersectWith != null && !intersectWith.contains(currObj)) {
                                setOf()
                            } else {
                                setOf(currObj)
                            }
                        }
                    } else {
                        ObjRetriever { obj, intersectWith ->
                            val res = kmethod.call(obj, contextParent, modifiers) as List<GeneratedObject>
                            if (intersectWith == null) {
                                res.toSet()
                            } else {
                                res.filter { intersectWith.contains(it) }.toSet()
                            }
                        }
                    }

                    val combinedFilter = if (contextParent.isRev) {
                        val revMethod = genObjects.getRevDefMethod(obj.name, contextParent.parent.name)
                        CombinedObjFilter(ObjRevFilterBuilder {objf ->
                            ObjFilter { obj ->
                                revMethod.call(obj, contextParent, modifiers).any { objf.accepts(it) }
                            }
                        }, null)
                    } else {
                        CombinedObjFilter(null, retriever)
                    }

                    PathExecutionResult(retriever, setOf(), combinedFilter)
                }
                cond != null && subPath == null -> {
                    val res = getChildResult(0) as ObjExecutionResult

                    return when (t) {
                        ExecType.FilterCalc -> {
                            val getMethod = genObjects.getDefMethod(contextParent.parent!!.name, contextParent.memName)
                            val resObjs = res.objs?.let { filterObjectsBottomUp(it, rootPath!!)}
                            if (res.objFilter != null) {
                                val retr = ObjRetriever { obj, intersectWith ->
                                    val subObj = getMethod.call(obj, contextParent, modifiers)
                                    var subObjs: List<GeneratedObject>
                                    if (contextParent.isMany) {
                                        subObjs = subObj as List<GeneratedObject>
                                        if (intersectWith != null) {
                                            subObjs = subObjs.filter { intersectWith.contains(it) && res.objFilter.accepts(it)}
                                        } else {
                                            subObjs = subObjs.filter { res.objFilter.accepts(it) }
                                        }
                                    } else {
                                        val subObj = subObj as GeneratedObject
                                        subObjs = if (intersectWith != null && !intersectWith.contains(subObj)) {
                                            listOf()
                                        } else if (res.objFilter.accepts(subObj)) {
                                            listOf(subObj)
                                        } else {
                                            listOf()
                                        }
                                    }
                                    subObjs.toSet()
                                }

                                val combinedFilter = if (contextParent.isRev) {
                                    val revMethod = genObjects.getRevDefMethod(obj.name, contextParent.parent.name)
                                    CombinedObjFilter(ObjRevFilterBuilder { objf ->
                                        ObjFilter {obj ->
                                            res.objFilter.accepts(obj) &&
                                                    revMethod.call(obj, contextParent, modifiers).any { objf.accepts(it) }
                                        }
                                    }, retr)
                                } else {
                                    CombinedObjFilter(null, retr)
                                }

                                PathExecutionResult(retr, resObjs, combinedFilter)
                            } else {
                                PathExecutionResult(null, resObjs, CombinedObjFilter(null, null))
                            }
                        }
                        ExecType.SourceObjCalc -> {
                            val objs = genObjects.getAllObjectsOfType(obj.name)

                            val filteredObjects = res.objFilter?.let { filter ->
                                objs.filter { filter.accepts(it) }
                            } ?: objs

                            val finalRes = filteredObjects.toMutableSet()
                            res.objs?.let {finalRes.addAll(it)}

                            val filteredFinalRes = filterObjectsBottomUp(finalRes, rootPath!!)
                            PathExecutionResult(null, filteredFinalRes, CombinedObjFilter(null, null))
                        }
                        ExecType.SourcePropertyCalc -> {
                            throw IllegalStateException("Buiding path from property not implemented")
                        }
                    }
                }
                cond == null && subPath != null -> {
                    val res = getChildResult(0) as PathExecutionResult
                    return when (t) {
                        ExecType.FilterCalc -> {
                            val subGet = genObjects.getDefMethod(contextParent.parent!!.name, contextParent.memName)
                            val newRet = res.objRetriever?.let { ret ->
                                ObjRetriever {obj, intersectWith ->
                                    val objs = if (contextParent.isMany) {
                                        subGet.call(obj, contextParent, modifiers) as List<GeneratedObject>
                                    } else {
                                        listOf(subGet.call(obj, contextParent, modifiers) as GeneratedObject)
                                    }
                                    val res = mutableSetOf<GeneratedObject>()
                                    objs.forEach {obj -> res.addAll(ret.retrieve(obj, intersectWith))}
                                    res
                                }
                            }
                            val combinedFilter = if (contextParent.isRev) {
                                val revMethod = genObjects.getRevDefMethod(obj.name, contextParent.parent.name)

                                val objFilterBuilder = res.combinedObjFilter.objFilter?.let { fb ->
                                    ObjRevFilterBuilder { objf ->
                                        fb.build { obj ->
                                            revMethod.call(obj, contextParent, modifiers).any { objf.accepts(it) }
                                        }
                                    }
                                }
                                CombinedObjFilter(objFilterBuilder, res.combinedObjFilter.objRetriever?.let { ret -> ObjRetriever {obj, intersectWith ->
                                    subGet.callToList(obj, contextParent, modifiers).flatMap { ret.retrieve(it, intersectWith) }.toSet()
                                } })
                            } else {
                                CombinedObjFilter(null, newRet)
                            }
                            PathExecutionResult(newRet, res.bottomLevelObjects, combinedFilter)
                        }
                        ExecType.SourceObjCalc -> {
                            if (res.objRetriever == null) {
                                PathExecutionResult(null, res.bottomLevelObjects, CombinedObjFilter(null, null))
                            } else {
                                val objs = filterObjectsBottomUp(genObjects.getAllObjectsOfType(obj.name), rootPath!!)

                                val resObjs = objs.flatMap { res.objRetriever.retrieve(it, null) }.toMutableSet()
                                res.bottomLevelObjects?.let { resObjs.addAll(it) }

                                PathExecutionResult(null, resObjs, CombinedObjFilter(null, null))
                            }
                        }
                        ExecType.SourcePropertyCalc -> {
                            throw IllegalStateException("no condition for source property")
                        }
                    }
                }
                else -> { // cond != null && subPath != null
                    val resCond = getChildResult(0) as ObjExecutionResult
                    val resPath = getChildResult(1) as PathExecutionResult
                    when (t) {
                        ExecType.FilterCalc -> {
                            val finalObjects = resPath.bottomLevelObjects?.toMutableSet() ?: mutableSetOf()

                            if (resPath.objRetriever != null && resCond.objs != null) {
                                val objs = filterObjectsBottomUp(resCond.objs, rootPath!!)
                                finalObjects.addAll(objs.flatMap { resPath.objRetriever.retrieve(it, null) })
                            }

                            val getSub = genObjects.getDefMethod(contextParent.parent!!.name, contextParent.memName)
                            val finalRet = resCond.objFilter?.let { filter -> resPath.objRetriever?.let { ret ->
                                ObjRetriever { obj, intersectWith ->
                                    val objs = if (contextParent.isMany) {
                                        getSub.call(obj, contextParent, modifiers) as List<GeneratedObject>
                                    } else {
                                        listOf(getSub.call(obj, contextParent, modifiers) as GeneratedObject)
                                    }

                                    objs.filter { filter.accepts(it) }.flatMap { ret.retrieve(it, intersectWith) }.toSet()
                                }
                            } }

                            val combinedFilter = if (resCond.objFilter != null) {
                                if (contextParent.isRev) {
                                    val revMethod = genObjects.getRevDefMethod(obj.name, contextParent.parent.name)

                                    val objFilterBuilder = resPath.combinedObjFilter.objFilter?.let { fb ->
                                        ObjRevFilterBuilder { objf ->
                                            fb.build { obj ->
                                                (resCond.objFilter.accepts(obj))
                                                        &&
                                                        revMethod.call(obj, contextParent, modifiers)
                                                            .any { objf.accepts(it) }
                                            }
                                        }
                                    }
                                    CombinedObjFilter(
                                        objFilterBuilder,
                                        resPath.combinedObjFilter.objRetriever?.let { ret ->
                                            ObjRetriever { obj, intersectWith ->
                                                getSub.callToList(obj, contextParent, modifiers)
                                                    .filter{resCond.objFilter.accepts(it)}
                                                    .flatMap { ret.retrieve(it, intersectWith) }.toSet()
                                            }
                                        })
                                } else {
                                    CombinedObjFilter(null, finalRet)
                                }
                            } else {
                                CombinedObjFilter(null, null)
                            }

                            PathExecutionResult(finalRet, finalObjects, combinedFilter)
                        }
                        ExecType.SourceObjCalc -> {
                            val finalObjs = if (resCond.objFilter == null) {
                                resCond.objs!!
                            } else {
                                val objs = genObjects.getAllObjectsOfType(obj.name)

                                objs.filter { resCond.objFilter.accepts(it) || resCond.objs?.contains(it) ?: false }
                            }
                            val filteredFinalObjects = filterObjectsBottomUp(finalObjs, rootPath!!)

                            val finalSobjs = mutableSetOf<GeneratedObject>()
                            resPath.bottomLevelObjects?.let { finalSobjs.addAll(it) }
                            resPath.objRetriever?.let { ret -> finalSobjs.addAll(filteredFinalObjects.flatMap { ret.retrieve(it, null) }) }
                            PathExecutionResult(null, finalSobjs, CombinedObjFilter(null, null))
                        }
                        ExecType.SourcePropertyCalc -> {
                            throw IllegalStateException("incorrect calc type")
                        }
                    }
                }
            }
        }
    }

    inner class ObjAndExecNode(l: ObjCondExecutionNode, r: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val left = getChildResult(0) as ObjExecutionResult
            val right = getChildResult(0) as ObjExecutionResult

            val leftObjs = right.objFilter?.let { filter ->
                left.objs?.filter { filter.accepts(it) }?.toSet()
            } ?: left.objs

            val rightObjs = left.objFilter?.let { filter ->
                right.objs?.filter { filter.accepts(it) }?.toSet()
            } ?: right.objs

            val resObjs = if (leftObjs == null && rightObjs == null) {
                null
            } else {
                leftObjs?.union(rightObjs ?: emptySet()) ?: rightObjs
            }

            val objFilter = if (left.objFilter != null && right.objFilter != null) {
                ObjFilter { obj -> left.objFilter.accepts(obj) && right.objFilter.accepts(obj)}
            } else {
                null
            }

            return ObjExecutionResult(objFilter, resObjs)
        }
    }

    inner class ObjOrExecNode(l: ObjCondExecutionNode, r: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val left = getChildResult(0) as ObjExecutionResult
            val right = getChildResult(0) as ObjExecutionResult

            val objFilter = if (left.objFilter != null || right.objFilter != null) {
                ObjFilter { obj ->
                    left.objFilter?.accepts(obj) ?: false || right.objFilter?.accepts(obj) ?: false
                }
            } else {
                null
            }

            val resObjs = if (left.objs == null || right.objs == null) {
                left.objs ?: right.objs
            } else {
                left.objs.union(right.objs)
            }

            return ObjExecutionResult(objFilter, resObjs)
        }
    }

    inner class ObjNotExecNode(o: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(o)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val res = getChildResult(0) as ObjExecutionResult
            if (res.objs != null || res.objFilter == null) {
                throw IllegalStateException("objs should be null")
            }

            val objFilter = ObjFilter { obj -> ! res.objFilter.accepts(obj) }

            return ObjExecutionResult(objFilter, null)
        }
    }

    inner class ObjSubObjExecNode(
        val obj: Definition,
        val extendedDefField: ExtendedDefField,
        val modifiers: Map<String, ModValueType>,
        cond: ObjCondExecutionNode
    ) : ObjCondExecutionNode() {
        init {
            addChild(cond)
        }

        override fun executeInternal(t: ExecType): ExecutionResult {
            val res = getChildResult(0) as ObjExecutionResult
            return when (t) {
                ExecType.FilterCalc -> {
                    val getMethod = genObjects.getDefMethod(extendedDefField.parent!!.name, extendedDefField.memName)
                    val objs = res.objs?.let { objs ->
                        val revMethod = genObjects.getRevDefMethod(obj.name, extendedDefField.parent.name)
                        objs.flatMap { revMethod.call(it, extendedDefField, modifiers) as List<GeneratedObject> }
                    }

                    val objFilter = res.objFilter?.let { filter ->
                        ObjFilter { obj ->
                            val objs = if (extendedDefField.isMany) {
                                getMethod.call(obj, extendedDefField, modifiers) as List<GeneratedObject>
                            } else {
                                listOf(getMethod.call(obj, extendedDefField, modifiers) as GeneratedObject)
                            }

                            objs.any { filter.accepts(it) }
                        }
                    }

                    ObjExecutionResult(objFilter, objs?.toSet())
                }
                ExecType.SourceObjCalc -> {
                    val resObjs = res.objs?.toMutableSet() ?: mutableSetOf()
                    if (res.objFilter != null) {
                        val revMethod = genObjects.getRevDefMethod(obj.name, extendedDefField.parent!!.name)
                        val objs = genObjects.getAllObjectsOfType(obj.name)
                            .filter { res.objFilter.accepts(it) }
                            .flatMap { revMethod.call(it, extendedDefField, modifiers) as List<GeneratedObject> }
                            .toSet()

                        resObjs.addAll(objs)
                    }
                    ObjExecutionResult(null, resObjs)
                }
                ExecType.SourcePropertyCalc -> {
                    val objects = genObjects.getAllObjectsByProperty(
                        extendedDefField.parent!!.name,
                        extendedDefField.memName,
                        (getChildren()[0] as FinalValueNode).getValue()
                    )

                    ObjExecutionResult(null, objects.toSet())
                }
            }
        }
    }

    interface FinalValueNode {
        fun getValue(): ValueObject
    }

    inner class ObjStringExecNode(val s: String) : ObjCondExecutionNode(), FinalValueNode {
        override fun executeInternal(t: ExecType): ExecutionResult {
            return ObjExecutionResult(ObjFilter { obj ->  (obj as StringBuiltIn).getString() == s}, null)
        }

        override fun getValue(): ValueObject {
            return ValueObject.String(s)
        }
    }

    inner class ObjIntExecNode(val n: Int) : ObjCondExecutionNode(), FinalValueNode {
        override fun executeInternal(t: ExecType): ExecutionResult {
            return ObjExecutionResult(ObjFilter { obj ->  (obj as IntBuiltIn).getInt() == n}, null)
        }

        override fun getValue(): ValueObject {
            return ValueObject.Int(n)
        }
    }

    inner class ObjBoolExecNode(val b: Boolean) : ObjCondExecutionNode(), FinalValueNode {
        override fun executeInternal(t: ExecType): ExecutionResult {
            return ObjExecutionResult(ObjFilter { obj ->  (obj as BoolBuiltIn).getBool() == b}, null)
        }

        override fun getValue(): ValueObject {
            return ValueObject.Bool(b)
        }
    }

    fun buildPathGraph(
        cond: PathCondition,
        currObj: Definition?,
        sobjDef: Definition,
        addSobjCond: ObjCondExecutionNode?,
        currPathToRoot: List<RootPathElem>?,
    ): PathExecutionNode {
        when (cond) {
            is AndObjPath -> {
                return PathAndExecutionNode(
                    buildPathGraph(cond.l, currObj, sobjDef, addSobjCond, currPathToRoot),
                    buildPathGraph(cond.r, currObj, sobjDef, addSobjCond, currPathToRoot)
                )
            }
            is OrObjPath -> {
                return PathOrExecutionNode(
                    buildPathGraph(cond.l, currObj, sobjDef, addSobjCond, currPathToRoot),
                    buildPathGraph(cond.r, currObj, sobjDef, addSobjCond, currPathToRoot)
                )
            }
            is SubObjPath -> {
                val newAddObjCond = if (cond.addSearchObjCond != null) {
                    val newCond = buildCondGraph(sobjDef, cond.addSearchObjCond)
                    if (addSobjCond == null) {
                        newCond
                    } else {
                        ObjAndExecNode(
                            newCond,
                            addSobjCond
                        )
                    }
                } else {
                    addSobjCond
                }

                val (newObj, defField) = if (currObj != null) {
                    scheme.getSubObj(currObj, cond.objType)!!
                } else {
                    val def = scheme.getDefinition(cond.objType.capitalize())!!
                    val defField = DefField(false, "", "", emptyList(), false, false, false)
                    Pair(def, ExtendedDefField(null, defField))
                }

                val currObjCond = cond.objCond?.let { buildCondGraph(newObj, it) }

                val newRootPath = if (defField.isRev || defField.parent == null) {
                    currPathToRoot?.toMutableList()
                } else {
                    null
                }
                newRootPath?.add(
                    RootPathElem(
                        currObjCond?.id ?: -1,
                        defField.parent?.let { genObjects.getRevDefMethod(defField.memType, it.name) },
                        defField,
                        cond.modifiers
                    )
                )

                var currPathCond = cond.subObjPath?.let { buildPathGraph(it, newObj, sobjDef, newAddObjCond, newRootPath) }
                currPathCond = currPathCond?.let { PathSubExecNode(newObj, defField, cond.modifiers, currObjCond, it, newRootPath)}

                if (currPathCond == null) {
                    val used = mutableSetOf<String>()
                    used.add(newObj.name)
                    currPathCond = buildPathToSobj(sobjDef, newObj, defField, newAddObjCond, currObjCond, used, newRootPath)
                    if (currPathCond == null) {
                        throw RuntimeException("did not find path to sobj")
                    }
                }

                return currPathCond
            }
        }
    }

    fun buildCondGraph(currObj: Definition, objCond: ObjCondition): ObjCondExecutionNode {
        when (objCond) {
            is AndObjCond -> {
                return ObjAndExecNode(
                    buildCondGraph(currObj, objCond.l),
                    buildCondGraph(currObj, objCond.r)
                )
            }
            is EmptyObjCond -> {
                return ObjBoolExecNode(true)
            }
            is IntObjectCond -> {
                return ObjIntExecNode(objCond.i)
            }
            is NotObjCond -> {
                return ObjNotExecNode(buildCondGraph(currObj, objCond.o))
            }
            is OrObjCond -> {
                return ObjOrExecNode(
                    buildCondGraph(currObj, objCond.l),
                    buildCondGraph(currObj, objCond.r)
                )
            }
            is StringObjCond -> {
                return ObjStringExecNode(
                    objCond.s
                )
            }
            is SubObjSearch -> {
                val (newObj, defField) = scheme.getSubObj(currObj, objCond.objType)!!
                return ObjSubObjExecNode(newObj, defField, objCond.modifiers, buildCondGraph(newObj, objCond.objCond))
            }
        }
    }

    fun buildPathToSobj(
        sobj: Definition,
        currObj: Definition,
        parentObjField: ExtendedDefField,
        newAddObjCond: ObjCondExecutionNode?,
        currObjCond: ObjCondExecutionNode?,
        used: MutableSet<String>,
        currPathToRoot: List<RootPathElem>?
    ): PathExecutionNode? {
        if (currObj.name == sobj.name) {
            return PathSubExecNode(sobj, parentObjField, mapOf(), newAddObjCond, null, currPathToRoot)
        }

        val paths = mutableListOf<PathExecutionNode>()
        for ((i, subObj) in currObj.members.withIndex()) {
            if (used.contains(subObj.memType) || subObj.reference) {
                continue
            }
            val newObj = scheme.getDefinition(subObj.memType)!!
            if (newObj is Filter) {
                continue
            }
            used.add(subObj.memType)

            val newPathToRoot = if (subObj.isRev) {
                currPathToRoot?.toMutableList()
            } else {
                null
            }
            newPathToRoot?.add(
                RootPathElem(
                    -1,
                    parentObjField.parent?.let {(genObjects.getRevDefMethod(subObj.memType, currObj.name))},
                    parentObjField,
                    mapOf()
                )
            )
            val pathCond = buildPathToSobj(sobj, newObj, ExtendedDefField(currObj, subObj), newAddObjCond, null, used, newPathToRoot)
            used.remove(subObj.memType)
            if (pathCond != null) {
                paths.add(pathCond)
            }
        }

        if (paths.isEmpty()) {
            return null
        }

        return PathSubExecNode(currObj, parentObjField, mapOf(), currObjCond, paths.reduce {acc, a -> PathOrExecutionNode(acc, a)}, currPathToRoot)
    }
}