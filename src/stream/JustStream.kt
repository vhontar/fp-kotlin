package stream

import list.JustList
import list.append
import list.reversed
import option.JustOption
import option.getOrElse
import option.isEmpty
import option.map

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
fun <A> JustStream<A>.toJustListUnsafe(): JustList<A> = when (this) {
    JustStream.Empty -> JustList.empty()
    is JustStream.Construct -> JustList.Construct(head(), tail().toJustListUnsafe())
}

fun <A> JustStream<A>.toJustListWhileLoop(): JustList<A> {
    var currentStream = this
    var result: JustList<A> = JustList.empty()
    while (currentStream != JustStream.Empty) {
        currentStream as JustStream.Construct
        result = result.append(JustList.Construct(currentStream.head(), JustList.empty()))
        currentStream = currentStream.tail()
    }
    return result
}

fun <A> JustStream<A>.toJustList(): JustList<A> {
    tailrec fun go(xs: JustStream<A>, acc: JustList<A>): JustList<A> = when (xs) {
        JustStream.Empty -> acc
        is JustStream.Construct -> go(xs.tail(), JustList.Construct(xs.head(), acc))
    }
    return go(this, JustList.empty()).reversed()
}

fun <A> JustStream<A>.take(n: Int): JustStream<A> {
    if (n == 0) return JustStream.empty()
    return when (this) {
        JustStream.Empty -> JustStream.empty()
        is JustStream.Construct -> JustStream.construct(head) { tail().take(n - 1) }
    }
}

fun <A> JustStream<A>.takeWhile(f: (A) -> Boolean): JustStream<A> = when (this) {
    JustStream.Empty -> JustStream.empty()
    is JustStream.Construct -> if (f(head())) {
        JustStream.construct(head) { tail().takeWhile(f) }
    } else JustStream.empty()
}

fun <A> JustStream<A>.drop(n: Int): JustStream<A> {
    if (n == 0) return this
    return when (this) {
        JustStream.Empty -> throw IllegalArgumentException("Cannot drop more elements than in this JustStreams")
        is JustStream.Construct -> tail().drop(n - 1)
    }
}

fun <A, B> JustStream<A>.foldRight(z: () -> B, f: (A, () -> B) -> B): B = when (this) {
    JustStream.Empty -> z()
    is JustStream.Construct -> f(head()) { tail().foldRight(z, f) }
}

fun <A> JustStream<A>.exists(predicate: (A) -> Boolean): Boolean =
    foldRight({ false }) { currentElement, remainingStream -> predicate(currentElement) || remainingStream() }

fun <A> JustStream<A>.forAll(predicate: (A) -> Boolean): Boolean =
    foldRight({ true }) { currentElement, remainingStream -> predicate(currentElement) && remainingStream() }

fun <A> JustStream<A>.takeWhileF(predicate: (A) -> Boolean): JustStream<A> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        if (predicate(currentElement)) JustStream.construct({ currentElement }, remainingStream) else JustStream.empty()
    }

fun <A> JustStream<A>.headOption(): JustOption<A> =
    foldRight({ JustOption.empty() }) { currentElement, _ -> JustOption.Some(currentElement) }

fun <A, B> JustStream<A>.map(f: (A) -> B): JustStream<B> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        JustStream.construct({ f(currentElement) }, remainingStream)
    }

fun <A> JustStream<A>.filter(f: (A) -> Boolean): JustStream<A> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        if (f(currentElement)) JustStream.construct({ currentElement }, remainingStream) else remainingStream()
    }

// append function is non-strict in its argument
fun <A> JustStream<A>.append(other: () -> JustStream<A>): JustStream<A> =
    foldRight(other) { currentElement, remainingStream ->
        JustStream.construct({ currentElement }, remainingStream)
    }

fun <A, B> JustStream<A>.flatMap(f: (A) -> JustStream<B>): JustStream<B> =
    foldRight({ JustStream.empty() }) { currentElement, remainingStream ->
        f(currentElement).append(remainingStream)
    }

fun <A> constant(a: A): JustStream<A> =
    JustStream.construct({ a }, { constant(a) })

fun from(n: Int): JustStream<Int> =
    JustStream.construct({ n }, { from(n + 1) })

fun fibs(): JustStream<Int> {
    fun go(prev: Int, current: Int): JustStream<Int> {
        return JustStream.construct({ current + prev }, { go(prev = current, current = prev + current) })
    }
    return go(prev = 0, current = 1)
}

fun <A, S> unfold(initialState: S, f: (S) -> JustOption<Pair<A, S>>): JustStream<A> =
    f(initialState).map { pair ->
        JustStream.construct({ pair.first }, { unfold(pair.second, f) })
    }.getOrElse { JustStream.empty() }

fun fibsUnfold(): JustStream<Int> =
    unfold(0 to 1) { (prev, next) ->
        val sum = prev + next
        JustOption.Some(value = sum to (next to sum))
    }

fun fromUnfold(n: Int): JustStream<Int> =
    unfold(n) { state ->
        JustOption.Some(value = state to state + 1)
    }

fun <A> constantUnfold(a: A): JustStream<A> =
    unfold(a) { state -> JustOption.Some(value = state to state) }

fun <A, B> JustStream<A>.mapUnfold(f: (A) -> B): JustStream<B> =
    unfold(initialState = this) { justStreamState ->
        when (justStreamState) {
            JustStream.Empty -> JustOption.empty()
            is JustStream.Construct -> JustOption.Some(value = f(justStreamState.head()) to justStreamState.tail())
        }
    }

fun <A> JustStream<A>.takeUnfold(n: Int): JustStream<A> =
    unfold(initialState = n to this) { (n, justStreamState) ->
        if (n == 0) JustOption.empty()
        else when (justStreamState) {
            JustStream.Empty -> JustOption.empty()
            is JustStream.Construct -> JustOption.Some(
                value = justStreamState.head() to (n - 1 to justStreamState.tail())
            )
        }
    }

fun <A> JustStream<A>.takeWhileUnfold(f: (A) -> Boolean): JustStream<A> =
    unfold(initialState = this) { justStreamState ->
        when (justStreamState) {
            JustStream.Empty -> JustOption.empty()
            is JustStream.Construct -> if (f(justStreamState.head())) {
                JustOption.Some(justStreamState.head() to justStreamState.tail())
            } else JustOption.empty()
        }
    }

fun <A, B, C> JustStream<A>.zipWith(that: JustStream<B>, f: (A, B) -> C): JustStream<C> =
    unfold(initialState = this to that) { (justStreamThis, justStreamThat) ->
        when (justStreamThis) {
            JustStream.Empty -> JustOption.empty()
            is JustStream.Construct -> when (justStreamThat) {
                JustStream.Empty -> JustOption.empty()
                is JustStream.Construct -> JustOption.Some(
                    value = f(
                        justStreamThis.head(),
                        justStreamThat.head()
                    ) to (justStreamThis.tail() to justStreamThat.tail())
                )
            }
        }
    }

fun <A, B> JustStream<A>.zipAll(that: JustStream<B>): JustStream<Pair<JustOption<A>, JustOption<B>>> =
    unfold(this to that) { (justStreamThis, justStreamThat) ->
        when (justStreamThis) {
            JustStream.Empty -> when (justStreamThat) {
                is JustStream.Construct -> {
                    val valuePair = JustOption.empty<A>() to JustOption.some(justStreamThat.head())
                    val nextStatePair = justStreamThis to justStreamThat.tail()
                    JustOption.some(value = valuePair to nextStatePair)
                }

                JustStream.Empty -> JustOption.empty()
            }

            is JustStream.Construct -> when (justStreamThat) {
                is JustStream.Construct -> {
                    val valuePair = JustOption.some(justStreamThis.head()) to JustOption.some(justStreamThat.head())
                    val nextStatePair = justStreamThis.tail() to justStreamThat.tail()
                    JustOption.some(value = valuePair to nextStatePair)
                }

                JustStream.Empty -> {
                    val valuePair = JustOption.some(justStreamThis.head()) to JustOption.empty<B>()
                    val nextStatePair = justStreamThis.tail() to justStreamThat
                    JustOption.some(value = valuePair to nextStatePair)
                }
            }
        }
    }

fun <A> JustStream<A>.startsWith(that: JustStream<A>): Boolean =
    zipWith(that) { a, b -> a == b }
        .foldRight({ true }) { currentElement, remainingStream -> currentElement && remainingStream() }

fun <A> JustStream<A>.startsWith2(that: JustStream<A>): Boolean =
    zipAll(that)
        .takeWhileF { pair -> !pair.second.isEmpty() }
        .forAll { pair -> pair.first == pair.second }

fun <A> JustStream<A>.generateTails(): JustStream<JustStream<A>> =
    unfold(initialState = this) { justStreamThis ->
        when (justStreamThis) {
            JustStream.Empty -> JustOption.empty()
            is JustStream.Construct -> JustOption.some(
                value = justStreamThis to justStreamThis.tail()
            )
        }
    }

fun <A> JustStream<A>.hasSubsequence(): Boolean =
    generateTails().exists { it.startsWith(this) }

// TODO finish, doesn't work right now
fun <A> JustStream<A>.hasSubsequence(other: JustStream<A>): Boolean =
    unfold(this to other) { (justStreamThis, justStreamOther) ->
        val result = justStreamThis.startsWith(justStreamOther)
        println("Rest: $result")
        val nextState = if (result) {
            JustStream.empty<A>() to JustStream.empty()
        } else {
            val nextThisStream = (justStreamThis as? JustStream.Construct)?.tail?.invoke() ?: JustStream.empty()
            nextThisStream to justStreamOther
        }
        JustOption.some(result to nextState)
    }.foldRight({ false }) { currentElement, remainingStream -> currentElement || remainingStream() }

fun <A, B> JustStream<A>.scanRight(z: B, f: (A, () -> B) -> B): JustStream<B> =
    foldRight({ z to JustStream.of(z) }) { currentElement, remainingStream ->
        val lazyPair by lazy(remainingStream)
        val result = f(currentElement) { lazyPair.first }
        result to JustStream.construct({ result }, { lazyPair.second })
    }.second
