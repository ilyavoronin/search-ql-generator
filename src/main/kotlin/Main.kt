import generator.codegen.CodeGen
import generator.scheme.GeneratorScheme
import generator.codegen.ObjectsGenerator
import generator.scheme.parser.astParser
import parser.*
import java.io.File

fun generateCode(scheme: String, rootPath: String, pack: String) {
    val res = astParser.parse(scheme.inp()).unwrap()
    val scheme = GeneratorScheme(res)

    val absPath = CodeGen.genCode(rootPath, pack, scheme)
    println("Generated into $absPath")
}

fun main() {
    val input = File("../genTCSearchQL/teamcity.gs").readText()
    generateCode(input, "../genTCSearchQL/src/main/kotlin", "gen.searchQL")
}