package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.map
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.paging.Paginator
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.posts.domain.PostPage
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase.Companion.PAGE_SIZE
import com.example.starter.feature.posts.domain.usecase.ObserveFavoriteIdsUseCase
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.feature.posts.domain.usecase.SearchPostsUseCase
import com.example.starter.feature.posts.domain.usecase.ToggleFavoriteUseCase
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
 * MVI example. This ViewModel only COORDINATES the screen:
 *  - browse mode: first page / refresh over the Room cache
 *  - search mode: debounced server-side search; flatMapLatest cancels stale requests
 * "Load more" mechanics for both modes are delegated to [Paginator].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observePosts: ObservePostsUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val loadPostsPage: LoadPostsPageUseCase,
    private val searchPosts: SearchPostsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : MviViewModel<PostListIntent, PostListState, PostListEffect>(
    PostListState(isLoading = true, query = savedStateHandle[KEY_QUERY] ?: "")
) {

    private val browsePager = Paginator(
        scope = viewModelScope,
        loadPage = { page -> loadPostsPage(page).map { it.endReached } }, // rows land in Room
        onStatusChange = { status -> setState { copy(browsePaging = status) } }
    )
    private val searchPager = Paginator(
        scope = viewModelScope,
        loadPage = ::loadSearchPage,
        onStatusChange = { status -> setState { copy(searchPaging = status) } }
    )
    private var firstPageJob: Job? = null

    /** Survives process death: the user comes back to the same search. */
    private val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val searchRetry = MutableStateFlow(0) // bump to re-run the same query (distinctUntilChanged would block it)

    init {
        viewModelScope.launch {
            observePosts().collect { posts -> setState { copy(posts = posts) } }
        }
        viewModelScope.launch {
            observeFavoriteIds().collect { ids -> setState { copy(favoriteIds = ids) } }
        }
        viewModelScope.launch {
            searchUpdates().collect(::reduceSearch)
        }
        onIntent(PostListIntent.Load)
    }

    override fun handleIntent(intent: PostListIntent) {
        val searching = currentState.isSearchActive
        when (intent) {
            PostListIntent.Load -> loadFirstPage(isPullToRefresh = false)
            PostListIntent.Retry -> if (searching) searchRetry.value++ else loadFirstPage(isPullToRefresh = false)
            PostListIntent.Refresh -> if (searching) searchRetry.value++ else loadFirstPage(isPullToRefresh = true)
            PostListIntent.LoadNextPage -> when {
                searching -> if (currentState.canLoadMoreSearch) searchPager.loadNext()
                else -> if (currentState.canLoadMoreBrowse) browsePager.loadNext()
            }
            PostListIntent.RetryLoadMore -> if (searching) searchPager.retry() else browsePager.retry()
            is PostListIntent.QueryChanged -> {
                setState { copy(query = intent.query) } // text field updates immediately
                savedStateHandle[KEY_QUERY] = intent.query // the search itself is debounced
            }
            is PostListIntent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(intent.post) }
            is PostListIntent.PostClicked -> sendEffect(PostListEffect.NavigateToDetail(intent.id))
            PostListIntent.OpenPaging3Sample -> sendEffect(PostListEffect.NavigateToPaging3Sample)
        }
    }

    // =====================================================================
    // Search
    // =====================================================================

    private sealed interface SearchUpdate {
        data object Cleared : SearchUpdate
        data class Loading(val query: String) : SearchUpdate
        data class Loaded(val query: String, val result: AppResult<PostPage>) : SearchUpdate
    }

    /**
     * keystrokes -> debounce (instant when cleared) -> trim -> distinct
     *   -> flatMapLatest: a new query CANCELS the request still running for the old one,
     *      so slow responses for "ko" can never overwrite results for "kotlin".
     */
    private fun searchUpdates(): Flow<SearchUpdate> = combine(
        queryFlow
            .debounce { q -> if (q.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
            .map { it.trim() }
            .distinctUntilChanged(),
        searchRetry
    ) { query, _ -> query }
        .flatMapLatest { query ->
            searchPager.reset() // a "load more" for the old query is stale too
            if (query.isEmpty()) {
                flowOf<SearchUpdate>(SearchUpdate.Cleared)
            } else {
                flow<SearchUpdate> {
                    emit(SearchUpdate.Loading(query))
                    emit(SearchUpdate.Loaded(query, searchPosts(query, page = 1)))
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
                is AppResult.Success -> {
                    setState { copy(searchedQuery = update.query, searchResults = r.data.items, isSearching = false) }
                    searchPager.reset(nextPage = 2, endReached = r.data.endReached)
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

    /** Page 2+ of the current search: results live in state (not cached), so append them here. */
    private suspend fun loadSearchPage(page: Int): AppResult<Boolean> {
        val query = currentState.searchedQuery ?: return AppResult.Success(true)
        val result = searchPosts(query, page)
        if (result is AppResult.Success) {
            setState { copy(searchResults = (searchResults + result.data.items).distinctBy { it.id }) }
        }
        return result.map { it.endReached }
    }

    // =====================================================================
    // Browse
    // =====================================================================

    private fun loadFirstPage(isPullToRefresh: Boolean) {
        firstPageJob?.cancel()
        browsePager.reset() // a refresh wins over an in-flight "load more"
        firstPageJob = viewModelScope.launch {
            setState { copy(isLoading = !isPullToRefresh, isRefreshing = isPullToRefresh, error = null) }
            when (val result = loadPostsPage(page = 1)) {
                is AppResult.Success -> {
                    setState { copy(isLoading = false, isRefreshing = false) }
                    browsePager.reset(nextPage = 2, endReached = result.data.endReached)
                }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (currentState.posts.isNotEmpty()) {
                        // Offline with cache: keep showing it, continue paging after what we have.
                        setState { copy(isLoading = false, isRefreshing = false) }
                        browsePager.reset(nextPage = currentState.posts.size / PAGE_SIZE + 1)
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
