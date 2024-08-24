sealed interface JustOption<out T> {
    data class Some<out T>(val value: T) : JustOption<T>

    data object None : JustOption<Nothing>
    companion object {
        fun <T> empty(): JustOption<T> = None
    }
}

private fun <A, B> JustOption<A>.map(f: (A) -> B): JustOption<B> = when (this) {
    JustOption.None -> this as JustOption.None
    is JustOption.Some -> JustOption.Some(f(value))
}

private fun <A> JustOption<A>.getOrElse(default: () -> A): A = when (this) {
    JustOption.None -> default()
    is JustOption.Some -> value
}

private fun <A, B> JustOption<A>.flatMap(f: (A) -> JustOption<B>): JustOption<B> = map(f).getOrElse { JustOption.None }

private fun <A> JustOption<A>.orElse(ob: () -> JustOption<A>): JustOption<A> = map { JustOption.Some(it) }.getOrElse { ob() }

private fun <A> JustOption<A>.filter(f: (A) -> Boolean): JustOption<A> = flatMap { if (f(it)) this else JustOption.None }

private fun <A, B, C> JustOption<A>.map2(other: JustOption<B>, f: (A, B) -> C): JustOption<C> =
    flatMap { a ->
        other.map { b -> f(a, b) }
    }

private fun <A> sequence(xs: List<JustOption<A>>): JustOption<List<A>> =
    xs.fold(JustOption.Some(listOf())) { acc, option -> acc.map2(option) { a, b -> a + b } }

private fun <A> sequence2(xs: List<JustOption<A>>): JustOption<List<A>> = traverse(xs) { it }

private fun <A> sequenceJustList(xs: JustList<JustOption<A>>): JustOption<JustList<A>> = traverseJustList(xs) { it }

private fun <T : Any?> T.asJustOption(): JustOption<T> = when {
    this is JustOption.None -> this
    this == null -> JustOption.None
    else -> JustOption.Some(value = this)
}

private fun <A, B> traverse(xs: List<A>, f: (A) -> JustOption<B>): JustOption<List<B>> = xs.map {
    when (val option = f(it)) {
        JustOption.None -> return JustOption.None
        is JustOption.Some -> option.value
    }
}.asJustOption()

private fun <A, B> traverseJustList(xs: JustList<A>, f: (A) -> JustOption<B>): JustOption<JustList<B>> = when (xs) {
    JustList.Nil -> JustOption.Some(JustList.Nil)
    is JustList.Construct -> f(xs.head).map2(traverseJustList(xs.tail, f)) { a, b -> JustList.Construct(a, b) }
}

private fun <A> catchesOption(f: () -> A): JustOption<A> =
    try {
        JustOption.Some(f())
    } catch (e: Throwable) {
        JustOption.None
    }

fun main() {
    val seq = listOf(JustOption.Some(10), JustOption.Some(4), JustOption.Some(3), JustOption.Some(2))
    println(sequence(seq))
    println(sequence2(seq))
}