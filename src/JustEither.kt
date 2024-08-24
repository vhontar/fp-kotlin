sealed interface JustEither<out E, out T> {
    data class Left<out E>(val value: E) : JustEither<E, Nothing>
    data class Right<out T>(val value: T) : JustEither<Nothing, T>
}

private fun <T> catchesEither(f: () -> T): JustEither<Exception, T> =
    try {
        JustEither.Right(f())
    } catch (e: Exception) {
        JustEither.Left(e)
    }

private fun <E, A, B> JustEither<E, A>.map(f: (A) -> B): JustEither<E, B> = when (this) {
    is JustEither.Left -> this
    is JustEither.Right -> JustEither.Right(f(value))
}

private fun <E, A> JustEither<E, A>.orElse(f: (E) -> JustEither<E, A>): JustEither<E, A> = when (this) {
    is JustEither.Left -> f(value)
    is JustEither.Right -> this
}


private fun <E, A, B> JustEither<E, A>.flatMap(f: (A) -> JustEither<E, B>): JustEither<E, B> = when (this) {
    is JustEither.Left -> this
    is JustEither.Right -> f(value)
}

private fun <E, A, B, C> JustEither<E, A>.map2(other: JustEither<E, B>, f: (A, B) -> C): JustEither<E, C> =
    flatMap { a ->
        other.map { b -> f(a, b) }
    }

private fun <E, T> T.toJustEither(): JustEither<E, T> = when (this) {
    is JustEither.Left<*> -> this as JustEither.Left<E> // awkward :thinking:
    else -> JustEither.Right(value = this)
}

private fun <E, A, B> traverse(xs: List<A>, f: (A) -> JustEither<E, B>): JustEither<E, List<B>> =
    xs.map {
        when (val result = f(it)) {
            is JustEither.Left -> return result
            is JustEither.Right -> result.value
        }
    }.toJustEither()

private fun <E, A, B> traverseJustList(xs: JustList<A>, f: (A) -> JustEither<E, B>): JustEither<E, JustList<B>> = when (xs) {
    JustList.Nil -> JustEither.Right(JustList.Nil)
    is JustList.Construct -> f(xs.head).map2(traverseJustList(xs.tail, f)) { a, b -> JustList.Construct(a, b) }
}
