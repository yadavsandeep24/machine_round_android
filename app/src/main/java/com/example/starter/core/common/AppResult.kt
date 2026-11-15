package com.example.starter.core.common

/**
 * Pure Kotlin result type shared by domain, data and UI.
 * Keep this file free of Android / Retrofit / logging imports: the domain layer depends on it.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val throwable: Throwable) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}
