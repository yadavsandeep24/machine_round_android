package com.example.starter.feature.attendance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.ErrorView
import com.example.starter.feature.attendance.domain.SyncState

@Composable
fun AttendanceRoute(viewModel: AttendanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AttendanceEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
            }
        }
    }
    AttendanceScreen(state, snackbar, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(state: AttendanceState, snackbar: SnackbarHostState, onIntent: (AttendanceIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.attendance_title, state.date)) },
                actions = {
                    TextButton(onClick = { onIntent(AttendanceIntent.SyncNow) }) {
                        Text(stringResource(R.string.attendance_sync_now, state.pendingCount))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!state.isOnline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.attendance_offline), Modifier.padding(12.dp))
                }
            }
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(AttendanceIntent.Retry) }
                else -> {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.attendance_summary, state.presentCount, state.absentCount, state.unmarkedCount),
                            Modifier.weight(1f)
                        )
                        if (state.unmarkedCount > 0) {
                            TextButton(onClick = {
                                onIntent(AttendanceIntent.MarkAllPresent)
                            }) { Text(stringResource(R.string.attendance_mark_all)) }
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.rows, key = { it.student.id }) { row -> StudentCard(row, onIntent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCard(row: StudentRow, onIntent: (AttendanceIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${row.student.rollNo}. ${row.student.name}", style = MaterialTheme.typography.titleSmall)
                row.syncState?.let { SyncChip(it, row.record?.overriddenByServer == true) }
            }
            FilterChip(
                selected = row.present == true,
                onClick = { onIntent(AttendanceIntent.SetPresent(row.student.id, true)) },
                label = { Text(stringResource(R.string.attendance_present)) }
            )
            FilterChip(
                selected = row.present == false,
                onClick = { onIntent(AttendanceIntent.SetPresent(row.student.id, false)) },
                label = { Text(stringResource(R.string.attendance_absent)) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SyncChip(state: SyncState, overridden: Boolean) {
    val label = when {
        overridden -> R.string.attendance_sync_overridden
        state == SyncState.SYNCED -> R.string.attendance_sync_synced
        state == SyncState.FAILED -> R.string.attendance_sync_failed
        else -> R.string.attendance_sync_pending
    }
    AssistChip(onClick = {}, label = { Text(stringResource(label), style = MaterialTheme.typography.labelSmall) })
}
