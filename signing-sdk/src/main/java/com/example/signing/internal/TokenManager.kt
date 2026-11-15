package com.example.signing.internal

import com.example.signing.TokenProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class UnauthorizedException : Exception("401")

/**
 * Caches the host-provided token and refreshes it:
 *  - proactively, [skewMillis] before expiry (clock skew, slow networks)
 *  - reactively, once, if the server says 401 anyway
 * Single-flight: concurrent callers share one refresh (Mutex + re-check inside the lock).
 */
internal class TokenManager(
    private val provider: TokenProvider,
    private val clock: () -> Long,
    private val skewMillis: Long = DEFAULT_SKEW_MS,
    private val onRefreshed: () -> Unit = {}
) {
    private val mutex = Mutex()

    @Volatile private var cached: com.example.signing.AccessToken? = null
    var fetchCount = 0
        private set

    suspend fun token(): String {
        validCached()?.let { return it }
        return mutex.withLock {
            validCached() ?: provider.fetchToken().also {
                cached = it
                fetchCount++
                onRefreshed()
            }.value
        }
    }

    /** Runs [block] with a token; on 401 invalidates that token and retries exactly once. */
    @Suppress("SwallowedException") // the 401 is the signal to retry; a second 401 propagates
    suspend fun <T> withToken(block: suspend (String) -> T): T {
        val token = token()
        return try {
            block(token)
        } catch (e: UnauthorizedException) {
            invalidate(token)
            block(token())
        }
    }

    private fun validCached(): String? = cached?.takeIf { it.expiresAtMillis - skewMillis > clock() }?.value

    /** Only drops the token if it's still the one that failed (another caller may have refreshed already). */
    private suspend fun invalidate(stale: String) = mutex.withLock {
        if (cached?.value == stale) cached = null
    }

    private companion object {
        const val DEFAULT_SKEW_MS = 60_000L
    }
}
