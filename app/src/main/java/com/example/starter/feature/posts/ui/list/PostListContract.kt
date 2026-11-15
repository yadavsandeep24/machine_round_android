package com.example.starter.feature.posts.ui.list

import com.example.starter.core.model.Post
import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.paging.PageStatus
import com.example.starter.core.ui.UiText

/** MVI contract: everything the screen can show, do, and emit, in one place. */
data class PostListState(
    val posts: List<Post> = emptyList(),
    // first page / full-screen
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    // load more (bottom of list)
    val paging: PageStatus = PageStatus()
) : MviState {
    /** Don't page while the first page is loading or while there's nothing to append to. */
    val canLoadMore: Boolean get() = paging.canLoadMore && posts.isNotEmpty() && !isLoading && !isRefreshing
}

sealed interface PostListIntent : UiIntent {
    data object Load : PostListIntent
    data object Refresh : PostListIntent
    data object Retry : PostListIntent

    /** Fired by the list when the user scrolls near the bottom. */
    data object LoadNextPage : PostListIntent
    data object RetryLoadMore : PostListIntent
}

sealed interface PostListEffect : UiEffect {
    data class ShowSnackbar(val message: UiText) : PostListEffect
}
