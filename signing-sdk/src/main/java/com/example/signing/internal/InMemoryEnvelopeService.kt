package com.example.signing.internal

import java.io.IOException
import kotlinx.coroutines.delay

/** Demo backend: deterministic envelopes, latency, failure injection, token revocation, idempotency. */
internal class InMemoryEnvelopeService(private val latencyMs: Long = DEFAULT_LATENCY_MS) : EnvelopeService {

    var failuresRemaining = 0
    val revokedTokens = mutableSetOf<String>()
    val appliedRequestIds = mutableSetOf<String>()
    var applyCount = 0
        private set

    private val envelopes = linkedMapOf(
        "env-1001" to EnvelopeDto("env-1001", "Mutual NDA", "Legal · Acme Corp", STATUS_SENT),
        "env-1002" to EnvelopeDto("env-1002", "Offer letter", "HR · Globex", STATUS_SENT),
        "env-1003" to EnvelopeDto("env-1003", "Rental agreement", "Mumbai Homes", STATUS_SENT),
        "env-1004" to EnvelopeDto("env-1004", "Vendor contract", "Procurement · Initech", STATUS_SENT),
        "env-1005" to EnvelopeDto("env-1005", "Parent consent form", "Springfield School", STATUS_SENT)
    )

    fun void(envelopeId: String) {
        envelopes.computeIfPresent(envelopeId) { _, e -> e.copy(status = STATUS_VOIDED) }
    }

    override suspend fun listEnvelopes(token: String): List<EnvelopeDto> {
        call(token)
        return envelopes.values.toList()
    }

    override suspend fun fetchDocument(token: String, envelopeId: String): OfflineDocument {
        call(token)
        val e = envelopes[envelopeId] ?: throw EnvelopeNotFoundException(envelopeId)
        return OfflineDocument(e.id, e.title, List(PAGES) { "Page ${it + 1} of ${e.title}" })
    }

    override suspend fun createSigningSession(token: String, envelopeId: String, recipientEmail: String): String {
        call(token)
        if (envelopeId !in envelopes) throw EnvelopeNotFoundException(envelopeId)
        return "https://appassets.androidplatform.net/assets/docusign_sdk/signing.html?envelopeId=$envelopeId"
    }

    override suspend fun submitOfflineSignature(token: String, submission: OfflineSubmission): SubmitResult {
        call(token)
        val envelope = envelopes[submission.envelopeId] ?: throw EnvelopeNotFoundException(submission.envelopeId)
        return when {
            submission.requestId in appliedRequestIds -> SubmitResult.ALREADY_APPLIED
            envelope.status == STATUS_VOIDED -> SubmitResult.VOIDED
            else -> {
                appliedRequestIds += submission.requestId
                applyCount++
                envelopes[envelope.id] = envelope.copy(status = STATUS_COMPLETED)
                SubmitResult.ACCEPTED
            }
        }
    }

    override suspend fun confirmOnlineCompletion(token: String, envelopeId: String) {
        call(token)
        envelopes.computeIfPresent(envelopeId) { _, e -> e.copy(status = STATUS_COMPLETED) }
    }

    private suspend fun call(token: String) {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
        if (token in revokedTokens) throw UnauthorizedException()
    }

    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_VOIDED = "voided"
        private const val DEFAULT_LATENCY_MS = 400L
        private const val PAGES = 3
    }
}
