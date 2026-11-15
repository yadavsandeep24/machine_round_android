package com.example.starter.feature.posts.ui.list

import com.example.starter.core.model.Post
import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText

/** MVI contract: everything the screen can show, do, and emit, in one place. */
data class PostListState(
    // ---- browse mode (cached in Room) ----
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,

    // ---- search mode (network only) ----
    /** What's in the text field. Updated on every keystroke. */
    val query: String = "",
    /** The query [searchResults] belong to. Lags behind [query] by the debounce. */
    val searchedQuery: String? = null,
    val searchResults: List<Post> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: UiText? = null
) : MviState {
    val isSearchActive: Boolean get() = query.isNotBlank()

    /** Typed, but the debounce hasn't fired yet (or the request is in flight). */
    val isSearchPending: Boolean get() = isSearchActive && (isSearching || searchedQuery != query.trim())

    val visiblePosts: List<Post> get() = if (isSearchActive) searchResults else posts
}

sealed interface PostListIntent : UiIntent {
    data object Load : PostListIntent
    data object Refresh : PostListIntent
    data object Retry : PostListIntent
    data class QueryChanged(val query: String) : PostListIntent
}

sealed interface PostListEffect : UiEffect {
    data class ShowSnackbar(val message: UiText) : PostListEffect
}
