package com.example.starter.core.network.auth

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Adds `Authorization: Bearer <token>` to every request that has a token and isn't marked [NO_AUTH]. */
class AuthInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(NO_AUTH) != null) {
            return chain.proceed(request.newBuilder().removeHeader(NO_AUTH).build())
        }
        // OkHttp interceptors are synchronous and already run off the main thread.
        val token = runBlocking { tokenStore.accessToken() } ?: return chain.proceed(request)
        return chain.proceed(request.withBearer(token))
    }

    companion object {
        /** Add `@Headers("No-Auth: true")` to public endpoints (login, register). */
        const val NO_AUTH = "No-Auth"
    }
}

internal fun Request.withBearer(token: String): Request = newBuilder().header("Authorization", "Bearer $token").build()
