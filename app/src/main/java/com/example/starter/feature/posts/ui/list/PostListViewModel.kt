package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.feature.posts.domain.usecase.RefreshPostsUseCase
import com.example.starter.feature.posts.domain.usecase.SearchPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * MVI with debounced server-side search:
 *   keystrokes -> debounce (instant when cleared) -> trim -> distinctUntilChanged
 *   -> flatMapLatest: a new query CANCELS the request still running for the old one,
 *      so a slow response for "ko" can never overwrite results for "kotlin".
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observePosts: ObservePostsUseCase,
    private val refreshPosts: RefreshPostsUseCase,
    private val searchPosts: SearchPostsUseCase
) : MviViewModel<PostListIntent, PostListState, PostListEffect>(
    PostListState(isLoading = true, query = savedStateHandle[KEY_QUERY] ?: "")
) {

    private var refreshJob: Job? = null

    /** Survives process death: the user comes back to the same search. */
    private val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val searchRetry = MutableStateFlow(0) // bump to re-run the same query (distinctUntilChanged would block it)

    init {
        viewModelScope.launch {
            observePosts().collect { posts -> setState { copy(posts = posts) } }
        }
        viewModelScope.launch {
            searchUpdates().collect(::reduceSearch)
        }
        onIntent(PostListIntent.Load)
    }

    override fun handleIntent(intent: PostListIntent) {
        val searching = currentState.isSearchActive
        when (intent) {
            PostListIntent.Load -> refresh(isPullToRefresh = false)
            PostListIntent.Retry -> if (searching) searchRetry.value++ else refresh(isPullToRefresh = false)
            PostListIntent.Refresh -> if (searching) searchRetry.value++ else refresh(isPullToRefresh = true)
            is PostListIntent.QueryChanged -> {
                setState { copy(query = intent.query) } // text field updates immediately
                savedStateHandle[KEY_QUERY] = intent.query // the search itself is debounced
            }
        }
    }

    // ---------------- search ----------------

    private sealed interface SearchUpdate {
        data object Cleared : SearchUpdate
        data class Loading(val query: String) : SearchUpdate
        data class Loaded(val query: String, val result: AppResult<List<Post>>) : SearchUpdate
    }

    private fun searchUpdates(): Flow<SearchUpdate> = combine(
        queryFlow
            .debounce { q -> if (q.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
            .map { it.trim() }
            .distinctUntilChanged(),
        searchRetry
    ) { query, _ -> query }
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf<SearchUpdate>(SearchUpdate.Cleared)
            } else {
                flow<SearchUpdate> {
                    emit(SearchUpdate.Loading(query))
                    emit(SearchUpdate.Loaded(query, searchPosts(query)))
                }
            }
        }

    private fun reduceSearch(update: SearchUpdate) {
        when (update) {
            SearchUpdate.Cleared -> setState {
                copy(searchedQuery = null, searchResults = emptyList(), isSearching = false, searchError = null)
            }
            // keep old results on screen while the new ones load
            is SearchUpdate.Loading -> setState { copy(isSearching = true, searchError = null) }
            is SearchUpdate.Loaded -> when (val r = update.result) {
                is AppResult.Success -> setState {
                    copy(searchedQuery = update.query, searchResults = r.data, isSearching = false)
                }
                is AppResult.Failure -> setState {
                    copy(
                        searchedQuery = update.query,
                        searchResults = emptyList(),
                        isSearching = false,
                        searchError = r.throwable.toUserMessage()
                    )
                }
            }
        }
    }

    // ---------------- browse ----------------

    private fun refresh(isPullToRefresh: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            setState { copy(isLoading = !isPullToRefresh, isRefreshing = isPullToRefresh, error = null) }
            when (val result = refreshPosts()) {
                is AppResult.Success -> setState { copy(isLoading = false, isRefreshing = false) }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (currentState.posts.isNotEmpty()) {
                        setState { copy(isLoading = false, isRefreshing = false) }
                        sendEffect(PostListEffect.ShowSnackbar(msg))
                    } else {
                        setState { copy(isLoading = false, isRefreshing = false, error = msg) }
                    }
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val KEY_QUERY = "query"
    }
}
