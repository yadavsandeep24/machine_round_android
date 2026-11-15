package com.example.signing.internal

import com.example.signing.AccessToken
import com.example.signing.EnvelopeStatus
import com.example.signing.SigningEvent
import com.example.signing.SigningException
import com.example.signing.TokenProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultSigningClientTest {

    @get:Rule val tmp = TemporaryFolder()

    private val service = InMemoryEnvelopeService(latencyMs = 50)
    private val events = mutableListOf<SigningEvent>()
    private var tokenCounter = 0
    private val provider = TokenProvider { AccessToken("tok-${++tokenCounter}", Long.MAX_VALUE) }
    private var requestIds = 0

    private fun TestScope.client() = DefaultSigningClient(
        service = service,
        store = OfflineStore(File(tmp.root, "offline.json"), Dispatchers.Unconfined),
        tokens = TokenManager(provider, clock = { testScheduler.currentTime }),
        events = { events += it },
        clock = { testScheduler.currentTime },
        newRequestId = { "req-${++requestIds}" }
    )

    private suspend fun DefaultSigningClient.status(id: String) = envelopes.first().first { it.id == id }

    private suspend fun DefaultSigningClient.signedOffline(id: String) {
        refresh()
        makeAvailableOffline(id)
        signOffline(id, "Asha Rao")
    }

    @Test
    fun `refresh loads envelopes, offline download marks them available`() = runTest {
        val client = client()
        client.refresh()
        assertThat(client.envelopes.first()).hasSize(5)
        client.makeAvailableOffline("env-1001")
        assertThat(client.status("env-1001").availableOffline).isTrue()
    }

    @Test
    fun `signing offline requires a downloaded copy`() = runTest {
        val client = client()
        client.refresh()
        try {
            client.signOffline("env-1001", "Asha")
            fail("expected NotAvailableOffline")
        } catch (e: SigningException.NotAvailableOffline) {
            assertThat(e.envelopeId).isEqualTo("env-1001")
        }
    }

    @Test
    fun `offline signature is pending, then sync completes it`() = runTest {
        val client = client()
        client.signedOffline("env-1001")
        assertThat(client.status("env-1001").status).isEqualTo(EnvelopeStatus.SIGNED_PENDING_SYNC)

        val report = client.syncPending()
        assertThat(report.synced).isEqualTo(1)
        assertThat(client.status("env-1001").status).isEqualTo(EnvelopeStatus.COMPLETED)
        assertThat(client.status("env-1001").availableOffline).isFalse() // local copy cleaned up
        assertThat(events).contains(SigningEvent.EnvelopeSynced("env-1001"))
    }

    @Test
    fun `signing twice keeps the first signature`() = runTest {
        val client = client()
        client.signedOffline("env-1001")
        client.signOffline("env-1001", "Someone Else")
        client.syncPending()
        assertThat(service.applyCount).isEqualTo(1)
    }

    @Test
    fun `network failure keeps the signature queued and reports it`() = runTest {
        val client = client()
        client.signedOffline("env-1001")
        service.failuresRemaining = 1
        val report = client.syncPending()
        assertThat(report.failed).isEqualTo(1)
        assertThat(client.status("env-1001").status).isEqualTo(EnvelopeStatus.SIGNED_PENDING_SYNC)
        assertThat(events.filterIsInstance<SigningEvent.SyncFailed>()).hasSize(1)

        assertThat(client.syncPending().synced).isEqualTo(1)
    }

    @Test
    fun `envelope voided while offline is rejected, not retried`() = runTest {
        val client = client()
        client.signedOffline("env-1002")
        service.void("env-1002")
        assertThat(client.syncPending().rejected).isEqualTo(1)
        assertThat(client.status("env-1002").status).isEqualTo(EnvelopeStatus.VOIDED)
        assertThat(client.syncPending()).isEqualTo(com.example.signing.SyncReport(0, 0, 0))
    }

    @Test
    fun `retry after a lost response does not sign twice`() = runTest {
        val client = client()
        client.signedOffline("env-1001")
        service.appliedRequestIds += "req-1" // server applied it, but the response never arrived
        assertThat(client.syncPending().synced).isEqualTo(1)
        assertThat(service.applyCount).isEqualTo(0) // de-duplicated, not applied again
    }

    @Test
    fun `concurrent syncs submit each signature once`() = runTest {
        val client = client()
        client.signedOffline("env-1001")
        listOf(async { client.syncPending() }, async { client.syncPending() }).awaitAll()
        assertThat(service.applyCount).isEqualTo(1)
    }

    @Test
    fun `pending signatures survive an app restart`() = runTest {
        client().signedOffline("env-1003")
        val afterRestart = client() // new instance, same file
        assertThat(afterRestart.status("env-1003").status).isEqualTo(EnvelopeStatus.SIGNED_PENDING_SYNC)
        assertThat(afterRestart.syncPending().synced).isEqualTo(1)
    }

    @Test
    fun `revoked token is refreshed transparently`() = runTest {
        val client = client()
        client.refresh() // uses tok-1
        service.revokedTokens += "tok-1"
        client.refresh() // 401 -> tok-2 -> success
        assertThat(tokenCounter).isEqualTo(2)
    }

    @Test
    fun `transport errors surface only as public SigningException types`() = runTest {
        val client = client()
        service.failuresRemaining = 1
        try {
            client.refresh()
            fail("expected Network")
        } catch (e: SigningException.Network) {
            assertThat(e.cause).isInstanceOf(java.io.IOException::class.java)
        }
    }
}
