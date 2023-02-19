package parser

import utils.Ok
import utils.Result

class TokenParser<T>(private val token: String, private val result: T): Parser<T> {
    override fun parse(input: ParserInput): Result<T, ParserError> {
        return if (input.startsWith(token)) {
            input.forward(token.length)
            Ok(result)
        } else {
            createErr(token, input.pos)
        }
    }
}

fun token(token: String): Parser<String> {
    return TokenParser(token, "")
}

val spaces = TokenParser(" ", "").many()

fun oneOf(tokens: List<String>): Parser<String> {
    return tokens.map { token(it) }.reduce { acc, parser -> acc.or(parser) }
}

val blank = oneOf(listOf(" ", "\t", "\n", "\r\n"))