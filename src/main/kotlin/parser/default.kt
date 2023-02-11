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