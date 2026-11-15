package com.example.starter.feature.organizations.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.map
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.paging.Paginator
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.organizations.domain.OrganizationPage
import com.example.starter.feature.organizations.domain.OrganizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * MVI. One pipeline serves both browsing (blank query) and search:
 *   query (SavedStateHandle) -> debounce (instant when cleared) -> trim -> distinct
 *   -> flatMapLatest: page 0 for the new query, cancelling the old request
 * Pages 1..n come from the reusable [Paginator] (one request at a time, stop at the end, no auto-retry).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrganizationListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: OrganizationRepository
) : MviViewModel<OrganizationListIntent, OrganizationListState, OrganizationListEffect>(
    OrganizationListState(query = savedStateHandle[KEY_QUERY] ?: "")
) {
    private val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val retry = MutableStateFlow(0)
    private val pager = Paginator(
        scope = viewModelScope,
        loadPage = ::loadPage,
        onStatusChange = { status -> setState { copy(paging = status) } }
    )

    init {
        viewModelScope.launch { firstPages().collect(::reduce) }
    }

    override fun handleIntent(intent: OrganizationListIntent) {
        when (intent) {
            is OrganizationListIntent.QueryChanged -> {
                setState { copy(query = intent.query) }
                savedStateHandle[KEY_QUERY] = intent.query
            }
            OrganizationListIntent.LoadNextPage -> if (currentState.canLoadMore) pager.loadNext()
            OrganizationListIntent.RetryLoadMore -> pager.retry()
            OrganizationListIntent.Retry -> retry.value++
            is OrganizationListIntent.OrganizationClicked -> sendEffect(OrganizationListEffect.NavigateToDetail(intent.ein))
        }
    }

    private sealed interface FirstPage {
        data class Loading(val query: String) : FirstPage
        data class Loaded(val query: String, val result: AppResult<OrganizationPage>) : FirstPage
    }

    private fun firstPages(): Flow<FirstPage> = combine(
        queryFlow
            .debounce { q -> if (q.isBlank()) 0L else DEBOUNCE_MS }
            .map { it.trim() }
            .distinctUntilChanged(),
        retry
    ) { query, _ -> query }
        .flatMapLatest { query ->
            pager.reset() // a load-more for the previous query is stale
            flow {
                emit(FirstPage.Loading(query))
                emit(FirstPage.Loaded(query, repository.search(query, page = 0)))
            }
        }

    private fun reduce(update: FirstPage) {
        when (update) {
            is FirstPage.Loading -> setState { copy(isLoading = true, error = null) }
            is FirstPage.Loaded -> when (val r = update.result) {
                is AppResult.Success -> {
                    setState {
                        copy(loadedQuery = update.query, items = r.data.items, totalResults = r.data.totalResults, isLoading = false)
                    }
                    pager.reset(nextPage = 1, endReached = r.data.endReached)
                }
                is AppResult.Failure -> setState {
                    copy(loadedQuery = update.query, items = emptyList(), isLoading = false, error = r.throwable.toUserMessage())
                }
            }
        }
    }

    private suspend fun loadPage(page: Int): AppResult<Boolean> {
        val query = currentState.loadedQuery ?: return AppResult.Success(true)
        val result = repository.search(query, page)
        if (result is AppResult.Success) {
            setState { copy(items = (items + result.data.items).distinctBy { it.ein }) }
        }
        return result.map { it.endReached }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val DEBOUNCE_MS = 350L
    }
}
