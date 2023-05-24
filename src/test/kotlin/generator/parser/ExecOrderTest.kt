package generator.parser

import generator.GeneratedObject
import generator.exec.*
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

        val graph = ExecutionGraph(scheme, GeneratedObjects(object : ObjectsSource{}, scheme, listOf(BuildConf::class, Template::class), listOf(), true), res)

        val order = FixedBottomUpExecOrder(scheme).genExecOrder(graph.root, graph.sobj)

        val expectedOrder = listOf(5,6,7,8,9,10,11,12,13,14,15,16,17,2,3,0,1,4,18,19,20,21,22,23,24,25,29,30,26,27,28,31,32,33,34,35,36,37)
        assertEquals(expectedOrder, order.map {it.nodeId})

        val source_property_nodes = setOf(6, 8, 17, 21, 30)
        val source_obj_nodes = setOf(25, 36)
        order.forEach { node ->
            if (node.nodeId in source_property_nodes) {
                assertEquals(ExecType.SourcePropertyCalc, node.type, "expected ExecOrderNode ${node.nodeId} to be of type SourcePropertyCalc")
            } else if (node.nodeId !in source_obj_nodes) {
                assertEquals(ExecType.FilterCalc, node.type, "expected ExecOrderNode ${node.nodeId} to be of type FilterCalc")
            } else {
                assertEquals(ExecType.SourceObjCalc, node.type, "expected ExecOrderNode ${node.nodeId} to be of type ObjCals")
            }
        }
    }

    class BuildConf: GeneratedObject {
        fun parentProject() {}
    }

    class Template: GeneratedObject {
        fun parentProject() {}
    }
}