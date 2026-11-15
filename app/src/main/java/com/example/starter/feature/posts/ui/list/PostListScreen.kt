package com.example.starter.feature.posts.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.model.Post
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView

/** Stateful route: wires the ViewModel, collects one-off effects. */
@Composable
fun PostListRoute(viewModel: PostListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PostListEffect.ShowSnackbar -> snackbar.showSnackbar(effect.message.asString(context))
            }
        }
    }

    PostListScreen(state = state, snackbar = snackbar, onIntent = viewModel::onIntent)
}

/** Stateless screen: easy to preview and to UI-test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen(state: PostListState, snackbar: SnackbarHostState, onIntent: (PostListIntent) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.posts_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        // Separate scroll positions: clearing the search returns you to where you were browsing.
        val browseListState = rememberLazyListState()
        val searchListState = rememberLazyListState()
        LaunchedEffect(state.searchedQuery) { searchListState.scrollToItem(0) } // new query -> top

        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(query = state.query, onQueryChange = { onIntent(PostListIntent.QueryChanged(it)) })
            if (state.isSearchActive) {
                SearchContent(state, searchListState, onIntent)
            } else {
                BrowseContent(state, browseListState, onIntent)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.posts_search_clear))
                }
            }
        },
        placeholder = { Text(stringResource(R.string.posts_search_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseContent(state: PostListState, listState: LazyListState, onIntent: (PostListIntent) -> Unit) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(PostListIntent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.posts.isEmpty() -> CenteredLoader()
            state.error != null && state.posts.isEmpty() ->
                ErrorView(state.error.asString()) { onIntent(PostListIntent.Retry) }
            state.posts.isEmpty() -> CenteredText(stringResource(R.string.empty_generic))
            else -> PostList(state.posts, listState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(state: PostListState, listState: LazyListState, onIntent: (PostListIntent) -> Unit) {
    val results = state.visiblePosts
    PullToRefreshBox(
        // While a new query loads we keep the old results visible and show the indicator on top.
        isRefreshing = state.isSearchPending && results.isNotEmpty(),
        onRefresh = { onIntent(PostListIntent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isSearchPending && results.isEmpty() -> CenteredLoader()
            state.searchError != null -> ErrorView(state.searchError.asString()) { onIntent(PostListIntent.Retry) }
            results.isEmpty() -> CenteredText(stringResource(R.string.posts_search_no_results, state.searchedQuery.orEmpty()))
            else -> PostList(results, listState)
        }
    }
}

@Composable
private fun PostList(posts: List<Post>, listState: LazyListState) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post -> PostItem(post) }
    }
}

@Composable
private fun PostItem(post: Post) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(post.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostListPreview() {
    PostListScreen(
        state = PostListState(posts = List(3) { Post(it, 1, "Title $it", "Body text $it") }),
        snackbar = SnackbarHostState(),
        onIntent = {}
    )
}
