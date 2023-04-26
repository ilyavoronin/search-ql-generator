package generator.parser

import generator.exec.graph.ExecType
import generator.exec.graph.ExecutionGraph
import generator.exec.graph.FixedBottomUpExecOrder
import generator.lang.parser.getLangParser
import generator.scheme.GeneratorScheme
import generator.scheme.parser.astParser
import org.junit.jupiter.api.Test
import parser.inp
import utils.getResourceAsText
import kotlin.test.assertEquals

class ExecOrderTest {

    val scheme: GeneratorScheme
    init {
        val schemeStr = getResourceAsText("teamcity.gs")!!
        scheme = GeneratorScheme(astParser.parse(schemeStr.inp()).unwrap())
    }
    @Test
    fun testSimpleBottomUpExecOrder() {
        val input = """
            find trigger
            in
            	project
            	(
            		name ("abacaba")
            		and
            		(build_conf (name ("T1")) or not build_conf (name ("T2")) )
            	) -> {
            		build_conf
            		(
            		   name ("abacaba")
            		) and
            		template
            		(
            		   id ("qwerty")
            		)
            	}.{type ("1")}

            	or 

            	project 
            	(
            	   name ("aba2")
            	).{type ("2")}
            
            with type ("scheduled")
        """.trimIndent()

        val p = getLangParser(scheme)
        val res = p.parse(input.inp()).unwrap()

        val graph = ExecutionGraph(scheme, res)

        val order = FixedBottomUpExecOrder().genExecOrder(graph.root, graph.sobj)

        val expectedOrder = listOf(5,6,7,8,9,10,11,12,13,14,15,16,17,2,3,0,1,4,18,19,20,21,22,23,24,25,29,30,26,27,28,31,32,33,34,35,36,37)
        assertEquals(expectedOrder, order.map {it.nodeId})

        val source_nodes = setOf(6, 8, 17, 21, 30)
        order.forEach { node ->
            if (node.nodeId in source_nodes) {
                assertEquals(ExecType.ObjCalc, node.type, "expected ExecOrderNode ${node.nodeId} to be of type ObjCalc")
            } else {
                assertEquals(ExecType.FilterCalc, node.type, "expected ExecOrderNode ${node.nodeId} to be of type FilterCalc")
            }
        }
    }
}