package com.example.starter.core.network

import com.example.starter.core.common.AppResult
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Data-layer boundary: wraps a network/DB call so repositories never throw to callers.
 * The single deliberate catch-all in the app.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> safeCall(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Timber.w(e, "Request failed")
    AppResult.Failure(e)
}
