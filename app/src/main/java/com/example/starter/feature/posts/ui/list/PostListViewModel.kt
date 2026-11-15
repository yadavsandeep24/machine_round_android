package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.map
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.paging.Paginator
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase.Companion.PAGE_SIZE
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * MVI with hand-written pagination. The ViewModel owns page 1 (first load / refresh);
 * "load more" mechanics are delegated to the reusable [Paginator].
 */
@HiltViewModel
class PostListViewModel @Inject constructor(observePosts: ObservePostsUseCase, private val loadPostsPage: LoadPostsPageUseCase) :
    MviViewModel<PostListIntent, PostListState, PostListEffect>(PostListState(isLoading = true)) {

    private val pager = Paginator(
        scope = viewModelScope,
        loadPage = { page -> loadPostsPage(page).map { it.endReached } }, // rows land in Room
        onStatusChange = { status -> setState { copy(paging = status) } }
    )
    private var firstPageJob: Job? = null

    init {
        viewModelScope.launch {
            observePosts().collect { posts -> setState { copy(posts = posts) } }
        }
        onIntent(PostListIntent.Load)
    }

    override fun handleIntent(intent: PostListIntent) {
        when (intent) {
            PostListIntent.Load, PostListIntent.Retry -> loadFirstPage(isPullToRefresh = false)
            PostListIntent.Refresh -> loadFirstPage(isPullToRefresh = true)
            PostListIntent.LoadNextPage -> if (currentState.canLoadMore) pager.loadNext()
            PostListIntent.RetryLoadMore -> pager.retry()
        }
    }

    private fun loadFirstPage(isPullToRefresh: Boolean) {
        firstPageJob?.cancel()
        pager.reset() // a refresh wins over an in-flight "load more"
        firstPageJob = viewModelScope.launch {
            setState { copy(isLoading = !isPullToRefresh, isRefreshing = isPullToRefresh, error = null) }
            when (val result = loadPostsPage(page = 1)) {
                is AppResult.Success -> {
                    setState { copy(isLoading = false, isRefreshing = false) }
                    pager.reset(nextPage = 2, endReached = result.data.endReached)
                }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (currentState.posts.isNotEmpty()) {
                        // Offline with cache: keep showing it, continue paging after what we have.
                        setState { copy(isLoading = false, isRefreshing = false) }
                        pager.reset(nextPage = currentState.posts.size / PAGE_SIZE + 1)
                        sendEffect(PostListEffect.ShowSnackbar(msg))
                    } else {
                        setState { copy(isLoading = false, isRefreshing = false, error = msg) }
                    }
                }
            }
        }
    }
}
