sealed interface Partial<out E, out T> {
    data class Failures<out E>(val get: List<E>): Partial<E, Nothing>
    data class Success<out T>(val get: T): Partial<Nothing, T>
}