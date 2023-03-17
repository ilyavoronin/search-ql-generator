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

fun parseTokenWhile(pred: (Char) -> Boolean): Parser<String> {
    return object : Parser<String> {
        override fun parse(input: ParserInput): Result<String, ParserError> {
            var res = ""
            var i = 0;
            while (input.pos + i < input.text.length && pred(input.text[input.pos + i])) {
                res += input.get(i)
                i++
            }
            input.forward(i);
            return Ok(res)
        }

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

class combine<T>(val f: combine<T>.(ParserInput) -> T): Parser<T> {
    operator fun <T> Parser<T>.get(input: ParserInput): T {
        return this.parse(input).panic()
    }
    operator fun <T> Parser<T>.invoke(input: ParserInput): Result<T, ParserError> {
        return this.parse(input)
    }

    fun <T> Parser<T>.b() = this.blank()

    fun <T> Parser<T>.br() = this.blankReq()

    fun <T> Parser<T>.nl() = this.newLine()

    fun <T> Parser<T>.s() = this.spaces()

    fun err(err: String, pos: Int) {
        throw CombineException(ParserError(err, pos))
    }

    override fun parse(input: ParserInput): Result<T, ParserError> {
        val initPos = input.pos
        return try {
            Ok(f(input))
        } catch (e: CombineException) {
            input.pos = initPos
            Err(e.err)
        }
    }
}

class CombineException(val err: ParserError): Throwable()

fun <T> Result<T, ParserError>.panic(): T {
    return when (this) {
        is Ok<T, *> -> this.v
        is Err<*, ParserError> -> throw CombineException(this.e)
    }
}