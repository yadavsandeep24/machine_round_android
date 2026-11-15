package com.example.starter.core.network.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface TokenStore {
    /** Observe this at the app root to send the user to login when the session ends. */
    val isLoggedIn: Flow<Boolean>
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun save(accessToken: String, refreshToken: String)
    suspend fun clear()
}

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * DataStore (not SharedPreferences): async, transactional, Flow-based.
 * Production: encrypt the values (e.g. Tink) before writing.
 */
@Singleton
class DataStoreTokenStore @Inject constructor(@ApplicationContext context: Context) : TokenStore {
    private val store = context.authDataStore

    override val isLoggedIn: Flow<Boolean> = store.data.map { it[ACCESS] != null }

    override suspend fun accessToken(): String? = store.data.first()[ACCESS]
    override suspend fun refreshToken(): String? = store.data.first()[REFRESH]

    override suspend fun save(accessToken: String, refreshToken: String) {
        store.edit {
            it[ACCESS] = accessToken
            it[REFRESH] = refreshToken
        }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
    }
}
