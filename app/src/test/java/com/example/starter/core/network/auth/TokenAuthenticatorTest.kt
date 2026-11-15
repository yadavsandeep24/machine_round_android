package com.example.starter.core.network.auth

import com.example.starter.testutil.InMemoryTokenStore
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TokenAuthenticatorTest {

    private val server = MockWebServer()
    private val refreshCalls = AtomicInteger(0)

    /** "/data" only accepts "Bearer fresh". "/auth/refresh" returns new tokens unless [refreshStatus] != 200. */
    private var refreshStatus = 200

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    Thread.sleep(100) // make parallel requests pile up behind the lock
                    if (refreshStatus == 200) {
                        MockResponse().setBody("""{"accessToken":"fresh","refreshToken":"r2"}""")
                    } else {
                        MockResponse().setResponseCode(refreshStatus)
                    }
                }
                else ->
                    if (request.getHeader("Authorization") == "Bearer fresh") {
                        MockResponse().setBody("ok")
                    } else {
                        MockResponse().setResponseCode(401)
                    }
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun client(store: TokenStore): OkHttpClient {
        val authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(TokenAuthenticator(store, authApi))
            .build()
    }

    private fun OkHttpClient.get(): Int = newCall(Request.Builder().url(server.url("/data")).build()).execute().use { it.code }

    @Test
    fun `expired token is refreshed and the request retried`() {
        val store = InMemoryTokenStore(access = "expired", refresh = "r1")
        assertThat(client(store).get()).isEqualTo(200)
        assertThat(refreshCalls.get()).isEqualTo(1)
        assertThat(runBlocking { store.accessToken() }).isEqualTo("fresh")
        assertThat(runBlocking { store.refreshToken() }).isEqualTo("r2")
    }

    @Test
    fun `parallel 401s trigger a single refresh`() {
        val store = InMemoryTokenStore(access = "expired", refresh = "r1")
        val client = client(store)
        val pool = Executors.newFixedThreadPool(5)
        val results = (1..5).map { pool.submit<Int> { client.get() } }.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertThat(results).containsExactly(200, 200, 200, 200, 200)
        assertThat(refreshCalls.get()).isEqualTo(1)
    }

    @Test
    fun `rejected refresh token logs the user out`() {
        refreshStatus = 401
        val store = InMemoryTokenStore(access = "expired", refresh = "revoked")
        assertThat(client(store).get()).isEqualTo(401)
        assertThat(runBlocking { store.accessToken() }).isNull()
    }

    @Test
    fun `refresh server error keeps the session`() {
        refreshStatus = 500
        val store = InMemoryTokenStore(access = "expired", refresh = "r1")
        assertThat(client(store).get()).isEqualTo(401)
        assertThat(runBlocking { store.refreshToken() }).isEqualTo("r1")
    }

    @Test
    fun `no token means no auth header and no refresh`() {
        val store = InMemoryTokenStore()
        assertThat(client(store).get()).isEqualTo(401)
        assertThat(refreshCalls.get()).isEqualTo(0)
    }
}
