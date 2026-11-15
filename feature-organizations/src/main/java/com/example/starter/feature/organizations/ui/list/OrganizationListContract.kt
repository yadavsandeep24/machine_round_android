package com.example.starter.feature.organizations.ui.list

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.paging.PageStatus
import com.example.starter.core.ui.UiText
import com.example.starter.feature.organizations.domain.Organization

data class OrganizationListState(
    /** Text field content, updated on every keystroke (never debounced). */
    val query: String = "",
    /** The query [items] belong to; lags [query] by the debounce. */
    val loadedQuery: String? = null,
    val items: List<Organization> = emptyList(),
    val totalResults: Int = 0,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val paging: PageStatus = PageStatus()
) : MviState {
    val isQueryPending: Boolean get() = loadedQuery != query.trim()
    val canLoadMore: Boolean get() = paging.canLoadMore && items.isNotEmpty() && !isLoading && !isQueryPending
}

sealed interface OrganizationListIntent : UiIntent {
    data class QueryChanged(val query: String) : OrganizationListIntent
    data object LoadNextPage : OrganizationListIntent
    data object RetryLoadMore : OrganizationListIntent
    data object Retry : OrganizationListIntent
    data class OrganizationClicked(val ein: Long) : OrganizationListIntent
}

sealed interface OrganizationListEffect : UiEffect {
    data class NavigateToDetail(val ein: Long) : OrganizationListEffect
}
