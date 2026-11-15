package com.example.signing.internal

import com.example.signing.AccessToken
import com.example.signing.TokenProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class TokenManagerTest {

    private var now = 0L
    private var issued = 0

    /** Issues "t1", "t2", ... valid for 10 minutes; takes 100ms like a backend call. */
    private val provider = TokenProvider {
        delay(100)
        issued++
        AccessToken("t$issued", now + 600_000)
    }

    private fun manager() = TokenManager(provider, clock = { now }, skewMillis = 60_000)

    @Test
    fun `caches the token until shortly before expiry`() = runTest {
        val tokens = manager()
        assertThat(tokens.token()).isEqualTo("t1")
        now += 500_000 // 100s left, more than the 60s skew
        assertThat(tokens.token()).isEqualTo("t1")
        now += 50_000 // 50s left, inside the skew: refresh proactively
        assertThat(tokens.token()).isEqualTo("t2")
    }

    @Test
    fun `concurrent callers share a single refresh`() = runTest {
        val tokens = manager()
        val results = (1..10).map { async { tokens.token() } }.awaitAll()
        assertThat(results.toSet()).containsExactly("t1")
        assertThat(tokens.fetchCount).isEqualTo(1)
    }

    @Test
    fun `a 401 refreshes once and retries`() = runTest {
        val tokens = manager()
        val seen = mutableListOf<String>()
        val result = tokens.withToken { token ->
            seen += token
            if (token == "t1") throw UnauthorizedException() else "ok"
        }
        assertThat(result).isEqualTo("ok")
        assertThat(seen).containsExactly("t1", "t2").inOrder()
    }

    @Test
    fun `a second 401 propagates instead of looping`() = runTest {
        val tokens = manager()
        assertThrows(UnauthorizedException::class.java) {
            kotlinx.coroutines.runBlocking { tokens.withToken<Unit> { throw UnauthorizedException() } }
        }
    }
}
