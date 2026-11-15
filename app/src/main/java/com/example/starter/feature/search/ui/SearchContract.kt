package com.example.starter.feature.search.ui

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.paging.PageStatus
import com.example.starter.core.ui.UiText
import com.example.starter.feature.search.domain.Asset

data class SearchState(
    /** Text field content, updated on every keystroke. */
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    /** Suggestions show while typing and hide once a query is submitted. */
    val showSuggestions: Boolean = false,
    /** The query the results belong to (null = nothing searched yet). */
    val submittedQuery: String? = null,
    val results: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val paging: PageStatus = PageStatus()
) : MviState {
    val canLoadMore: Boolean get() = paging.canLoadMore && results.isNotEmpty() && !isLoading
}

sealed interface SearchIntent : UiIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data class SuggestionClicked(val suggestion: String) : SearchIntent
    data object Submit : SearchIntent
    data object DismissSuggestions : SearchIntent
    data object Retry : SearchIntent
    data object LoadNextPage : SearchIntent
    data object RetryLoadMore : SearchIntent
}

sealed interface SearchEffect : UiEffect {
    data object HideKeyboard : SearchEffect
}
