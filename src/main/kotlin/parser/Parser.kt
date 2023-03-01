package parser

import utils.Err
import utils.Result

interface Parser<out T> {
    fun parse(input: ParserInput): Result<T, ParserError>
}

data class ParserInput(val text: String) {
    var pos = 0

    fun startsWith(other: String): Boolean {
        for (i in other.indices) {
            if (pos + i >= text.length || other[i] != text[pos + i]) {
                return false
            }
        }
        return true
    }

    fun forward(cnt: Int) {
        this.pos += cnt
    }

    fun get(i: Int): Char {
        return text[pos + i]
    }
}

fun String.inp(): ParserInput {
    return ParserInput(this)
}

data class ParserError(val err: String, val pos: Int): Throwable()

fun <T> createErr(err: String, pos: Int): Result<T, ParserError> {
    return Err(ParserError(err, pos))
}