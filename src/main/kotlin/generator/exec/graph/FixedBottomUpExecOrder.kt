package generator.exec.graph

import generator.scheme.ast.Definition
import java.lang.RuntimeException

class FixedBottomUpExecOrder : ExecutionOrder {
    override fun genExecOrder(root: ExecutionGraph.PathExecutionNode, sobj: Definition): List<ExecOrderNode> {
        val resOrder = mutableListOf<ExecOrderNode>()
        val used = mutableMapOf<Int, Boolean>()
        val res = buildOrder(root, sobj, resOrder, true, used, false)
        return resOrder
    }

    private fun buildOrder(
        v: ExecutionGraph.ExecutionNode,
        sobj: Definition,
        res: MutableList<ExecOrderNode>,
        allowObjCalc: Boolean,
        used: MutableMap<Int, Boolean>,
        metSource: Boolean
    ) : Boolean {
        fun buildForChildren(v: ExecutionGraph.ExecutionNode, sobj: Definition, res: MutableList<ExecOrderNode>, allowCalc: Boolean, metSource: Boolean): List<Boolean> {
            return v.getChildren().map { buildOrder(it, sobj, res, allowCalc, used, metSource) }
        }
        if (used.contains(v.id)) {
            return used[v.id]!!
        }
        val isTopDown = when (v) {
            is ExecutionGraph.PathAndExecutionNode -> {
                val cr = buildForChildren(v, sobj, res, allowObjCalc, metSource)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                cr[0] || cr[1]
            }
            is ExecutionGraph.PathOrExecutionNode -> {
                val cr = buildForChildren(v, sobj, res, allowObjCalc, metSource)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                cr[0] && cr[1]
            }
            is ExecutionGraph.PathSubExecNode -> {
                val allowObjCalc = allowObjCalc && (v.contextParent.isRev || v.contextParent.isSource || !metSource)
                if (v.contextParent.isSource && allowObjCalc) {
                    buildForChildren(v, sobj, res, false, true)
                    res.add(ExecOrderNode(v.id, ExecType.SourcePropertyCalc))
                    true
                } else {
                    val cr = buildForChildren(v, sobj, res, allowObjCalc, true)
                    if (metSource || (cr[0] || cr[1])) {
                        res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                    } else {
                        res.add(ExecOrderNode(v.id, ExecType.SourcePropertyCalc))
                    }
                    cr[0] || cr[1]
                }
            }
            is ExecutionGraph.ObjAndExecNode -> {
                val cr = buildForChildren(v, sobj, res, allowObjCalc, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                cr[0] || cr[1]
            }
            is ExecutionGraph.ObjOrExecNode -> {
                val cr = buildForChildren(v, sobj, res, allowObjCalc, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                cr[0] && cr[1]
            }
            is ExecutionGraph.ObjNotExecNode -> {
                buildForChildren(v, sobj, res, false, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                false
            }
            is ExecutionGraph.ObjBoolExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                false
            }
            is ExecutionGraph.ObjIntExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                false
            }
            is ExecutionGraph.ObjStringExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjSubObjExecNode -> {
                val allowObjCalc = allowObjCalc && (v.extendedDefField.isRev || v.extendedDefField.isSource || !metSource)
                if (v.extendedDefField.isSource && allowObjCalc) {
                    buildForChildren(v, sobj, res, false, true)
                    res.add(ExecOrderNode(v.id, ExecType.SourcePropertyCalc))
                    true
                } else {
                    val cr = buildForChildren(v, sobj, res, allowObjCalc, true)
                    res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                    cr[0]
                }
            }
            else -> {
                throw RuntimeException("unexpected type")
            }
        }
        used[v.id] = isTopDown
        return isTopDown
    }
}