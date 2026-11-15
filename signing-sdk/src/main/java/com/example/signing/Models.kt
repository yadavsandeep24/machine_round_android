package com.example.signing

/** Short-lived access token issued by the host app's backend (e.g. a 1-hour JWT). */
public data class AccessToken(val value: String, val expiresAtMillis: Long)

public enum class EnvelopeStatus {
    /** Waiting for this user's signature. */
    SENT,

    /** Signed offline on this device; will be submitted on the next sync. */
    SIGNED_PENDING_SYNC,
    COMPLETED,

    /** Cancelled by the sender. Offline signatures for it are rejected. */
    VOIDED
}

public data class Envelope(
    val id: String,
    val title: String,
    val sender: String,
    val status: EnvelopeStatus,
    /** True if the document is stored on the device and can be signed with no network. */
    val availableOffline: Boolean
)

public data class Recipient(val name: String, val email: String)

/** Outcome of [SigningClient.syncPending]. */
public data class SyncReport(val synced: Int, val failed: Int, val rejected: Int) {
    val hasFailures: Boolean get() = failed > 0
}

/** A short-lived online signing ceremony, launched with [SignEnvelope]. */
public data class SigningSession(val envelopeId: String, val url: String)

public sealed interface SigningResult {
    public data class Completed(val envelopeId: String) : SigningResult
    public data class Declined(val envelopeId: String, val reason: String) : SigningResult

    /** [envelopeId] is null only if the system finished the screen without a result. */
    public data class Cancelled(val envelopeId: String?) : SigningResult
    public data class Failed(val envelopeId: String, val message: String) : SigningResult
}

/** Telemetry for the host app. Events never contain signer names or emails. */
public sealed interface SigningEvent {
    public data object TokenRefreshed : SigningEvent
    public data class EnvelopeSynced(val envelopeId: String) : SigningEvent
    public data class EnvelopeVoided(val envelopeId: String) : SigningEvent
    public data class SyncFailed(val envelopeId: String, val reason: String) : SigningEvent
}
