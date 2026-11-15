package com.example.signing.internal

import com.example.signing.SigningResult
import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Messages the signing page posts to the app through the web message bridge. */
internal sealed interface BridgeEvent {
    data object PageViewed : BridgeEvent
    data object Completed : BridgeEvent
    data class Declined(val reason: String) : BridgeEvent
    data object Cancelled : BridgeEvent
}

internal object BridgeMessageParser {
    @Serializable
    private data class Raw(val type: String, val reason: String? = null)

    private val json = Json { ignoreUnknownKeys = true }

    /** Untrusted input from a web page: anything malformed or unknown is ignored, never thrown. */
    fun parse(raw: String?): BridgeEvent? {
        val msg = raw?.let { runCatching { json.decodeFromString(Raw.serializer(), it) }.getOrNull() } ?: return null
        return when (msg.type) {
            "page_viewed" -> BridgeEvent.PageViewed
            "signing_complete" -> BridgeEvent.Completed
            "declined" -> BridgeEvent.Declined(msg.reason?.take(MAX_REASON).orEmpty().ifBlank { "No reason given" })
            "cancel" -> BridgeEvent.Cancelled
            else -> null
        }
    }

    private const val MAX_REASON = 500
}

/**
 * Exactly-once result delivery. Pages can post "complete" twice (double tap, retries, reloads)
 * and activities get recreated; the host must still receive ONE terminal result.
 * [delivered] is saved in instance state so a recreated activity can't deliver again.
 */
internal class SigningResultGate(private val envelopeId: String, delivered: Boolean = false) {
    var delivered: Boolean = delivered
        private set

    fun onEvent(event: BridgeEvent): SigningResult? {
        if (delivered) return null
        val result = when (event) {
            BridgeEvent.PageViewed -> return null // progress, not terminal
            BridgeEvent.Completed -> SigningResult.Completed(envelopeId)
            is BridgeEvent.Declined -> SigningResult.Declined(envelopeId, event.reason)
            BridgeEvent.Cancelled -> SigningResult.Cancelled(envelopeId)
        }
        delivered = true
        return result
    }

    fun fail(message: String): SigningResult? {
        if (delivered) return null
        delivered = true
        return SigningResult.Failed(envelopeId, message)
    }
}

/** Only https pages on allow-listed hosts may load or talk to the bridge. */
internal class OriginPolicy(private val allowedHosts: Set<String>) {
    val origins: Set<String> get() = allowedHosts.mapTo(linkedSetOf()) { "https://$it" }

    fun isAllowed(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && uri.host?.lowercase() in allowedHosts && uri.userInfo == null
    }

    companion object {
        /** Production would list the signing domains; the demo serves the page from app assets. */
        val DEFAULT = OriginPolicy(setOf("appassets.androidplatform.net"))
    }
}
