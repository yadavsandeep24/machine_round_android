package com.example.signing.internal

import com.example.signing.SigningEvent
import com.example.signing.SigningEventListener
import com.example.signing.SyncReport
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Drains the offline outbox. Guarantees:
 *  - at-least-once delivery + server de-duplication by requestId = effectively exactly-once
 *  - one sync at a time (Mutex), so two callers never submit the same item concurrently
 *  - a network failure on one item doesn't stop the others
 *  - envelopes voided while the user was offline are rejected, not retried forever
 */
internal class OfflineSyncer(
    private val store: OfflineStore,
    private val service: EnvelopeService,
    private val tokens: TokenManager,
    private val events: SigningEventListener
) {
    private val mutex = Mutex()

    suspend fun sync(): SyncReport = mutex.withLock {
        var report = SyncReport(synced = 0, failed = 0, rejected = 0)
        for (item in store.data.value.outbox.filterNot { it.rejected }) {
            report = when (submit(item)) {
                null -> report.copy(failed = report.failed + 1)
                SubmitResult.ACCEPTED, SubmitResult.ALREADY_APPLIED -> {
                    markSynced(item)
                    report.copy(synced = report.synced + 1)
                }
                SubmitResult.VOIDED -> {
                    markRejected(item)
                    report.copy(rejected = report.rejected + 1)
                }
            }
        }
        report
    }

    /** Returns null on a network failure (already recorded on the item). */
    private suspend fun submit(item: OutboxItem): SubmitResult? = try {
        tokens.withToken { token ->
            service.submitOfflineSignature(token, OfflineSubmission(item.requestId, item.envelopeId, item.signerName, item.signedAtMillis))
        }
    } catch (e: IOException) {
        val reason = Redactor.redact(e.message ?: "Network error")
        updateItem(item) { it.copy(attempts = it.attempts + 1, lastError = reason) }
        events.onEvent(SigningEvent.SyncFailed(item.envelopeId, reason))
        null
    }

    private suspend fun markSynced(item: OutboxItem) {
        store.update { data ->
            data.copy(
                outbox = data.outbox.filterNot { it.requestId == item.requestId },
                offlineDocs = data.offlineDocs - item.envelopeId, // signed: no need to keep the copy
                envelopes = data.envelopes.withStatus(item.envelopeId, InMemoryEnvelopeService.STATUS_COMPLETED)
            )
        }
        events.onEvent(SigningEvent.EnvelopeSynced(item.envelopeId))
    }

    private suspend fun markRejected(item: OutboxItem) {
        store.update { data ->
            data.copy(
                outbox = data.outbox.map { if (it.requestId == item.requestId) it.copy(rejected = true, lastError = null) else it },
                envelopes = data.envelopes.withStatus(item.envelopeId, InMemoryEnvelopeService.STATUS_VOIDED)
            )
        }
        events.onEvent(SigningEvent.EnvelopeVoided(item.envelopeId))
    }

    private suspend fun updateItem(item: OutboxItem, change: (OutboxItem) -> OutboxItem) = store.update { data ->
        data.copy(outbox = data.outbox.map { if (it.requestId == item.requestId) change(it) else it })
    }

    private fun List<EnvelopeDto>.withStatus(id: String, status: String) = map { if (it.id == id) it.copy(status = status) else it }
}
