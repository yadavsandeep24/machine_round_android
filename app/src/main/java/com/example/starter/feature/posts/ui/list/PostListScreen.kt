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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
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
import com.example.starter.core.ui.ListFooterEnd
import com.example.starter.core.ui.ListFooterError
import com.example.starter.core.ui.ListFooterLoading
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(PostListIntent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> CenteredLoader()
                state.error != null && state.posts.isEmpty() ->
                    ErrorView(state.error.asString()) { onIntent(PostListIntent.Retry) }
                state.posts.isEmpty() -> CenteredText(stringResource(R.string.empty_generic))
                else -> PostList(state, onIntent)
            }
        }
    }
}

@Composable
private fun PostList(state: PostListState, onIntent: (PostListIntent) -> Unit) {
    val listState = rememberLazyListState()

    // Trigger the next page when the last visible item is within PREFETCH_DISTANCE of the end.
    // snapshotFlow re-emits when totalItems changes, so a short page that still fits on screen
    // keeps loading until the screen is filled. The ViewModel/Paginator guard against duplicates.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            (total > 0 && lastVisible >= total - 1 - PREFETCH_DISTANCE) to total
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { onIntent(PostListIntent.LoadNextPage) }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.posts, key = { it.id }) { post -> PostItem(post) }
        item(key = "footer", contentType = "footer") {
            when {
                state.paging.isLoadingMore -> ListFooterLoading()
                state.paging.loadMoreError != null ->
                    ListFooterError(state.paging.loadMoreError.asString()) { onIntent(PostListIntent.RetryLoadMore) }
                state.paging.endReached -> ListFooterEnd()
            }
        }
    }
}

private const val PREFETCH_DISTANCE = 5

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
