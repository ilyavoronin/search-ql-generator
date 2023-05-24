import generator.codegen.CodeGen
import generator.scheme.GeneratorScheme
import generator.scheme.parser.astParser
import parser.*
import java.io.File

fun generateCode(scheme: String, rootPath: String, pack: String) {
    val res = astParser.parse(scheme.inp()).unwrap()
    val scheme = GeneratorScheme(res)

    val absPath = CodeGen.genCode(rootPath, pack, scheme)
    println("Generated into $absPath")
}

fun main(args: Array<String>) {
    val scheme = args[0]
    val rootPath = args[1]
    val pack = args[2]
    val input = File(scheme).readText()
    generateCode(input, rootPath, pack)
}