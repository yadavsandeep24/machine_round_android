package com.example.starter.feature.posts.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starter.R
import com.example.starter.core.model.Post
import com.example.starter.core.ui.UiState
import com.example.starter.core.ui.UiText
import com.example.starter.feature.posts.domain.usecase.GetPostUseCase
import com.example.starter.navigation.PostDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * MVVM example: exposes observable state (+ public functions when the screen has actions).
 * Compare with PostListViewModel (MVI: single onIntent entry point + effects).
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, getPost: GetPostUseCase) : ViewModel() {

    // Type-safe nav stores args under their property names. Reading by name (instead of
    // toRoute<PostDetailDestination>()) works in plain JVM unit tests without Robolectric.
    private val postId: Int = checkNotNull(savedStateHandle[PostDetailDestination::id.name])

    val uiState: StateFlow<UiState<Post>> = getPost(postId)
        .map { post -> if (post != null) UiState.Success(post) else UiState.Error(UiText.Res(R.string.post_detail_load_error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private companion object {
        /** Keep the upstream alive across rotation, stop it shortly after leaving the screen. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
