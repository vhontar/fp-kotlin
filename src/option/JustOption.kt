package option

import list.JustList

sealed interface JustOption<out T> {
    data class Some<out T>(val value: T) : JustOption<T>

    data object None : JustOption<Nothing>
    companion object {
        fun <T> empty(): JustOption<T> = None

        fun <T> some(value: T): JustOption<T> = Some(value)
    }
}

fun <T> JustOption<T>.isEmpty(): Boolean = this is JustOption.None

fun <A, B> JustOption<A>.map(f: (A) -> B): JustOption<B> = when (this) {
    JustOption.None -> JustOption.empty()
    is JustOption.Some -> JustOption.Some(f(value))
}

fun <A> JustOption<A>.getOrElse(default: () -> A): A = when (this) {
    JustOption.None -> default()
    is JustOption.Some -> value
}

fun <A, B> JustOption<A>.flatMap(f: (A) -> JustOption<B>): JustOption<B> = map(f).getOrElse { JustOption.empty() }

fun <A> JustOption<A>.orElse(ob: () -> JustOption<A>): JustOption<A> = map { JustOption.Some(it) }.getOrElse { ob() }

fun <A> JustOption<A>.filter(f: (A) -> Boolean): JustOption<A> = flatMap { if (f(it)) this else JustOption.empty() }

fun <A, B, C> JustOption<A>.map2(other: JustOption<B>, f: (A, B) -> C): JustOption<C> =
    flatMap { a ->
        other.map { b -> f(a, b) }
    }

fun <A> sequence(xs: List<JustOption<A>>): JustOption<List<A>> =
    xs.fold(JustOption.Some(listOf())) { acc, option -> acc.map2(option) { a, b -> a + b } }

fun <A> sequence2(xs: List<JustOption<A>>): JustOption<List<A>> = traverse(xs) { it }

fun <A> sequenceJustList(xs: JustList<JustOption<A>>): JustOption<JustList<A>> = traverseJustList(xs) { it }

fun <T : Any?> T.asJustOption(): JustOption<T> = when {
    this is JustOption.None -> this
    this == null -> JustOption.empty()
    else -> JustOption.Some(value = this)
}

fun <A, B> traverse(xs: List<A>, f: (A) -> JustOption<B>): JustOption<List<B>> = xs.map {
    when (val option = f(it)) {
        JustOption.None -> return JustOption.empty()
        is JustOption.Some -> option.value
    }
}.asJustOption()

fun <A, B> traverseJustList(xs: JustList<A>, f: (A) -> JustOption<B>): JustOption<JustList<B>> = when (xs) {
    JustList.Nil -> JustOption.Some(JustList.empty())
    is JustList.Construct -> f(xs.head).map2(traverseJustList(xs.tail, f)) { a, b -> JustList.Construct(a, b) }
}

fun <A> catch(f: () -> A): JustOption<A> =
    try {
        JustOption.Some(f())
    } catch (e: Throwable) {
        JustOption.empty()
    }