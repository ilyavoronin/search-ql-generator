package generator.parser

import generator.scheme.ast.DefField
import generator.scheme.ast.Filter
import generator.scheme.ast.Object
import generator.scheme.ast.ShortCut
import generator.scheme.parser.astParser
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
                ref project: Project [many, rev]
                build_conf: BuildConf [many, rev]
                template: Template [many, rev]
            } `abcd`
        """.trimIndent().inp()

        val res = astParser.parse(input)

        assertEquals(listOf(
            Object("Project", null, listOf(
                DefField(false, "id", "Id", listOf(), false, true, false),
                DefField(false, "name", "Name", listOf(), false, true, false),
                DefField(false, "feature", "Feature", listOf("withInherited"), true, false, true),
                DefField(false, "vcs_root", "VcsRoot", listOf(), true, false, true),
                DefField(false, "archived", "Archived", listOf(), false, false, false),
                DefField(true, "project", "Project", listOf(), true, false, true),
                DefField(false, "build_conf", "BuildConf", listOf(), true, false, true),
                DefField(false, "template", "Template", listOf(), true, false, true),
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