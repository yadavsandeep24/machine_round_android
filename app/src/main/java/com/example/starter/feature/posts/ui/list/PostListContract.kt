package com.example.starter.feature.posts.ui.list

import com.example.starter.core.model.Post
import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.paging.PageStatus
import com.example.starter.core.ui.UiText

/** MVI contract: everything the screen can show, do, and emit, in one place. */
data class PostListState(
    // ---- browse mode (cached in Room) ----
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val browsePaging: PageStatus = PageStatus(),

    // ---- search mode (network only) ----
    /** What's in the text field. Updated on every keystroke. */
    val query: String = "",
    /** The query [searchResults] belong to. Lags behind [query] by the debounce. */
    val searchedQuery: String? = null,
    val searchResults: List<Post> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: UiText? = null,
    val searchPaging: PageStatus = PageStatus(),
    val favoriteIds: Set<Int> = emptySet()
) : MviState {
    val isSearchActive: Boolean get() = query.isNotBlank()

    /** Typed, but the debounce hasn't fired yet (or the request is in flight). */
    val isSearchPending: Boolean get() = isSearchActive && (isSearching || searchedQuery != query.trim())

    val visiblePosts: List<Post>
        get() = if (isSearchActive) searchResults.map { it.copy(isFavorite = it.id in favoriteIds) } else posts

    val paging: PageStatus get() = if (isSearchActive) searchPaging else browsePaging

    val canLoadMoreBrowse: Boolean get() = browsePaging.canLoadMore && posts.isNotEmpty() && !isLoading && !isRefreshing

    val canLoadMoreSearch: Boolean get() = searchPaging.canLoadMore && !isSearchPending && searchResults.isNotEmpty()
}

sealed interface PostListIntent : UiIntent {
    data object Load : PostListIntent
    data object Refresh : PostListIntent
    data object Retry : PostListIntent

    /** Fired by the list when the user scrolls near the bottom. Applies to browse or search. */
    data object LoadNextPage : PostListIntent
    data object RetryLoadMore : PostListIntent
    data class QueryChanged(val query: String) : PostListIntent
    data class ToggleFavorite(val post: Post) : PostListIntent
    data class PostClicked(val id: Int) : PostListIntent
    data object OpenPaging3Sample : PostListIntent
}

sealed interface PostListEffect : UiEffect {
    data class NavigateToDetail(val id: Int) : PostListEffect
    data object NavigateToPaging3Sample : PostListEffect
    data class ShowSnackbar(val message: UiText) : PostListEffect
}
