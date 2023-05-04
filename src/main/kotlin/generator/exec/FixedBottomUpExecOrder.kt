package generator.exec

import generator.lang.ast.AndObjPath
import generator.scheme.GeneratorScheme
import generator.scheme.ast.Definition
import java.lang.RuntimeException

class FixedBottomUpExecOrder(val scheme: GeneratorScheme) : ExecutionOrder {
    override fun genExecOrder(root: ExecutionGraph.PathExecutionNode, sobj: Definition): List<ExecOrderNode> {
        val resOrder = mutableListOf<ExecOrderNode>()
        val used = mutableMapOf<Int, Boolean>()
        buildOrder(root, sobj, resOrder, true, used, false)
        return resOrder
    }

    private fun buildOrder(
        v: ExecutionGraph.ExecutionNode,
        sobj: Definition,
        res: MutableList<ExecOrderNode>,
        allowObjCalc: Boolean,
        used: MutableMap<Int, Boolean>,
        metSource: Boolean
    ) {
        fun buildForChildren(v: ExecutionGraph.ExecutionNode, sobj: Definition, res: MutableList<ExecOrderNode>, allowCalc: Boolean, metSource: Boolean) {
            v.getChildren().map { buildOrder(it, sobj, res, allowCalc, used, metSource) }
        }
        if (used.contains(v.id)) {
            return
        }
        val isTopDown = when (v) {
            is ExecutionGraph.PathAndExecutionNode -> {
                buildForChildren(v, sobj, res, allowObjCalc, metSource)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.PathOrExecutionNode -> {
                buildForChildren(v, sobj, res, allowObjCalc, metSource)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.PathSubExecNode -> {
                val allowObjCalc = allowObjCalc && (v.contextParent.isRev || v.contextParent.isSource || !metSource) && v.modifiers.all { scheme.getModifier(it.key)!!.revAllowed }
                buildForChildren(v, sobj, res, allowObjCalc, true)
                if (metSource) {
                    res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                } else {
                    res.add(ExecOrderNode(v.id, ExecType.SourceObjCalc))
                }
            }
            is ExecutionGraph.ObjAndExecNode -> {
                buildForChildren(v, sobj, res, allowObjCalc, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjOrExecNode -> {
                buildForChildren(v, sobj, res, allowObjCalc, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjNotExecNode -> {
                buildForChildren(v, sobj, res, false, true)
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjBoolExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjIntExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjStringExecNode -> {
                res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
            }
            is ExecutionGraph.ObjSubObjExecNode -> {
                val allowObjCalc = allowObjCalc && (v.extendedDefField.isRev || v.extendedDefField.isSource || !metSource) && v.modifiers.all { scheme.getModifier(it.key)!!.revAllowed }
                if (v.extendedDefField.isSource && allowObjCalc) {
                    buildForChildren(v, sobj, res, false, true)
                    res.add(ExecOrderNode(v.id, ExecType.SourcePropertyCalc))
                } else {
                    buildForChildren(v, sobj, res, allowObjCalc, true)
                    res.add(ExecOrderNode(v.id, ExecType.FilterCalc))
                }
            }
            else -> {
                throw RuntimeException("unexpected type")
            }
        }
        used[v.id] = isTopDown
    }
}