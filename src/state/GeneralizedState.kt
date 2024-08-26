package state

import list.JustList
import list.foldRightL

data class State<S, out A>(val run: (S) -> Pair<A, S>)

fun <S, A> unit(value: A): State<S, A> = State { state -> value to state }

fun <S, A> State<S, A>.print(z: S, s: (S) -> Unit, a: (A) -> Unit) {
    s(run(z).second)
    map(a)
}

fun <S, A, B> State<S, A>.map(f: (A) -> B): State<S, B> =
    State { state ->
        val (value, newState) = run(state)
        f(value) to newState
    }

fun <S, A, B> State<S, A>.flatMap(f: (A) -> State<S, B>): State<S, B> =
    State { state ->
        val (value, newState) = run(state)
        f(value).run(newState)
    }

fun <S, A, B, C> State<S, A>.map2(other: State<S, B>, f: (A, B) -> C): State<S, C> =
    flatMap { valueA ->
        other.map { valueB ->
            f(valueA, valueB)
        }
    }

// We begin to sacrifice readability as a result of the escalating complexity of our functional code
fun <S, A> sequence(fs: JustList<State<S, A>>): State<S, JustList<A>> =
    fs.foldRightL(State { state -> JustList.empty<A>() to state }) { state, next ->
        state.map2(next) { a, b -> JustList.construct(a, b) }
    }