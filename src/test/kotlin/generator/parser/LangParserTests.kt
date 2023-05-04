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
            SubObjPath("buildConf", null, null, null, mapOf()),
            SubObjSearch("type", StringObjCond("scheduled"), mapOf())
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
                SubObjSearch("name", StringObjCond("abacaba"), mapOf()),
                OrObjCond(
                    SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T1"), mapOf()), mapOf()),
                    NotObjCond(SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T2"), mapOf()), mapOf()))
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
                        SubObjSearch("name", StringObjCond("abacaba"), mapOf()),
                        OrObjCond(
                            SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T1"), mapOf()), mapOf()),
                            NotObjCond(SubObjSearch("build_conf", SubObjSearch("name", StringObjCond("T2"), mapOf()), mapOf()))
                        )
                    ),
                    AndObjPath(
                        SubObjPath("build_conf", SubObjSearch("name", StringObjCond("abacaba"), mapOf()), null, null, mapOf()),
                        SubObjPath("template", SubObjSearch("id", StringObjCond("qwerty"), mapOf()), null, null, mapOf())
                    ),
                    SubObjSearch("type", StringObjCond("1"), mapOf()),
                    mapOf()
                ),
                SubObjPath("project",
                    SubObjSearch("name", StringObjCond("aba2"), mapOf()),
                    null,
                    SubObjSearch("type", StringObjCond("2"), mapOf()),
                    mapOf()
                )
            ),
            SubObjSearch("type", StringObjCond("scheduled"), mapOf())
        ), res.unwrap())
    }
}