package com.example.starter.core.network.auth

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import timber.log.Timber

/**
 * OkHttp calls this on every 401. It refreshes the access token once and retries the request.
 *
 * Key points interviewers ask about:
 *  - synchronized: 5 parallel requests that all get 401 cause ONE refresh, not 5.
 *  - "already refreshed" check: threads that waited reuse the new token.
 *  - retry limit: never loop forever if the new token is rejected too.
 *  - refresh call uses a separate OkHttp client WITHOUT this authenticator (no recursion).
 *  - only a rejected refresh token logs the user out; a network error doesn't.
 */
@Singleton
class TokenAuthenticator @Inject constructor(private val tokenStore: TokenStore, private val authApi: AuthApi) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null // wasn't an authed call
        if (response.priorCount() >= MAX_RETRIES) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(lock) {
            val current = runBlocking { tokenStore.accessToken() }
            if (current != null && current != failedToken) {
                return response.request.withBearer(current) // another thread refreshed while we waited
            }

            val newAccessToken = refreshTokens() ?: return null
            return response.request.withBearer(newAccessToken)
        }
    }

    /** Returns the new access token, or null if refresh wasn't possible. */
    private fun refreshTokens(): String? = runBlocking {
        val refreshToken = tokenStore.refreshToken() ?: return@runBlocking null
        try {
            val tokens = authApi.refresh(RefreshRequest(refreshToken))
            tokenStore.save(tokens.accessToken, tokens.refreshToken)
            tokens.accessToken
        } catch (e: HttpException) {
            if (e.code() in REJECTED_CODES) {
                Timber.w(e, "Refresh token rejected, logging out")
                tokenStore.clear()
            } else {
                Timber.w(e, "Refresh endpoint error ${e.code()}, keeping session")
            }
            null
        } catch (e: IOException) {
            Timber.w(e, "Token refresh failed (network), keeping session")
            null
        } catch (e: SerializationException) {
            Timber.w(e, "Unexpected refresh response, keeping session")
            null
        }
    }

    private fun Response.priorCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 2

        /** Server says the refresh token itself is bad. 5xx / timeouts are NOT a reason to log out. */
        val REJECTED_CODES = setOf(400, 401, 403)
    }
}
