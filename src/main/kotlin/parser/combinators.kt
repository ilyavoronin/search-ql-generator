package parser

import utils.Err
import utils.Ok
import utils.Result

fun <T> Parser<T>.or(other: Parser<T>): Parser<T> {
    return object : Parser<T> {
        override fun parse(input: ParserInput): Result<T, ParserError> {
            return when (val res = this@or.parse(input)) {
                is Ok<*, *> -> res
                is Err<*, *> -> {
                    when (val res2 = other.parse(input)) {
                        is Ok<*, *> -> res2
                        is Err<*, *> -> createErr("${res.e} or ${res2.e}", input.pos)
                    }
                }
            }
        }
    }
}

fun <T, S> Parser<T>.map(f: (T) -> S): Parser<S> {
    return object : Parser<S> {
        override fun parse(input: ParserInput): Result<S, ParserError> {
            return when (val res = this@map.parse(input)) {
                is Ok<T, *> -> Ok(f(res.v))
                is Err<*, ParserError> -> Err(res.e)
            }
        }

    }
}

private fun <T> parseWhile(parser: Parser<T>, input: ParserInput): Pair<List<T>, ParserError> {
    val resList = mutableListOf<T>()
    while (true) {
        val res = parser.parse(input)
        if (res.isErr()) {
            return Pair(resList, res.err())
        }
        resList.add(res.unwrap())
    }
}

fun <T> Parser<T>.atLeast(cnt: Int): Parser<List<T>> {
    return object : Parser<List<T>> {
        override fun parse(input: ParserInput): Result<List<T>, ParserError> {
            val (resList, err) = parseWhile(this@atLeast, input);
            if (resList.size < cnt) {
                return Err(err)
            }
            return Ok(resList)
        }
    }
}

fun <T> Parser<T>.many(): Parser<List<T>> {
    return object : Parser<List<T>> {
        override fun parse(input: ParserInput): Result<List<T>, ParserError> {
            val (resList, _) = parseWhile(this@many, input);
            return Ok(resList)
        }
    }
}