import generator.GeneratorScheme
import generator.codegen.ObjectsGenerator
import generator.parser.astParser
import parser.*
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths

fun main() {
    val input = File("src/test/resources/teamcity.gs").readText()
    val res = astParser.parse(input.inp()).unwrap()
    val scheme = GeneratorScheme(res)

    val objGen = ObjectsGenerator()
    objGen.genCode("genSearchQL", "net.searchQL", scheme);
}