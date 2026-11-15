package com.example.starter.feature.posts.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.example.starter.core.model.Post
import com.example.starter.core.ui.UiState
import com.example.starter.feature.posts.domain.usecase.GetPostUseCase
import com.example.starter.feature.posts.domain.usecase.ToggleFavoriteUseCase
import com.example.starter.testutil.FakePostRepository
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    // toRoute<PostDetailDestination>() reads the "id" key
    private fun vm(repo: FakePostRepository, id: Int) = PostDetailViewModel(
        SavedStateHandle(mapOf("id" to id)),
        GetPostUseCase(repo),
        ToggleFavoriteUseCase(repo),
    )

    @Test
    fun `uncached post (e g from search) is loaded from network`() = runTest {
        val repo = FakePostRepository(remote = listOf(Post(45, 1, "Deep link", "b")))
        val state = vm(repo, 45).uiState.filterIsInstance<UiState.Success<Post>>().first()
        assertThat(state.data.title).isEqualTo("Deep link")
    }

    @Test
    fun `missing post shows error`() = runTest {
        val state = vm(FakePostRepository(), 99).uiState.filterIsInstance<UiState.Error>().first()
        assertThat(state).isInstanceOf(UiState.Error::class.java)
    }
}
