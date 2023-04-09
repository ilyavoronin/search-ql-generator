package generator.parser

import generator.lang.ast.*
import generator.lang.parser.getLangParser
import generator.scheme.GeneratorScheme
import generator.scheme.parser.astParser
import parser.inp
import utils.getResourceAsText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LangParserTests {

    val scheme: GeneratorScheme
    init {
        val schemeStr = getResourceAsText("teamcity.gs")!!
        scheme = GeneratorScheme(astParser.parse(schemeStr.inp()).unwrap())
    }
    @Test
    fun testSimple() {
        val input = """
            find trigger in buildConf with type ("scheduled")
        """.trimIndent()

        val p = getLangParser(scheme)
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
            name ("abacaba")
            and
            (build_conf (name ("T1")) or not build_conf (name ("T2")) )
        """.trimIndent()

        val p = getLangParser(scheme)
        val res = p.parse(input.inp())
        assertEquals(FindQuery(
            "project",
            null,
            AndObjCond(
                SubObjSearch("name", StringObjCond("abacaba")),
                OrObjCond(
                    SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T1"))),
                    NotObjCond(SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T2"))))
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
        val res = p.parse(input.inp())

        assertEquals(FindQuery("trigger",
            OrObjPath(
                SubObjPath("project",
                    AndObjCond(
                        SubObjSearch("name", StringObjCond("abacaba")),
                        OrObjCond(
                            SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T1"))),
                            NotObjCond(SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T2"))))
                        )
                    ),
                    AndObjPath(
                        SubObjPath("build_conf", SubObjSearch("name", StringObjCond("abacaba")), null, null),
                        SubObjPath("template", SubObjSearch("id", StringObjCond("qwerty")), null, null)
                    ),
                    SubObjSearch("type", StringObjCond("1"))
                ),
                SubObjPath("project",
                    SubObjSearch("name", StringObjCond("aba2")),
                    null,
                    SubObjSearch("type", StringObjCond("2"))
                )
            ),
            SubObjSearch("type", StringObjCond("scheduled"))
        ), res.unwrap())
    }
}