package com.example.starter.feature.envelopes.ui

import androidx.lifecycle.viewModelScope
import com.example.signing.Recipient
import com.example.signing.SigningClient
import com.example.signing.SigningException
import com.example.signing.SigningResult
import com.example.starter.R
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/** Host-app side: depends only on the SDK's public [SigningClient] interface (fakeable in tests). */
@HiltViewModel
class EnvelopesViewModel @Inject constructor(private val client: SigningClient) :
    MviViewModel<EnvelopesIntent, EnvelopesState, EnvelopesEffect>(EnvelopesState()) {

    init {
        viewModelScope.launch { client.envelopes.collect { list -> setState { copy(envelopes = list) } } }
        onIntent(EnvelopesIntent.Refresh)
    }

    override fun handleIntent(intent: EnvelopesIntent) {
        when (intent) {
            EnvelopesIntent.Refresh -> launchGuarded {
                setState { copy(isRefreshing = true) }
                try {
                    client.refresh()
                } finally {
                    setState { copy(isRefreshing = false) }
                }
            }
            EnvelopesIntent.SyncNow -> launchGuarded {
                val report = client.syncPending()
                message(R.string.envelopes_sync_report, report.synced, report.failed, report.rejected)
            }
            is EnvelopesIntent.SignOnline -> busy(intent.envelopeId) {
                sendEffect(EnvelopesEffect.LaunchSigning(client.createSigningSession(intent.envelopeId, DEMO_RECIPIENT)))
            }
            is EnvelopesIntent.SigningFinished -> launchGuarded {
                client.handleResult(intent.result)
                when (val r = intent.result) {
                    is SigningResult.Completed -> message(R.string.envelopes_signed)
                    is SigningResult.Declined -> message(R.string.envelopes_declined, r.reason)
                    is SigningResult.Cancelled -> Unit
                    is SigningResult.Failed -> sendEffect(EnvelopesEffect.ShowMessage(UiText.Dynamic(r.message)))
                }
            }
            is EnvelopesIntent.SaveOffline -> busy(intent.envelopeId) {
                client.makeAvailableOffline(intent.envelopeId)
                message(R.string.envelopes_saved_offline)
            }
            is EnvelopesIntent.StartOfflineSign -> setState { copy(signingOfflineId = intent.envelopeId) }
            is EnvelopesIntent.ConfirmOfflineSign -> {
                val id = currentState.signingOfflineId ?: return
                setState { copy(signingOfflineId = null) }
                busy(id) {
                    client.signOffline(id, intent.signerName)
                    message(R.string.envelopes_signed_offline)
                }
            }
            EnvelopesIntent.DismissDialog -> setState { copy(signingOfflineId = null) }
        }
    }

    private fun busy(id: String, block: suspend () -> Unit) = launchGuarded {
        setState { copy(busyIds = busyIds + id) }
        try {
            block()
        } finally {
            setState { copy(busyIds = busyIds - id) }
        }
    }

    /** The SDK only throws SigningException (or IllegalArgumentException), so mapping is exhaustive. */
    private fun launchGuarded(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: SigningException) {
                val res = when (e) {
                    is SigningException.Network -> R.string.envelopes_error_network
                    is SigningException.Unauthorized -> R.string.envelopes_error_auth
                    is SigningException.EnvelopeNotFound -> R.string.envelopes_error_not_found
                    is SigningException.NotAvailableOffline -> R.string.envelopes_error_not_offline
                }
                message(res)
            } catch (e: IllegalArgumentException) {
                Timber.d(e, "Rejected input")
                message(R.string.envelopes_error_name)
            }
        }
    }

    private fun message(res: Int, vararg args: Any) = sendEffect(EnvelopesEffect.ShowMessage(UiText.Res(res, args.toList())))

    private companion object {
        val DEMO_RECIPIENT = Recipient("Demo Signer", "signer@example.com")
    }
}
