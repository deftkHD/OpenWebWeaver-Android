package de.deftk.openlonet.api

sealed class Response<out T> {

    data class Success<out T>(val value: T) : Response<T>() {
        fun <K> map(block: (T) -> K): Success<K> = Success(block(value))
    }

    data class Failure(val exception: Exception) : Response<Nothing>()

    fun <K> smartMap(block: (T) -> K): Response<K> {
        return when (this) {
            is Success -> Success(block(value))
            is Failure -> this
        }
    }

    fun valueOrNull(): T? {
        return when (this) {
            is Success -> value
            is Failure -> null
        }
    }

}

fun <T> Response<T>?.success(): Boolean = this is Response.Success<*>
fun <T> Response<T>?.failure(): Boolean = this is Response.Failure