package parser

import kotlin.test.Test

internal class ParserTests {
    @Test
    fun simpleTest() {
        val p1 = TokenParser("Hello", "")
        val p2 = TokenParser("World", "")
        val p3 = p1.or(p2).map { "Parsed successfully" }

        assert(p3.parse("Hello".inp()).isOk())
        assert(p3.parse("World".inp()).isOk())
        assert(p3.parse("Dog".inp()).isErr())
    }

    @Test
    fun combineTest() {
        val p1 = TokenParser("Hello", "")
        val p2 = TokenParser("World", "")
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

        assert(p.parse("Hello,World!".inp()).isOk())
        assert(p.parse("Hello   , World  !".inp()).isOk())
        assert(p.parse("Hello , world!".inp()).isErr())
    }
}