sealed class JustTree<out T>

data class Leaf<T>(val value: T) : JustTree<T>() {
    companion object {
        fun <T> create(value: T): JustTree<T> = Leaf(value)
    }
}

data class Branch<T>(val left: JustTree<T>, val right: JustTree<T>) : JustTree<T>()

private fun <T> JustTree<T>.size(): Int = when (this) {
    is Leaf -> 1
    is Branch -> 1 + left.size() + right.size()
}

private fun <T> JustTree<T>.max(f: (T, T) -> T): T = when (this) {
    is Leaf -> value
    is Branch -> f(left.max(f), right.max(f))
}

private fun <T> JustTree<T>.depth(): Int = when (this) {
    is Leaf -> 0
    is Branch -> 1 + maxOf(left.depth(), right.depth())
}

private fun <A, B> JustTree<A>.map(f: (A) -> B): JustTree<B> = when (this) {
    is Leaf -> Leaf(f(value))
    is Branch -> Branch(left.map(f), right.map(f))
}

private fun <A, B> JustTree<A>.fold(leaf: (A) -> B, branch: (B, B) -> B): B = when (this) {
    is Leaf -> leaf(value)
    is Branch -> branch(left.fold(leaf, branch), right.fold(leaf, branch))
}

private fun <T> JustTree<T>.sizeF(): Int = fold(
    leaf = { 1 },
    branch = { a, b -> 1 + a + b },
)

private fun <T> JustTree<T>.depthF(): Int = fold(
    leaf = { 0 },
    branch = { a, b -> 1 + maxOf(a, b) },
)

private fun <T> JustTree<T>.maxF(f: (T, T) -> T): T = fold(
    leaf = { it },
    branch = { a, b -> f(a, b) },
)

private fun <A, B> JustTree<A>.mapF(f: (A) -> B): JustTree<B> = fold(
    leaf = { a -> Leaf.create(f(a)) },
    branch = { a, b -> Branch(a, b) },
)

