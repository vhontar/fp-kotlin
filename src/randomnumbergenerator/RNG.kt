package randomnumbergenerator

import list.JustList
import list.foldRight
import list.foldRightL

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

// taken from wikipedia
data class SimpleRNG(private val seed: Long) : RNG {

    val intR: Rand<Int> = { rng -> rng.nextInt() }

    fun <A> unit(a: A): Rand<A> = { rng -> a to rng }

    override fun nextInt(): Pair<Int, RNG> {
        val newSeed = (seed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
        val nextRNG = SimpleRNG(newSeed)
        val n = (newSeed ushr 16).toInt()
        return n to nextRNG
    }
}

typealias Rand<A> = (RNG) -> Pair<A, RNG>

fun <A> unit(a: A): Rand<A> = { rng -> a to rng }

fun RNG.nonNegativeInt(): Pair<Int, RNG> {
    val (number, rng) = nextInt()
    val nonNegativeNumber = if (number < 0) -(number + 1) else number
    return nonNegativeNumber to rng
}

fun RNG.double(): Pair<Double, RNG> {
    val (number, rng) = nonNegativeInt()
    val double = number / (Int.MAX_VALUE.toDouble() + 1) // cover edge case
    return double to rng
}

fun RNG.intDouble(): Pair<Pair<Int, Double>, RNG> {
    val (int, rng) = nonNegativeInt()
    val (double, rng2) = rng.double()
    return (int to double) to rng2
}

fun RNG.doubleInt(): Pair<Pair<Double, Int>, RNG> {
    val (double, rng) = double()
    val (int, rng2) = rng.nonNegativeInt()
    return (double to int) to rng2
}

fun RNG.double3(): Pair<Triple<Double, Double, Double>, RNG> {
    val (double1, rng1) = double()
    val (double2, rng2) = rng1.double()
    val (double3, rng3) = rng2.double()
    return Triple(double1, double2, double3) to rng3
}

fun RNG.ints(count: Int): Pair<List<Int>, RNG> {
    val ints = mutableListOf<Int>()
    var currentRng = this
    repeat((0..<count).count()) {
        val (int, rng) = currentRng.nextInt()
        ints.add(int)
        currentRng = rng
    }

    return ints to currentRng
}

fun RNG.ints2(count: Int): Pair<JustList<Int>, RNG> = when {
    count > 0 -> {
        val (num1, rng1) = nextInt()
        val (num2, rng2) = rng1.ints2(count - 1)
        JustList.Construct(num1, num2) to rng2
    }

    else -> JustList.Nil to this
}

fun <A, B> map(s: Rand<A>, f: (A) -> B): Rand<B> = { rng ->
    val (num, rng1) = s(rng)
    f(num) to rng1
}

fun <A, B> mapF(s: Rand<A>, f: (A) -> B): Rand<B> =
    flatMap(s) { unit(f(it)) }

fun <A, B> flatMap(s: Rand<A>, f: (A) -> Rand<B>): Rand<B> = { rng ->
    val (num, rng1) = s(rng)
    f(num)(rng1)
}

fun nonNegativeInt2(rng: RNG): Pair<Int, RNG> = rng.nonNegativeInt()

fun doubleR(): Rand<Double> =
    map(::nonNegativeInt2) { it / (Int.MAX_VALUE.toDouble() + 1) }

fun <A, B, C> map2(ra: Rand<A>, rb: Rand<B>, f: (A, B) -> C): Rand<C> =
    flatMap(ra) { a ->
        map(rb) { b -> f(a, b) }
    }

fun <A, B> both(ra: Rand<A>, rb: Rand<B>): Rand<Pair<A, B>> =
    map2(ra, rb) { a, b -> a to b }

fun <A, B> traverse(fs: JustList<A>, f: (A) -> Rand<B>): Rand<JustList<B>> = when (fs) {
    JustList.Nil -> { rng -> JustList.empty<B>() to rng }
    is JustList.Construct -> map2(f(fs.head), traverse(fs.tail, f)) { a, b -> JustList.construct(a, b) }
}

fun <A> sequence2(fs: JustList<Rand<A>>): Rand<JustList<A>> =
    fs.foldRightL({ rng -> JustList.empty<A>() to rng }) { a, b ->
        map2(a, b) { x, y -> JustList.construct(x, y) }
    }

fun nonNegativeIntLessThen(n: Int): Rand<Int> =
    flatMap(::nonNegativeInt2) { number ->
        val mod = number % n
        if (number + (n - 1) - mod >= 0) {
            unit(mod)
        } else nonNegativeIntLessThen(n)
    }

fun main() {

}