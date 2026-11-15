package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.feature.posts.domain.usecase.RefreshPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** MVI: the screen sends intents, the ViewModel reduces them into one immutable state. */
@HiltViewModel
class PostListViewModel @Inject constructor(observePosts: ObservePostsUseCase, private val refreshPosts: RefreshPostsUseCase) :
    MviViewModel<PostListIntent, PostListState, PostListEffect>(PostListState(isLoading = true)) {

    private var refreshJob: Job? = null

    init {
        // Observe the DB (source of truth). Network results arrive here via Room.
        viewModelScope.launch {
            observePosts().collect { posts -> setState { copy(posts = posts) } }
        }
        onIntent(PostListIntent.Load)
    }

    override fun handleIntent(intent: PostListIntent) {
        when (intent) {
            PostListIntent.Load, PostListIntent.Retry -> refresh(isPullToRefresh = false)
            PostListIntent.Refresh -> refresh(isPullToRefresh = true)
            is PostListIntent.PostClicked -> sendEffect(PostListEffect.NavigateToDetail(intent.id))
        }
    }

    private fun refresh(isPullToRefresh: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            setState { copy(isLoading = !isPullToRefresh, isRefreshing = isPullToRefresh, error = null) }
            when (val result = refreshPosts()) {
                is AppResult.Success -> setState { copy(isLoading = false, isRefreshing = false) }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (currentState.posts.isNotEmpty()) {
                        // Offline with cache: keep showing it, just tell the user.
                        setState { copy(isLoading = false, isRefreshing = false) }
                        sendEffect(PostListEffect.ShowSnackbar(msg))
                    } else {
                        setState { copy(isLoading = false, isRefreshing = false, error = msg) }
                    }
                }
            }
        }
    }
}
