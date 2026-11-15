package com.example.starter.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView
import com.example.starter.core.ui.ListFooterEnd
import com.example.starter.core.ui.ListFooterError
import com.example.starter.core.ui.ListFooterLoading
import com.example.starter.feature.search.domain.Asset
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun SearchRoute(viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SearchEffect.HideKeyboard -> keyboard?.hide()
            }
        }
    }
    SearchScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.search_title)) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(state.query, onIntent)
            Box(Modifier.fillMaxSize()) {
                Results(state, onIntent)
                // Suggestions overlay the results, like a dropdown.
                if (state.showSuggestions) SuggestionsPanel(state, onIntent)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onIntent: (SearchIntent) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onIntent(SearchIntent.QueryChanged("")) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onIntent(SearchIntent.Submit) }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SuggestionsPanel(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    // Blank field -> recent searches; typing -> live suggestions.
    val (rows, isRecent) = if (state.query.isBlank()) state.recentSearches to true else state.suggestions to false
    if (rows.isEmpty()) return
    Surface(tonalElevation = 6.dp, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            rows.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().clickable { onIntent(SearchIntent.SuggestionClicked(text)) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isRecent) Icons.Default.History else Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(text)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Results(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    when {
        state.submittedQuery == null -> CenteredText(stringResource(R.string.search_start))
        state.isLoading -> CenteredLoader()
        state.error != null -> ErrorView(state.error.asString()) { onIntent(SearchIntent.Retry) }
        state.results.isEmpty() -> CenteredText(stringResource(R.string.search_no_results, state.submittedQuery))
        else -> ResultList(state, onIntent)
    }
}

@Composable
private fun ResultList(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.submittedQuery) { listState.scrollToItem(0) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1 - PREFETCH_DISTANCE) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { onIntent(SearchIntent.LoadNextPage) }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.results, key = { it.id }) { asset -> AssetRow(asset) }
        item(key = "footer") {
            when {
                state.paging.isLoadingMore -> ListFooterLoading()
                state.paging.loadMoreError != null ->
                    ListFooterError(state.paging.loadMoreError.asString()) { onIntent(SearchIntent.RetryLoadMore) }
                state.paging.endReached -> ListFooterEnd()
            }
        }
    }
}

@Composable
private fun AssetRow(asset: Asset) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("#${asset.id}  ${asset.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("${asset.author} · ${asset.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val PREFETCH_DISTANCE = 5
