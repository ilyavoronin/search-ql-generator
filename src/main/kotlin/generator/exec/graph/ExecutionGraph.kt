package generator.exec.graph

import generator.lang.ast.*
import generator.scheme.GeneratorScheme
import generator.scheme.ast.Definition
import generator.scheme.ast.Object
import java.lang.IllegalStateException
import java.util.Objects

class ExecutionGraph(val scheme: GeneratorScheme, findQuery: FindQuery) {

    val root: PathExecutionNode
    val sobj: Object
    val idToNode: MutableMap<Int, ExecutionNode>
    init {
        idToNode = mutableMapOf()
        sobj = scheme.getDefinition(findQuery.sobject.capitalize())!! as Object
        root = if (findQuery.inCond != null) {
            buildPathGraph(findQuery.inCond, null, sobj, findQuery.withCond?.let {buildCondGraph(sobj, it) })
        } else {
            PathSubExecNode(sobj, buildCondGraph(sobj, findQuery.withCond!!), null)
        }
    }

    fun getNode(id: Int): ExecutionNode? {
        return idToNode[id]
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
        
        protected fun addChild(node: ExecutionNode) {
            children.add(node)
        }
    }

    abstract inner class PathExecutionNode: ExecutionNode()

    abstract inner class ObjCondExecutionNode: ExecutionNode()

    inner class PathAndExecutionNode(l: PathExecutionNode, r: PathExecutionNode) : PathExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }
    }
    inner class PathOrExecutionNode(l: PathExecutionNode, r: PathExecutionNode) : PathExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }
    }
    inner class PathSubExecNode(val obj: Definition, cond: ObjCondExecutionNode?, subPath: PathExecutionNode?) : PathExecutionNode() {
        init {
            cond?.let{ addChild(it)}
            cond?.let { addChild(it) }
        }
    }

    inner class ObjAndExecNode(l: ObjCondExecutionNode, r: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }
    }

    inner class ObjOrExecNode(l: ObjCondExecutionNode, r: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(l)
            addChild(r)
        }
    }

    inner class ObjNotExecNode(o: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(o)
        }
    }

    inner class ObjSubObjExecNode(val obj: Definition, cond: ObjCondExecutionNode) : ObjCondExecutionNode() {
        init {
            addChild(cond)
        }
    }

    inner class ObjStringExecNode(val s: String) : ObjCondExecutionNode()
    inner class ObjIntExecNode(val n: Int) : ObjCondExecutionNode()
    inner class ObjBoolExecNode(val b: Boolean) : ObjCondExecutionNode()

    fun buildPathGraph(
        cond: PathCondition,
        currObj: Definition?,
        sobjDef: Definition,
        addSobjCond: ObjCondExecutionNode?,
    ): PathExecutionNode {
        when (cond) {
            is AndObjPath -> {
                return PathAndExecutionNode(
                    buildPathGraph(cond.l, currObj, sobjDef, addSobjCond),
                    buildPathGraph(cond.r, currObj, sobjDef, addSobjCond)
                )
            }
            is OrObjPath -> {
                return PathOrExecutionNode(
                    buildPathGraph(cond.l, currObj, sobjDef, addSobjCond),
                    buildPathGraph(cond.r, currObj, sobjDef, addSobjCond)
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

                val newObj = if (currObj != null) {
                    scheme.getSubObj(currObj, cond.objType)!!
                } else {
                    scheme.getDefinition(cond.objType.capitalize())!!
                }

                val currObjCond = cond.objCond?.let { buildCondGraph(newObj, it) }

                var currPathCond = cond.subObjPath?.let { buildPathGraph(it, newObj, sobjDef,  newAddObjCond) }

                if (currPathCond == null) {
                    val used = mutableSetOf<String>()
                    currObj?.let {used.add(it.name) }
                    currPathCond = buildPathToSobj(sobjDef, newObj, PathSubExecNode(sobjDef, newAddObjCond, null), used)
                }

                return PathSubExecNode(newObj, currObjCond, currPathCond)
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
                val newObj = scheme.getSubObj(currObj, objCond.objType)!!
                return ObjSubObjExecNode(newObj, buildCondGraph(newObj, objCond.objCond))
            }
        }
    }

    fun buildPathToSobj(sobj: Definition, currObj: Definition, sobjPathNode: PathExecutionNode, used: MutableSet<String>): PathExecutionNode? {
        if (currObj.name == sobj.name) {
            return sobjPathNode
        }

        val paths = mutableListOf<PathExecutionNode>()
        for (subObj in currObj.members) {
            if (used.contains(subObj.memType)) {
                continue
            }
            used.add(subObj.memType)
            val pathCond = buildPathToSobj(sobj, scheme.getDefinition(subObj.memType)!!, sobjPathNode, used)
            used.remove(subObj.memType)
            if (pathCond != null) {
                paths.add(pathCond)
            }
        }

        if (paths.isEmpty()) {
            return null
        }

        return paths.reduce {acc, a -> PathOrExecutionNode(acc, a)}
    }
}