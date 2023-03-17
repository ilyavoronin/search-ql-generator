import generator.parser.astParser
import parser.*
import java.io.BufferedReader
import java.io.File

fun main() {
    val p1 = TokenParser("Hello", "")
    val p2 = TokenParser("World", "")
    val p3 = p1.or(p2).map { "Parsed successfully" }

    println(p3.parse("Hello".inp()).unwrap())
    println(p3.parse("World".inp()).unwrap())
    println(p3.parse("Dog".inp()).err())

    val p = combine {
        p1[it]
        spaces[it]
        token(",")[it]
        spaces[it]
        p2[it]
        spaces[it]
        token("!")[it]
        "Parsed successfully"
    }

    println(p.parse("Hello,World!".inp()).unwrap())
    println(p.parse("Hello   , World  !".inp()).unwrap())
    println(p.parse("Hello , world!".inp()).err())

    val bufferedReader: BufferedReader = File("teamcity.gs").bufferedReader()
    val inputString = bufferedReader.use { it.readText() }
    val res = astParser.parse(inputString.inp())
    println(res)
}