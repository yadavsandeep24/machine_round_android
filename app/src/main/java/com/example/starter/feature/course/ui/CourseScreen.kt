package com.example.starter.feature.course.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView
import com.example.starter.feature.course.domain.DownloadStatus

@Composable
fun CourseRoute(viewModel: CourseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CourseEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
            }
        }
    }
    CourseScreen(state, snackbar, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(state: CourseState, snackbar: SnackbarHostState, onIntent: (CourseIntent) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(state.courseTitle.ifEmpty { stringResource(R.string.course_title) }) }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!state.isOnline) OfflineBanner()
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(CourseIntent.Retry) }
                else -> {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = state.showingDownloadedOnly,
                            enabled = state.isOnline,
                            onClick = { onIntent(CourseIntent.ToggleDownloadedOnly) },
                            label = { Text(stringResource(R.string.course_downloaded_only)) }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.course_storage, state.downloadedCount, state.downloadedBytes / BYTES_PER_MB),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (state.rows.isEmpty()) {
                        CenteredText(stringResource(R.string.course_nothing_downloaded))
                    } else {
                        LessonList(state.rows, onIntent)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.course_offline), Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun LessonList(rows: List<LessonRow>, onIntent: (CourseIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(rows, key = { it.lesson.id }) { row -> LessonCard(row, onIntent) }
    }
}

@Composable
private fun LessonCard(row: LessonRow, onIntent: (CourseIntent) -> Unit) {
    val id = row.lesson.id
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.lesson.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.course_lesson_meta, row.lesson.durationMinutes, row.lesson.sizeBytes / BYTES_PER_MB),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                when (val status = row.status) {
                    DownloadStatus.NotDownloaded -> IconButton(onClick = { onIntent(CourseIntent.Download(id)) }) {
                        Icon(Icons.Default.Download, stringResource(R.string.course_download))
                    }
                    DownloadStatus.Queued, is DownloadStatus.Downloading -> IconButton(onClick = { onIntent(CourseIntent.Cancel(id)) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.course_cancel))
                    }
                    is DownloadStatus.Downloaded -> Row {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
                        IconButton(onClick = {
                            onIntent(CourseIntent.Delete(id))
                        }) { Icon(Icons.Default.Delete, stringResource(R.string.course_delete)) }
                    }
                    is DownloadStatus.Failed -> IconButton(onClick = { onIntent(CourseIntent.Download(id)) }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.course_retry_download, status.reason))
                    }
                }
            }
            when (val status = row.status) {
                DownloadStatus.Queued -> Text(stringResource(R.string.course_queued), style = MaterialTheme.typography.labelSmall)
                is DownloadStatus.Downloading -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { status.percent / PERCENT }, modifier = Modifier.fillMaxWidth())
                }
                is DownloadStatus.Failed -> Text(
                    status.reason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
                else -> Unit
            }
        }
    }
}

private const val BYTES_PER_MB = 1_048_576L
private const val PERCENT = 100f
