sealed interface JustOption<out T> {
    companion object {
        fun <T> empty(): JustOption<T> = None
    }
}

data class Some<out T>(val value: T) : JustOption<T>

data object None : JustOption<Nothing>

private fun <A, B> JustOption<A>.map(f: (A) -> B): JustOption<B> = when (this) {
    None -> this as None
    is Some -> Some(f(value))
}

private fun <A> JustOption<A>.getOrElse(default: () -> A): A = when (this) {
    None -> default()
    is Some -> value
}

private fun <A, B> JustOption<A>.flatMap(f: (A) -> JustOption<B>): JustOption<B> = map(f).getOrElse { None }

private fun <A> JustOption<A>.orElse(ob: () -> JustOption<A>): JustOption<A> = map { Some(it) }.getOrElse { ob() }

private fun <A> JustOption<A>.filter(f: (A) -> Boolean): JustOption<A> = flatMap { if (f(it)) this else None }

private fun <A, B, C> JustOption<A>.map2(other: JustOption<B>, f: (A, B) -> C): JustOption<C> =
    flatMap { a ->
        other.map { b -> f(a, b) }
    }

private fun <A> sequence(xs: List<JustOption<A>>): JustOption<List<A>> =
    xs.fold(Some(listOf())) { acc, option -> acc.map2(option) { a, b -> a + b } }

private fun <A> sequence2(xs: List<JustOption<A>>): JustOption<List<A>> = traverse(xs) { it }

private fun <A> sequenceJustList(xs: JustList<JustOption<A>>): JustOption<JustList<A>> = traverseJustList(xs) { it }

private fun <T : Any?> T.asJustOption(): JustOption<T> = when {
    this is None -> this
    this == null -> None
    else -> Some(value = this)
}

private fun <A, B> traverse(xs: List<A>, f: (A) -> JustOption<B>): JustOption<List<B>> = xs.map {
    when (val option = f(it)) {
        None -> return None
        is Some -> option.value
    }
}.asJustOption()

private fun <A, B> traverseJustList(xs: JustList<A>, f: (A) -> JustOption<B>): JustOption<JustList<B>> = when (xs) {
    Nil -> Some(Nil)
    is Construct -> f(xs.head).map2(traverseJustList(xs.tail, f)) { a, b -> Construct(a, b) }
}

private fun <A> catchesOption(f: () -> A): JustOption<A> =
    try {
        Some(f())
    } catch (e: Throwable) {
        None
    }

fun main() {
    val seq = listOf(Some(10), Some(4), Some(3), Some(2))
    println(sequence(seq))
    println(sequence2(seq))
}