package com.example.starter.feature.envelopes

import app.cash.turbine.test
import com.example.signing.Envelope
import com.example.signing.EnvelopeStatus
import com.example.signing.Recipient
import com.example.signing.SigningClient
import com.example.signing.SigningException
import com.example.signing.SigningResult
import com.example.signing.SigningSession
import com.example.signing.SyncReport
import com.example.starter.R
import com.example.starter.core.ui.UiText
import com.example.starter.feature.envelopes.ui.EnvelopesEffect
import com.example.starter.feature.envelopes.ui.EnvelopesIntent
import com.example.starter.feature.envelopes.ui.EnvelopesViewModel
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Host-app test: the SDK is faked through its public interface, which is why SigningClient is an interface. */
@OptIn(ExperimentalCoroutinesApi::class)
class EnvelopesViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private class FakeSigningClient : SigningClient {
        override val envelopes = MutableStateFlow(listOf(Envelope("e1", "NDA", "Legal", EnvelopeStatus.SENT, availableOffline = true)))
        var refreshError: SigningException? = null
        val handled = mutableListOf<SigningResult>()
        val offlineSigned = mutableListOf<Pair<String, String>>()
        override suspend fun refresh() {
            refreshError?.let { throw it }
        }
        override suspend fun makeAvailableOffline(envelopeId: String) = Unit
        override suspend fun signOffline(envelopeId: String, signerName: String) {
            offlineSigned += envelopeId to signerName
        }
        override suspend fun syncPending() = SyncReport(synced = 2, failed = 1, rejected = 0)
        override suspend fun createSigningSession(envelopeId: String, recipient: Recipient) =
            SigningSession(envelopeId, "https://x/$envelopeId")
        override suspend fun handleResult(result: SigningResult) {
            handled += result
        }
    }

    private val client = FakeSigningClient()

    @Test
    fun `sign online creates a session and asks the UI to launch the SDK contract`() = runTest(mainRule.dispatcher) {
        val vm = EnvelopesViewModel(client)
        advanceUntilIdle()
        vm.effects.test {
            vm.onIntent(EnvelopesIntent.SignOnline("e1"))
            assertThat(awaitItem()).isEqualTo(EnvelopesEffect.LaunchSigning(SigningSession("e1", "https://x/e1")))
        }
    }

    @Test
    fun `signing result is handed back to the SDK`() = runTest(mainRule.dispatcher) {
        val vm = EnvelopesViewModel(client)
        advanceUntilIdle()
        vm.effects.test {
            vm.onIntent(EnvelopesIntent.SigningFinished(SigningResult.Completed("e1")))
            assertThat(awaitItem()).isEqualTo(EnvelopesEffect.ShowMessage(UiText.Res(R.string.envelopes_signed)))
        }
        assertThat(client.handled).containsExactly(SigningResult.Completed("e1"))
    }

    @Test
    fun `offline sign dialog flow signs with the typed name`() = runTest(mainRule.dispatcher) {
        val vm = EnvelopesViewModel(client)
        advanceUntilIdle()
        vm.onIntent(EnvelopesIntent.StartOfflineSign("e1"))
        assertThat(vm.state.value.signingOfflineId).isEqualTo("e1")
        vm.onIntent(EnvelopesIntent.ConfirmOfflineSign("Asha Rao"))
        advanceUntilIdle()
        assertThat(vm.state.value.signingOfflineId).isNull()
        assertThat(client.offlineSigned).containsExactly("e1" to "Asha Rao")
    }

    @Test
    fun `SDK errors map to user messages exhaustively`() = runTest(mainRule.dispatcher) {
        client.refreshError = SigningException.Unauthorized()
        val vm = EnvelopesViewModel(client)
        vm.effects.test {
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(EnvelopesEffect.ShowMessage(UiText.Res(R.string.envelopes_error_auth)))
        }
        assertThat(vm.state.value.isRefreshing).isFalse()
    }
}
