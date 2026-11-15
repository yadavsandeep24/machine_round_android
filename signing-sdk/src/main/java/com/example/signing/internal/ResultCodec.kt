package com.example.signing.internal

import android.content.Intent
import com.example.signing.SigningResult

internal object ResultCodec {
    private const val KEY_TYPE = "ds_result_type"
    private const val KEY_ENVELOPE = "ds_envelope_id"
    private const val KEY_MESSAGE = "ds_message"

    fun toIntent(result: SigningResult): Intent = Intent().apply {
        when (result) {
            is SigningResult.Completed -> putExtra(KEY_TYPE, "completed").putExtra(KEY_ENVELOPE, result.envelopeId)
            is SigningResult.Declined -> putExtra(
                KEY_TYPE,
                "declined"
            ).putExtra(KEY_ENVELOPE, result.envelopeId).putExtra(KEY_MESSAGE, result.reason)
            is SigningResult.Cancelled -> putExtra(KEY_TYPE, "cancelled").putExtra(KEY_ENVELOPE, result.envelopeId)
            is SigningResult.Failed -> putExtra(
                KEY_TYPE,
                "failed"
            ).putExtra(KEY_ENVELOPE, result.envelopeId).putExtra(KEY_MESSAGE, result.message)
        }
    }

    fun fromIntent(intent: Intent?): SigningResult? {
        intent ?: return null
        val envelopeId = intent.getStringExtra(KEY_ENVELOPE)
        val message = intent.getStringExtra(KEY_MESSAGE).orEmpty()
        return when (intent.getStringExtra(KEY_TYPE)) {
            "completed" -> envelopeId?.let { SigningResult.Completed(it) }
            "declined" -> envelopeId?.let { SigningResult.Declined(it, message) }
            "cancelled" -> SigningResult.Cancelled(envelopeId)
            "failed" -> envelopeId?.let { SigningResult.Failed(it, message) }
            else -> null
        }
    }
}
