import generator.scheme.GeneratorScheme
import generator.scheme.codegen.ObjectsGenerator
import generator.scheme.parser.astParser
import parser.*
import java.io.File

fun main() {
    val input = File("../genTCSearchQL/teamcity.gs").readText()
    val res = astParser.parse(input.inp()).unwrap()
    val scheme = GeneratorScheme(res)

    val objGen = ObjectsGenerator()
    objGen.genCode("../genTCSearchQL/src/main/kotlin", "gen.searchQL", scheme)
}