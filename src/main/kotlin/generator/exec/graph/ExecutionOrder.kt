package generator.exec.graph

import generator.scheme.ast.Definition

interface ExecutionOrder {
    fun genExecOrder(root: ExecutionGraph.PathExecutionNode, sobj: Definition): List<ExecOrderNode>
}