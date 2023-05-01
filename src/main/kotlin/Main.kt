import generator.codegen.CodeGen
import generator.scheme.GeneratorScheme
import generator.codegen.ObjectsGenerator
import generator.scheme.parser.astParser
import parser.*
import java.io.File

fun main() {
    val input = File("../genTCSearchQL/teamcity.gs").readText()
    val res = astParser.parse(input.inp()).unwrap()
    val scheme = GeneratorScheme(res)

    val absPath = CodeGen.genCode("../genTCSearchQL/src/main/kotlin", "gen.searchQL", scheme)
    println("Generated to $absPath")
}