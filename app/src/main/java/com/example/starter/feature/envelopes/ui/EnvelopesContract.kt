package com.example.starter.feature.envelopes.ui

import com.example.signing.Envelope
import com.example.signing.EnvelopeStatus
import com.example.signing.SigningResult
import com.example.signing.SigningSession
import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText

data class EnvelopesState(
    val envelopes: List<Envelope> = emptyList(),
    val isRefreshing: Boolean = false,
    val busyIds: Set<String> = emptySet(),
    /** Envelope whose "type your name" dialog is open. */
    val signingOfflineId: String? = null
) : MviState {
    val pendingSyncCount: Int get() = envelopes.count { it.status == EnvelopeStatus.SIGNED_PENDING_SYNC }
}

sealed interface EnvelopesIntent : UiIntent {
    data object Refresh : EnvelopesIntent
    data object SyncNow : EnvelopesIntent
    data class SignOnline(val envelopeId: String) : EnvelopesIntent
    data class SigningFinished(val result: SigningResult) : EnvelopesIntent
    data class SaveOffline(val envelopeId: String) : EnvelopesIntent
    data class StartOfflineSign(val envelopeId: String) : EnvelopesIntent
    data class ConfirmOfflineSign(val signerName: String) : EnvelopesIntent
    data object DismissDialog : EnvelopesIntent
}

sealed interface EnvelopesEffect : UiEffect {
    data class LaunchSigning(val session: SigningSession) : EnvelopesEffect
    data class ShowMessage(val message: UiText) : EnvelopesEffect
}
