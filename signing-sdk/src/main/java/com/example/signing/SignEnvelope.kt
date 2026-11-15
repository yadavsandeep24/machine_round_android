package com.example.signing

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.example.signing.internal.ResultCodec
import com.example.signing.internal.SigningActivity

/**
 * Launches the online signing ceremony and returns exactly one [SigningResult].
 *
 * ```
 * val launcher = rememberLauncherForActivityResult(SignEnvelope()) { result -> ... }
 * launcher.launch(client.createSigningSession(envelopeId, recipient))
 * ```
 */
public class SignEnvelope : ActivityResultContract<SigningSession, SigningResult>() {
    override fun createIntent(context: Context, input: SigningSession): Intent = Intent(context, SigningActivity::class.java)
        .putExtra(SigningActivity.EXTRA_URL, input.url)
        .putExtra(SigningActivity.EXTRA_ENVELOPE_ID, input.envelopeId)

    override fun parseResult(resultCode: Int, intent: Intent?): SigningResult =
        ResultCodec.fromIntent(intent) ?: SigningResult.Cancelled(null)
}
