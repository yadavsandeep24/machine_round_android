package com.example.signing.internal

import kotlinx.serialization.Serializable

/** Transport boundary. Production: Retrofit/OkHttp against the eSignature REST API. */
internal interface EnvelopeService {
    suspend fun listEnvelopes(token: String): List<EnvelopeDto>
    suspend fun fetchDocument(token: String, envelopeId: String): OfflineDocument
    suspend fun createSigningSession(token: String, envelopeId: String, recipientEmail: String): String
    suspend fun submitOfflineSignature(token: String, submission: OfflineSubmission): SubmitResult

    /** Demo only: real backends learn about completion from server-side webhooks. */
    suspend fun confirmOnlineCompletion(token: String, envelopeId: String)
}

@Serializable
internal data class EnvelopeDto(val id: String, val title: String, val sender: String, val status: String)

@Serializable
internal data class OfflineDocument(val envelopeId: String, val title: String, val pages: List<String>)

internal data class OfflineSubmission(val requestId: String, val envelopeId: String, val signerName: String, val signedAtMillis: Long)

internal enum class SubmitResult { ACCEPTED, ALREADY_APPLIED, VOIDED }

internal class EnvelopeNotFoundException(val envelopeId: String) : Exception(envelopeId)
