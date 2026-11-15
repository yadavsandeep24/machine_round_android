package com.example.starter.feature.comments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.ErrorView
import com.example.starter.feature.comments.domain.CommentRow
import com.example.starter.feature.comments.domain.PetLabel
import com.example.starter.feature.comments.domain.Preference

@Composable
fun CommentThreadRoute(viewModel: CommentThreadViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CommentThreadScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentThreadScreen(state: CommentThreadState, onIntent: (CommentThreadIntent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.comments_title)) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Controls(state, onIntent)
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(CommentThreadIntent.Retry) }
                else -> Thread(state, onIntent)
            }
        }
    }
}

@Composable
private fun Controls(state: CommentThreadState, onIntent: (CommentThreadIntent) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Preference.DOG_PERSON to R.string.comments_pref_dog,
            Preference.CAT_PERSON to R.string.comments_pref_cat,
            Preference.SHOW_ALL to R.string.comments_pref_all
        ).forEach { (pref, label) ->
            FilterChip(
                selected = state.preference == pref,
                onClick = { onIntent(CommentThreadIntent.SetPreference(pref)) },
                label = { Text(stringResource(label)) }
            )
        }
    }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        THRESHOLDS.forEach { limit ->
            FilterChip(
                selected = state.scoreThreshold == limit,
                onClick = { onIntent(CommentThreadIntent.SetScoreThreshold(limit)) },
                label = {
                    Text(
                        if (limit ==
                            null
                        ) {
                            stringResource(R.string.comments_rescue_off)
                        } else {
                            stringResource(R.string.comments_rescue_above, limit)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun Thread(state: CommentThreadState, onIntent: (CommentThreadIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.hiddenCount > 0) {
            item(key = "hidden") {
                Text(
                    stringResource(R.string.comments_hidden_count, state.hiddenCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.rows, key = { it.comment.id }) { row ->
            CommentItem(row, onToggle = { onIntent(CommentThreadIntent.ToggleCollapse(row.comment.id)) })
        }
    }
}

@Composable
private fun CommentItem(row: CommentRow, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = (row.depth * INDENT_DP).dp).heightIn(min = 48.dp)) {
        if (row.depth > 0) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }
        Column(Modifier.padding(start = 8.dp).clickable(onClick = onToggle)) {
            val badge = if (row.comment.label == PetLabel.DOG) "🐶" else "🐱"
            Text("$badge ${row.comment.author} · ${row.comment.score} pts", style = MaterialTheme.typography.labelMedium)
            if (!row.isCollapsed) Text(row.comment.text, style = MaterialTheme.typography.bodyMedium)
            if (row.replyCount > 0) {
                val label = if (row.isCollapsed) R.string.comments_expand else R.string.comments_collapse
                Text(
                    stringResource(label, row.replyCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(Modifier.height(4.dp))
        }
    }
}

private const val INDENT_DP = 12
private val THRESHOLDS = listOf(null, 10, 50)
