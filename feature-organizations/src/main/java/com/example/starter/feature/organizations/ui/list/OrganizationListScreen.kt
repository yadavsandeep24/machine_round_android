package com.example.starter.feature.organizations.ui.list

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView
import com.example.starter.core.ui.ListFooterEnd
import com.example.starter.core.ui.ListFooterError
import com.example.starter.core.ui.ListFooterLoading
import com.example.starter.feature.organizations.R
import com.example.starter.feature.organizations.domain.Organization
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun OrganizationListRoute(onOpenDetail: (Long) -> Unit, viewModel: OrganizationListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OrganizationListEffect.NavigateToDetail -> onOpenDetail(effect.ein)
            }
        }
    }
    OrganizationListScreen(state, viewModel::onIntent)
}

/** Stateless: driven entirely by [state], so it's previewable and UI-testable without a ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationListScreen(state: OrganizationListState, onIntent: (OrganizationListIntent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.orgs_title)) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(state.query, onIntent)
            // Thin progress bar while a new query loads over existing results.
            if (state.isLoading && state.items.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                state.isLoading && state.items.isEmpty() -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(OrganizationListIntent.Retry) }
                state.items.isEmpty() && !state.isQueryPending ->
                    CenteredText(stringResource(R.string.orgs_no_results, state.loadedQuery.orEmpty()))
                else -> OrganizationList(state, onIntent)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onIntent: (OrganizationListIntent) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = { onIntent(OrganizationListIntent.QueryChanged(it)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onIntent(OrganizationListIntent.QueryChanged("")) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.orgs_clear_search))
                }
            }
        },
        placeholder = { Text(stringResource(R.string.orgs_search_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(TAG_SEARCH)
    )
}

@Composable
private fun OrganizationList(state: OrganizationListState, onIntent: (OrganizationListIntent) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.loadedQuery) { listState.scrollToItem(0) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1 - PREFETCH_DISTANCE) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { onIntent(OrganizationListIntent.LoadNextPage) }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().testTag(TAG_LIST)
    ) {
        item(key = "header") {
            Text(
                stringResource(R.string.orgs_result_count, state.totalResults),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(state.items, key = { it.ein }) { org ->
            OrganizationRow(org) { onIntent(OrganizationListIntent.OrganizationClicked(org.ein)) }
        }
        item(key = "footer") {
            // Local copy: Kotlin can't smart-cast a property declared in another module (:core).
            val loadMoreError = state.paging.loadMoreError
            when {
                state.paging.isLoadingMore -> ListFooterLoading()
                loadMoreError != null -> ListFooterError(loadMoreError.asString()) { onIntent(OrganizationListIntent.RetryLoadMore) }
                state.paging.endReached -> ListFooterEnd()
            }
        }
    }
}

@Composable
private fun OrganizationRow(org: Organization, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(org.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(org.location, org.nteeCode?.let { "NTEE $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

const val TAG_SEARCH = "org_search"
const val TAG_LIST = "org_list"
private const val PREFETCH_DISTANCE = 5

@Preview(showBackground = true)
@Composable
private fun OrganizationListPreview() {
    OrganizationListScreen(
        state = OrganizationListState(
            loadedQuery = "",
            isLoading = false,
            totalResults = 10_000,
            items = listOf(
                Organization(941340523, "Kaiser Foundation Health Plan Inc", "Oakland, CA", "E31"),
                Organization(911663695, "Gates Foundation Trust", "Kirkland, WA", "T20Z")
            )
        ),
        onIntent = {}
    )
}
