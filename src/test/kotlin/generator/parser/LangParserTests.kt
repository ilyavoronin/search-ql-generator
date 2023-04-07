package generator.parser

import generator.lang.ast.*
import generator.lang.parser.getLangParser
import generator.scheme.GeneratorScheme
import parser.inp
import kotlin.test.Test
import kotlin.test.assertEquals

class LangParserTests {
    @Test
    fun testSimple() {
        val input = """
            find trigger in buildConf with type ("scheduled")
        """.trimIndent()

        val p = getLangParser(GeneratorScheme(listOf()))
        val res = p.parse(input.inp())
        assertEquals(FindQuery(
            "trigger",
            SubObjPath("buildConf", null, null, null),
            SubObjSearch("type", StringObjCond("scheduled"))
        ), res.unwrap())
    }

    @Test
    fun testCondition() {
        val input = """
            find project with
            type ("abacaba")
            and
            (buildConf (type ("T1")) or not buildConf (type ("T2")) )
        """.trimIndent()

        val p = getLangParser(GeneratorScheme(listOf()))
        val res = p.parse(input.inp())
        assertEquals(FindQuery(
            "project",
            null,
            AndObjCond(
                SubObjSearch("type", StringObjCond("abacaba")),
                OrObjCond(
                    SubObjSearch("buildConf", SubObjSearch("type", StringObjCond("T1"))),
                    NotObjCond(SubObjSearch("buildConf", SubObjSearch("type", StringObjCond("T2"))))
                )
            )
        ), res.unwrap())
    }

    @Test
    fun testComplexQuery() {
        val input = """
            find trigger
            in
            	project
            	(
            		type ("abacaba")
            		and
            		(buildConf (type ("T1")) or not buildConf (type ("T2")) )
            	) -> {
            		buildConf
            		(
            		   type ("abacaba")
            		) and
            		template
            		(
            		   type ("qwerty")
            		)
            	}.{owner ("1")}

            	or 

            	project 
            	(
            	   type ("aba2")
            	).{owner ("2")}
            
            
            with type ("scheduled")
        """.trimIndent()

        val p = getLangParser(GeneratorScheme(listOf()))
        val res = p.parse(input.inp())

        assertEquals(FindQuery("trigger",
            OrObjPath(
                SubObjPath("project",
                    AndObjCond(
                        SubObjSearch("type", StringObjCond("abacaba")),
                        OrObjCond(
                            SubObjSearch("buildConf", SubObjSearch("type", StringObjCond("T1"))),
                            NotObjCond(SubObjSearch("buildConf", SubObjSearch("type", StringObjCond("T2"))))
                        )
                    ),
                    AndObjPath(
                        SubObjPath("buildConf", SubObjSearch("type", StringObjCond("abacaba")), null, null),
                        SubObjPath("template", SubObjSearch("type", StringObjCond("qwerty")), null, null)
                    ),
                    SubObjSearch("owner", StringObjCond("1"))
                ),
                SubObjPath("project",
                    SubObjSearch("type", StringObjCond("aba2")),
                    null,
                    SubObjSearch("owner", StringObjCond("2"))
                )
            ),
            SubObjSearch("type", StringObjCond("scheduled"))
        ), res.unwrap())
    }
}