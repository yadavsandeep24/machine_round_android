package com.example.starter.feature.envelopes.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signing.Envelope
import com.example.signing.EnvelopeStatus
import com.example.signing.SignEnvelope
import com.example.starter.R

@Composable
fun EnvelopesRoute(viewModel: EnvelopesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // The SDK's ActivityResultContract: one launch, exactly one SigningResult back.
    val signingLauncher = rememberLauncherForActivityResult(SignEnvelope()) { result ->
        viewModel.onIntent(EnvelopesIntent.SigningFinished(result))
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EnvelopesEffect.LaunchSigning -> signingLauncher.launch(effect.session)
                is EnvelopesEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
            }
        }
    }
    EnvelopesScreen(state, snackbar, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopesScreen(state: EnvelopesState, snackbar: SnackbarHostState, onIntent: (EnvelopesIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.envelopes_title)) },
                actions = {
                    TextButton(onClick = { onIntent(EnvelopesIntent.SyncNow) }) {
                        Text(stringResource(R.string.envelopes_sync, state.pendingSyncCount))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(EnvelopesIntent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.envelopes, key = { it.id }) { envelope ->
                    EnvelopeCard(envelope, isBusy = envelope.id in state.busyIds, onIntent = onIntent)
                }
            }
        }
    }
    state.signingOfflineId?.let { OfflineSignDialog(onIntent) }
}

@Composable
private fun EnvelopeCard(envelope: Envelope, isBusy: Boolean, onIntent: (EnvelopesIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(envelope.title, style = MaterialTheme.typography.titleMedium)
                    Text(envelope.sender, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = {}, label = { Text(stringResource(envelope.status.label())) })
            }
            Spacer(Modifier.height(8.dp))
            if (isBusy) {
                CircularProgressIndicator()
            } else if (envelope.status == EnvelopeStatus.SENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onIntent(EnvelopesIntent.SignOnline(envelope.id)) }) {
                        Text(stringResource(R.string.envelopes_sign_online))
                    }
                    if (envelope.availableOffline) {
                        OutlinedButton(onClick = { onIntent(EnvelopesIntent.StartOfflineSign(envelope.id)) }) {
                            Text(stringResource(R.string.envelopes_sign_offline))
                        }
                    } else {
                        OutlinedButton(onClick = { onIntent(EnvelopesIntent.SaveOffline(envelope.id)) }) {
                            Text(stringResource(R.string.envelopes_save_offline))
                        }
                    }
                }
            }
        }
    }
}

private fun EnvelopeStatus.label(): Int = when (this) {
    EnvelopeStatus.SENT -> R.string.envelopes_status_sent
    EnvelopeStatus.SIGNED_PENDING_SYNC -> R.string.envelopes_status_pending
    EnvelopeStatus.COMPLETED -> R.string.envelopes_status_completed
    EnvelopeStatus.VOIDED -> R.string.envelopes_status_voided
}

@Composable
private fun OfflineSignDialog(onIntent: (EnvelopesIntent) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onIntent(EnvelopesIntent.DismissDialog) },
        title = { Text(stringResource(R.string.envelopes_offline_dialog_title)) },
        text = {
            OutlinedTextField(value = name, onValueChange = {
                name = it
            }, label = { Text(stringResource(R.string.envelopes_full_name)) }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onIntent(EnvelopesIntent.ConfirmOfflineSign(name)) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.envelopes_adopt_and_sign))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(EnvelopesIntent.DismissDialog) }) { Text(stringResource(R.string.envelopes_cancel)) }
        }
    )
}
