package com.example.starter.feature.search.ui

import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.map
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.paging.Paginator
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.search.domain.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Two independent pipelines:
 *  1. Suggestions: keystrokes -> debounce -> distinct -> flatMapLatest (stale requests cancelled), cached in the repo.
 *  2. Results: submitted query -> page 1, then infinite scroll through [Paginator].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: SearchRepository) :
    MviViewModel<SearchIntent, SearchState, SearchEffect>(SearchState()) {

    private val typedQuery = MutableStateFlow("")
    private var firstPageJob: Job? = null
    private val pager = Paginator(
        scope = viewModelScope,
        loadPage = ::loadResultsPage,
        onStatusChange = { status -> setState { copy(paging = status) } }
    )

    init {
        viewModelScope.launch {
            repository.recentSearches.collect { recent -> setState { copy(recentSearches = recent) } }
        }
        viewModelScope.launch {
            typedQuery
                .debounce(SUGGEST_DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .flatMapLatest { prefix ->
                    flow {
                        if (prefix.length < MIN_PREFIX) {
                            emit(emptyList())
                        } else {
                            // Suggestions are best-effort: on failure just show none.
                            emit((repository.suggestions(prefix) as? AppResult.Success)?.data.orEmpty())
                        }
                    }
                }
                .collect { suggestions -> setState { copy(suggestions = suggestions) } }
        }
    }

    override fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                setState { copy(query = intent.query, showSuggestions = true) }
                typedQuery.value = intent.query
            }
            is SearchIntent.SuggestionClicked -> submit(intent.suggestion)
            SearchIntent.Submit -> submit(currentState.query)
            SearchIntent.DismissSuggestions -> setState { copy(showSuggestions = false) }
            SearchIntent.Retry -> currentState.submittedQuery?.let(::submit)
            SearchIntent.LoadNextPage -> if (currentState.canLoadMore) pager.loadNext()
            SearchIntent.RetryLoadMore -> pager.retry()
        }
    }

    private fun submit(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return
        repository.addRecentSearch(query)
        sendEffect(SearchEffect.HideKeyboard)
        firstPageJob?.cancel()
        pager.reset() // results of the previous query are stale
        setState {
            copy(query = query, showSuggestions = false, submittedQuery = query, results = emptyList(), isLoading = true, error = null)
        }
        firstPageJob = viewModelScope.launch {
            when (val result = repository.search(query, page = 1, pageSize = PAGE_SIZE)) {
                is AppResult.Success -> {
                    setState { copy(results = result.data.items, isLoading = false) }
                    pager.reset(nextPage = 2, endReached = result.data.endReached)
                }
                is AppResult.Failure -> setState { copy(isLoading = false, error = result.throwable.toUserMessage()) }
            }
        }
    }

    private suspend fun loadResultsPage(page: Int): AppResult<Boolean> {
        val query = currentState.submittedQuery ?: return AppResult.Success(true)
        val result = repository.search(query, page, PAGE_SIZE)
        if (result is AppResult.Success) {
            setState { copy(results = (results + result.data.items).distinctBy { it.id }) }
        }
        return result.map { it.endReached }
    }

    companion object {
        const val PAGE_SIZE = 20
        const val SUGGEST_DEBOUNCE_MS = 200L
        const val MIN_PREFIX = 2
    }
}
