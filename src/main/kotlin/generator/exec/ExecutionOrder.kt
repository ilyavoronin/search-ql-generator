package generator.exec

import generator.scheme.GeneratorScheme
import generator.scheme.ast.Definition

interface ExecutionOrder {
    fun genExecOrder(root: ExecutionGraph.PathExecutionNode, scheme: GeneratorScheme, sobj: Definition): List<ExecOrderNode>
}