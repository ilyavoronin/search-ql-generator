import parser.TokenParser
import parser.inp
import parser.map
import parser.or

fun main() {
    val p1 = TokenParser("Hello", "")
    val p2 = TokenParser("World", "")
    val p3 = p1.or(p2).map { "Parsed successfully" }

    println(p3.parse("Hello".inp()).unwrap())
    println(p3.parse("World".inp()).unwrap())
    println(p3.parse("Dog".inp()))
}