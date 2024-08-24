sealed interface Partial<E, T>

data class Failures<E>(val get: List<E>): Partial<E, Nothing>

data class Success<T>(val get: T): Partial<Nothing, T>