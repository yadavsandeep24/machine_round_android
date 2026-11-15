package com.example.starter.testutil

import com.example.starter.core.network.auth.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryTokenStore(access: String? = null, refresh: String? = null) : TokenStore {
    private val tokens = MutableStateFlow(access to refresh)

    override val isLoggedIn: Flow<Boolean> = tokens.map { it.first != null }
    override suspend fun accessToken() = tokens.value.first
    override suspend fun refreshToken() = tokens.value.second
    override suspend fun save(accessToken: String, refreshToken: String) {
        tokens.value = accessToken to refreshToken
    }
    override suspend fun clear() {
        tokens.value = null to null
    }
}
