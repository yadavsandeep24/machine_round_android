package com.example.signing

import kotlinx.coroutines.flow.Flow

/**
 * The SDK's entry point. An interface so host apps can fake it in their own tests.
 * All suspend functions are main-safe and throw only [SigningException] (or
 * IllegalArgumentException for invalid input).
 */
public interface SigningClient {
    /** Envelopes merged with local offline state. Works offline from the last known list. */
    public val envelopes: Flow<List<Envelope>>

    /** Fetches the latest envelope list from the server. */
    public suspend fun refresh()

    /** Downloads the document so it can be signed with no network. */
    public suspend fun makeAvailableOffline(envelopeId: String)

    /** Signs a downloaded envelope locally. Idempotent: signing twice keeps the first signature. */
    public suspend fun signOffline(envelopeId: String, signerName: String)

    /** Submits offline signatures. Safe to call concurrently and repeatedly. */
    public suspend fun syncPending(): SyncReport

    /** Creates an online signing ceremony to launch with [SignEnvelope]. */
    public suspend fun createSigningSession(envelopeId: String, recipient: Recipient): SigningSession

    /** Pass the result from [SignEnvelope] back so the SDK can refresh the envelope's state. */
    public suspend fun handleResult(result: SigningResult)
}
