package generator.parser

import generator.ast.DefField
import generator.ast.Filter
import generator.ast.Object
import generator.ast.ShortCut
import parser.inp
import utils.getResourceAsText
import kotlin.test.Test
import kotlin.test.assertEquals

class GenParserTests {

    @Test
    fun testObject() {
        val input = """
            source object Project {
            	id: Id [source]
            	name: Name [source]
            	feature: Feature [many, rev] (withInherited)
            	vcs_root: VcsRoot [many, rev]
            	archived: Archived
                project: Project [many, rev]
                build_conf: BuildConf [many, rev]
                template: Template [many, rev]
            } `abcd`
        """.trimIndent().inp()

        val res = astParser.parse(input)

        assertEquals(listOf(
            Object("Project", null, listOf(
                DefField("id", "Id", listOf(), false, true, false),
                DefField("name", "Name", listOf(), false, true, false),
                DefField("feature", "Feature", listOf("withInherited"), true, false, true),
                DefField("vcs_root", "VcsRoot", listOf(), true, false, true),
                DefField("archived", "Archived", listOf(), false, false, false),
                DefField("project", "Project", listOf(), true, false, true),
                DefField("build_conf", "BuildConf", listOf(), true, false, true),
                DefField("template", "Template", listOf(), true, false, true),
            ), ShortCut("abcd"), true)
        ), res.unwrap())
    }

    @Test
    fun testTeamcityGs() {
        val input = getResourceAsText("teamcity.gs")!!
        val res = astParser.parse(input.inp())
        assert(res.isOk())
    }
}