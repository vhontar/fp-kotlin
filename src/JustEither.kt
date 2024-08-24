sealed interface JustEither<out E, out T>

data class Left<out E>(val value: E) : JustEither<E, Nothing>

data class Right<out T>(val value: T) : JustEither<Nothing, T>

private fun <T> catchesEither(f: () -> T): JustEither<Exception, T> =
    try {
        Right(f())
    } catch (e: Exception) {
        Left(e)
    }

private fun <E, A, B> JustEither<E, A>.map(f: (A) -> B): JustEither<E, B> = when (this) {
    is Left -> this
    is Right -> Right(f(value))
}

private fun <E, A> JustEither<E, A>.orElse(f: (E) -> JustEither<E, A>): JustEither<E, A> = when (this) {
    is Left -> f(value)
    is Right -> this
}


private fun <E, A, B> JustEither<E, A>.flatMap(f: (A) -> JustEither<E, B>): JustEither<E, B> = when (this) {
    is Left -> this
    is Right -> f(value)
}

private fun <E, A, B, C> JustEither<E, A>.map2(other: JustEither<E, B>, f: (A, B) -> C): JustEither<E, C> =
    flatMap { a ->
        other.map { b -> f(a, b) }
    }

private fun <E, T> T.toJustEither(): JustEither<E, T> = when (this) {
    is Left<*> -> this as Left<E> // awkward :thinking:
    else -> Right(value = this)
}

private fun <E, A, B> traverse(xs: List<A>, f: (A) -> JustEither<E, B>): JustEither<E, List<B>> =
    xs.map {
        when (val result = f(it)) {
            is Left -> return result
            is Right -> result.value
        }
    }.toJustEither()

private fun <E, A, B> traverseJustList(xs: JustList<A>, f: (A) -> JustEither<E, B>): JustEither<E, JustList<B>> = when (xs) {
    Nil -> Right(Nil)
    is Construct -> f(xs.head).map2(traverseJustList(xs.tail, f)) { a, b -> Construct(a, b) }
}
