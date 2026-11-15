package com.example.starter.core.network

import com.example.starter.core.common.AppResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class SafeCallTest {

    @Test
    fun `success is wrapped`() = runTest {
        assertThat(safeCall { 42 }).isEqualTo(AppResult.Success(42))
    }

    @Test
    fun `exceptions become Failure`() = runTest {
        val result = safeCall { error("boom") }
        assertThat((result as AppResult.Failure).throwable).hasMessageThat().isEqualTo("boom")
    }

    @Test
    fun `cancellation is rethrown, never swallowed`() {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { safeCall { throw CancellationException("cancelled") } }
        }
    }
}
