sealed interface JustStream<out A> {
    data class Construct<out A>(
        val head: () -> A,
        val tail: () -> JustStream<A>,
    ) : JustStream<A>

    data object Empty : JustStream<Nothing>

    companion object {
        fun <A> construct(hd: () -> A, tl: () -> JustStream<A>): Construct<A> {
            val head by lazy(hd)
            val tail by lazy(tl)
            return Construct({ head }, { tail })
        }

        fun <A> empty(): JustStream<A> = Empty

        fun <A> of(vararg xs: A): JustStream<A> {
            val tail = xs.sliceArray(1..<xs.size)
            return if (xs.isEmpty()) empty() else construct({ xs[0] }, { of(*tail) })
        }
    }
}

// This function could cause stack overflow exception
private fun <A> JustStream<A>.toJustListUnsafe(): JustList<A> = when (this) {
    JustStream.Empty -> JustList.empty()
    is JustStream.Construct -> JustList.Construct(head(), tail().toJustListUnsafe())
}

private fun <A> JustStream<A>.toJustListWhileLoop(): JustList<A> {
    var currentStream = this
    var result: JustList<A> = JustList.empty()
    while (currentStream != JustStream.Empty) {
        currentStream as JustStream.Construct
        result = result.append(JustList.Construct(currentStream.head(), JustList.empty()))
        currentStream = currentStream.tail()
    }
    return result
}

private fun <A> JustStream<A>.toJustList(): JustList<A> {
    tailrec fun go(xs: JustStream<A>, acc: JustList<A>): JustList<A> = when (xs) {
        JustStream.Empty -> acc
        is JustStream.Construct -> go(xs.tail(), JustList.Construct(xs.head(), acc))
    }
    return go(this, JustList.empty()).reversed()
}

private fun <A> JustStream<A>.take(n: Int): JustStream<A> {
    if (n == 0) return JustStream.empty()
    return when (this) {
        JustStream.Empty -> JustStream.empty()
        is JustStream.Construct -> JustStream.construct(head) { tail().take(n - 1) }
    }
}

private fun <A> JustStream<A>.takeWhile(f: (A) -> Boolean): JustStream<A> = when (this) {
    JustStream.Empty -> JustStream.empty()
    is JustStream.Construct -> if (f(head())) {
        JustStream.construct(head) { tail().takeWhile(f) }
    } else JustStream.empty()
}

private fun <A> JustStream<A>.drop(n: Int): JustStream<A> {
    if (n == 0) return this
    return when (this) {
        JustStream.Empty -> throw IllegalArgumentException("Cannot drop more elements than in this JustStreams")
        is JustStream.Construct -> tail().drop(n - 1)
    }
}

private fun <A, B> JustStream<A>.foldRight(z: () -> B, f: (A, () -> B) -> B): B = when (this) {
    JustStream.Empty -> z()
    is JustStream.Construct -> f(head()) { tail().foldRight(z, f) }
}

private fun <A> JustStream<A>.exists(predicate: (A) -> Boolean): Boolean =
    foldRight({ false }) { currentElement, remainingStream -> predicate(currentElement) || remainingStream() }

private fun <A> JustStream<A>.forAll(predicate: (A) -> Boolean): Boolean =
    foldRight({ true }) { currentElement, remainingStream -> predicate(currentElement) && remainingStream() }

private fun <A> JustStream<A>.takeWhileF(predicate: (A) -> Boolean): JustStream<A> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        if (predicate(currentElement)) JustStream.construct({ currentElement }, remainingStream) else JustStream.empty()
    }

private fun <A> JustStream<A>.headOption(): JustOption<A> = JustOption.empty()

private fun <A, B> JustStream<A>.map(f: (A) -> B): JustStream<B> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        JustStream.construct({ f(currentElement) }, remainingStream)
    }

private fun <A> JustStream<A>.filter(f: (A) -> Boolean): JustStream<A> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        if (f(currentElement)) JustStream.construct({ currentElement }, remainingStream) else remainingStream()
    }

private fun <A> JustStream<A>.append(other: JustStream<A>): JustStream<A> =
    foldRight({ other }) { currentElement, remainingStream ->
        JustStream.construct({ currentElement }, remainingStream)
    }

private fun <A, B> JustStream<A>.flatMap(f: (A) -> JustStream<B>): JustStream<B> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        remainingStream()
        f(currentElement)
    }

fun main() {
    val justStreams = JustStream.of(1, 2, 3, 4, 5, 6)
    val justStreamsEven = JustStream.of(2, 4, 7, 8)
//    println(justStreams.toJustListUnsafe())
//    println(justStreams.toJustListWhileLoop())
//    println(justStreams.toJustList())
//
//    println("--------")
//
//    println(justStreams.take(3).toJustList())
//    println(justStreams.drop(3).toJustList())
//
//    println("---------")
//    println(justStreams.takeWhile { it == 1 || it == 2 }.toJustList())

//    println("---------")
//    println(justStreamsEven.takeWhileF {
//        println(it)
//        it % 2 == 0
//    }.toJustList())
//    println(justStreams.filter { it % 2 == 0 }.toJustList())
//    println(justStreams.append(justStreamsEven).toJustList())
    println(justStreams.flatMap { JustStream.construct({ it * 2 }, { JustStream.empty() }) }.toJustList())
}