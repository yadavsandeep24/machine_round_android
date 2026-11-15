package com.example.starter.core.network.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Placeholder: rename to your backend's refresh endpoint and payload. */
interface AuthApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse
}

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String)
