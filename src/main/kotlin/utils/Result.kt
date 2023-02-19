package utils

sealed class Result<T, E> {
    protected abstract fun value(): T?

    protected abstract fun error(): E?

    fun unwrap(): T {
        if (this.isOk()) {
            return this.value()!!
        } else {
            val err = this.err()!!
            if (err is Throwable) {
                throw err
            } else {
                throw UnwrapError(err)
            }
        }
    }

    fun err(): E {
        return this.error()!!
    }

    fun isOk(): Boolean {
        return this.value() != null
    }

    fun isErr(): Boolean {
        return this.error() != null
    }

    fun map(f: (T) -> T): Result<T, E> {
        return when(this) {
            is Ok -> {
                Ok(f(this.v!!))
            }
            is Err -> {
                this
            }
        }
    }

    fun mapErr(f: (E) -> E): Result<T, E> {
        return when(this) {
            is Ok -> {
                this
            }
            is Err -> {
                Err(f(this.err()!!))
            }
        }
    }
}

class Ok<T, E>(val v: T): Result<T, E>() {
    override fun value(): T? {
        return v
    }

    override fun error(): E? {
        return null
    }
}

class Err<T, E>(val e: E): Result<T, E>() {
    override fun value(): T? {
        return null
    }

    override fun error(): E? {
        return e
    }
}

class UnwrapError(err: Any): Throwable()


