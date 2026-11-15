package com.example.starter.feature.posts.ui.list

import com.example.starter.core.model.Post
import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText

/** MVI contract: everything the screen can show, do, and emit, in one place. */
data class PostListState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null
) : MviState

sealed interface PostListIntent : UiIntent {
    data object Load : PostListIntent
    data object Refresh : PostListIntent
    data object Retry : PostListIntent
    data object OpenPaging3Sample : PostListIntent
}

sealed interface PostListEffect : UiEffect {
    data object NavigateToPaging3Sample : PostListEffect
    data class ShowSnackbar(val message: UiText) : PostListEffect
}
