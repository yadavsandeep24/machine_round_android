package com.example.starter.feature.pagedposts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.starter.R
import com.example.starter.core.model.Post
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView
import com.example.starter.core.ui.ListFooterEnd
import com.example.starter.core.ui.ListFooterError
import com.example.starter.core.ui.ListFooterLoading
import com.example.starter.core.ui.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagedPostListRoute(onBack: () -> Unit, viewModel: PagedPostListViewModel = hiltViewModel()) {
    val items = viewModel.posts.collectAsLazyPagingItems()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val refresh = items.loadState.refresh

    // Refresh failed but we still have cached rows: keep them, tell the user.
    LaunchedEffect(refresh) {
        if (refresh is LoadState.Error && items.itemCount > 0) {
            snackbar.showSnackbar(refresh.error.toUserMessage().asString(context))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.paged_posts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refresh is LoadState.Loading && items.itemCount > 0,
            onRefresh = items::refresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> CenteredLoader()
                refresh is LoadState.Error && items.itemCount == 0 ->
                    ErrorView(refresh.error.toUserMessage().asString(), onRetry = items::retry)
                refresh is LoadState.NotLoading && items.itemCount == 0 -> CenteredText(stringResource(R.string.empty_generic))
                else -> PagedList(items)
            }
        }
    }
}

@Composable
private fun PagedList(items: LazyPagingItems<Post>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.id },
            contentType = items.itemContentType { "post" }
        ) { index ->
            items[index]?.let { PagedPostItem(it) } // accessing items[index] is what triggers loading
        }
        item(key = "footer", contentType = "footer") {
            when (val append = items.loadState.append) {
                is LoadState.Loading -> ListFooterLoading()
                is LoadState.Error -> ListFooterError(append.error.toUserMessage().asString(), onRetry = items::retry)
                is LoadState.NotLoading -> if (append.endOfPaginationReached) ListFooterEnd()
            }
        }
    }
}

@Composable
private fun PagedPostItem(post: Post) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("#${post.id}  ${post.title}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(post.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
