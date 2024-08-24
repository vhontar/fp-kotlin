sealed class JustList<out T> {
    data object Nil : JustList<Nothing>()
    data class Construct<out T>(val head: T, val tail: JustList<T>) : JustList<T>()
    companion object {
        fun <T> of(vararg aa: T): JustList<T> {
            val tail = aa.sliceArray(1..< aa.size)
            return if (aa.isEmpty()) Nil else Construct(aa[0], of(*tail))
        }

        fun <T> empty(): JustList<T> = Nil
    }
}

private fun <T> JustList<T>.tail(): JustList<T> = when (this) {
    JustList.Nil -> throw IllegalArgumentException("JustList.Nil cannot have a tail")
    is JustList.Construct -> tail
}

private fun <T> JustList<T>.setHead(head: T): JustList<T> = when (this) {
    JustList.Nil -> throw IllegalArgumentException("Cannot replace head of the JustList.Nil")
    is JustList.Construct -> JustList.Construct(head, tail)
}

private fun <T> JustList<T>.setTail(tail: JustList<T>): JustList<T> = when (this) {
    JustList.Nil -> throw IllegalArgumentException("Cannot replace tail of the JustList.Nil")
    is JustList.Construct -> JustList.Construct(head, tail)
}

private tailrec fun <T> JustList<T>.drop(n: Int): JustList<T> {
    if (n == 0) return this

    return when (this) {
        JustList.Nil -> throw IllegalArgumentException("Cannot drop more elements than in this JustList")
        is JustList.Construct -> tail.drop(n - 1)
    }
}

private tailrec fun <T> JustList<T>.dropWhile(f: (T) -> Boolean): JustList<T> {
    return when (this) {
        JustList.Nil -> this
        is JustList.Construct -> if (f(head)) tail.dropWhile(f) else this
    }
}

private fun <T> JustList<T>.dropLast(): JustList<T> = when (this) {
    JustList.Nil -> throw IllegalArgumentException("Cannot initialize JustList.Nil")
    is JustList.Construct -> if (tail == JustList.Nil) JustList.Nil else JustList.Construct(head, tail.dropLast())
}

private fun <T> JustList<T>.append(other: JustList<T>): JustList<T> = when (this) {
    JustList.Nil -> other
    is JustList.Construct -> JustList.Construct(head, tail.append(other))
}

private fun <T> JustList<T>.append2(other: JustList<T>) = foldRight(other) { head, tail -> JustList.Construct(head, tail) }

private fun <T> JustList<T>.append3(other: JustList<T>) = reversed().foldLeft(other) { tail, head -> JustList.Construct(head, tail) }


private fun <A, B> JustList<A>.foldRight(z: B, f: (A, B) -> B): B = when (this) {
    is JustList.Construct -> f(head, tail.foldRight(z, f))
    JustList.Nil -> z
}

private tailrec fun <A, B> JustList<A>.foldLeft(z: B, f: (B, A) -> B): B = when (this) {
    is JustList.Construct -> tail.foldLeft(f(z, head), f)
    JustList.Nil -> z
}

// TODO Try to investigate more here!!
private fun <A, B> JustList<A>.foldRightL(z: B, f: (A, B) -> B): B =
    foldLeft(
        z = { tail: B -> tail },
        f = { lambda, head ->
            { tail: B -> lambda(f(head, tail)) }
        }
    )(z)

private fun <T> JustList<T>.length2(): Int = foldRight(0) { _, acc -> acc + 1 }

private fun <T> JustList<T>.length(): Int = foldLeft(0) { acc, _ -> acc + 1 }

private fun <T> JustList<T>.reversed(): JustList<T> = when (this) {
    is JustList.Construct -> foldLeft(JustList.empty()) { tail, head -> JustList.Construct(head, tail) }
    JustList.Nil -> this
}

private fun <T> JustList<JustList<T>>.concat(): JustList<T> =
    foldRightL(JustList.empty()) { list1, list2 -> list1.append(list2) }

private fun <A, B> JustList<A>.map(f: (A) -> B): JustList<B> =
    foldRightL(JustList.empty()) { head, tail -> JustList.Construct(f(head), tail) }

private fun <A, B> JustList<A>.flatMap(f: (A) -> JustList<B>): JustList<B> =
    foldRightL(JustList.empty()) { head, tail -> f(head).append(tail) }

private fun <T> JustList<T>.filter2(predicate: (T) -> Boolean): JustList<T> =
    foldRightL(JustList.empty()) { head, tail -> if (predicate(head)) JustList.Construct(head, tail) else tail }

private fun <T> JustList<T>.filter(predicate: (T) -> Boolean): JustList<T> =
    flatMap { head -> if (predicate(head)) JustList.of(head) else JustList.Nil }

private fun <A, B> JustList<A>.zipWith(other: JustList<A>, f: (A, A) -> B): JustList<B> =
    when(this) {
        JustList.Nil -> JustList.empty()
        is JustList.Construct -> when(other) {
            JustList.Nil -> JustList.empty()
            is JustList.Construct -> JustList.Construct(f(head, other.head), tail.zipWith(other.tail, f))
        }
    }

private tailrec fun <T> JustList<T>.hasSubsequence(other: JustList<T>): Boolean = when(this) {
    JustList.Nil -> false
    is JustList.Construct -> when(other) {
        JustList.Nil -> true
        is JustList.Construct -> if (head == other.head) {
            tail.hasSubsequence(other.tail)
        } else {
            tail.hasSubsequence(other)
        }
    }
}
