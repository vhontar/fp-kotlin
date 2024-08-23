sealed interface Option<out T>

data class Some<out T>(val value: T) : Option<T>

data object None : Option<Nothing>

fun <A, B> Option<A>.map(f: (A) -> B): Option<B> = when (this) {
    None -> this as None
    is Some -> Some(f(value))
}

fun <A> Option<A>.getOrElse(default: () -> A): A = when (this) {
    None -> default()
    is Some -> value
}

fun <A, B> Option<A>.flatMap(f: (A) -> Option<B>): Option<B> = map(f).getOrElse { None }

fun <A> Option<A>.orElse(ob: () -> Option<A>): Option<A> = map { Some(it) }.getOrElse { ob() }

fun <A> Option<A>.filter(f: (A) -> Boolean): Option<A> = flatMap { if (f(it)) this else None }