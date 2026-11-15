package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.starter.core.model.Post
import com.example.starter.core.ui.UiText
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.feature.posts.domain.usecase.RefreshPostsUseCase
import com.example.starter.feature.posts.domain.usecase.SearchPostsUseCase
import com.example.starter.testutil.FakePostRepository
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostListViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val posts = listOf(Post(1, 1, "Kotlin flows", "b"), Post(2, 1, "Compose tips", "b"))

    private fun vm(repo: FakePostRepository, savedState: SavedStateHandle = SavedStateHandle()) =
        PostListViewModel(savedState, ObservePostsUseCase(repo), RefreshPostsUseCase(repo), SearchPostsUseCase(repo))

    private fun TestScope.type(viewModel: PostListViewModel, text: String) {
        viewModel.onIntent(PostListIntent.QueryChanged(text))
        runCurrent()
    }

    @Test
    fun `load success shows posts`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts))
        advanceUntilIdle()
        with(viewModel.state.value) {
            assertThat(isLoading).isFalse()
            assertThat(error).isNull()
            assertThat(this.posts.map { it.id }).containsExactly(1, 2).inOrder()
        }
    }

    @Test
    fun `load failure with empty cache shows full-screen error`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(shouldFail = true))
        advanceUntilIdle()
        assertThat(viewModel.state.value.error).isEqualTo(UiText.Dynamic("boom"))
    }

    @Test
    fun `failure with cached data keeps the list and shows a snackbar`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(shouldFail = true).apply { db.value = posts }
        val viewModel = vm(repo)
        viewModel.effects.test {
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(PostListEffect.ShowSnackbar(UiText.Dynamic("boom")))
        }
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.posts).hasSize(2)
    }

    @Test
    fun `retry after error loads posts`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts, shouldFail = true)
        val viewModel = vm(repo)
        advanceUntilIdle()

        repo.shouldFail = false
        viewModel.onIntent(PostListIntent.Retry)
        advanceUntilIdle()
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.posts).hasSize(2)
    }

    @Test
    fun `pull to refresh toggles isRefreshing, not the full-screen loader`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts, delayMs = 1_000))
        advanceUntilIdle()

        viewModel.onIntent(PostListIntent.Refresh)
        mainRule.dispatcher.scheduler.runCurrent()
        assertThat(viewModel.state.value.isRefreshing).isTrue()
        assertThat(viewModel.state.value.isLoading).isFalse()

        advanceUntilIdle()
        assertThat(viewModel.state.value.isRefreshing).isFalse()
    }

    // ---------- search ----------

    @Test
    fun `search is debounced - only the final query hits the server`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts)
        val viewModel = vm(repo)
        advanceUntilIdle()

        for (text in listOf("k", "ko", "kot", "kotlin")) {
            type(viewModel, text)
            assertThat(viewModel.state.value.query).isEqualTo(text) // text field is never debounced
            advanceTimeBy(100)
        }
        assertThat(repo.searchRequests).isEmpty()

        advanceUntilIdle()
        assertThat(repo.searchRequests).containsExactly("kotlin")
        assertThat(viewModel.state.value.visiblePosts.map { it.id }).containsExactly(1)
    }

    @Test
    fun `new query cancels the in-flight request for the old one`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts, searchDelays = mutableMapOf("kotlin" to 5_000L))
        val viewModel = vm(repo)
        advanceUntilIdle()

        type(viewModel, "kotlin")
        advanceTimeBy(301)
        runCurrent()
        assertThat(repo.searchRequests).containsExactly("kotlin") // slow request started

        type(viewModel, "compose")
        advanceUntilIdle()

        assertThat(repo.completedSearches).containsExactly("compose") // "kotlin" never finished
        assertThat(viewModel.state.value.searchedQuery).isEqualTo("compose")
        assertThat(viewModel.state.value.visiblePosts.map { it.id }).containsExactly(2)
    }

    @Test
    fun `retyping the same query does not search again`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts)
        val viewModel = vm(repo)
        advanceUntilIdle()

        type(viewModel, "kotlin")
        advanceUntilIdle()
        type(viewModel, "kotlin ") // trimmed -> same query
        advanceUntilIdle()
        type(viewModel, "kotli")
        advanceTimeBy(100)
        type(viewModel, "kotlin") // back before the debounce fired
        advanceUntilIdle()

        assertThat(repo.searchRequests).containsExactly("kotlin")
    }

    @Test
    fun `clearing the query returns to browse immediately`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts))
        advanceUntilIdle()
        type(viewModel, "kotlin")
        advanceUntilIdle()

        type(viewModel, "") // no advanceTimeBy: clearing skips the debounce
        with(viewModel.state.value) {
            assertThat(isSearchActive).isFalse()
            assertThat(searchedQuery).isNull()
            assertThat(visiblePosts.map { it.id }).containsExactly(1, 2)
        }
    }

    @Test
    fun `search error shows retry, retry re-runs the same query`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts, shouldFailSearch = true)
        val viewModel = vm(repo)
        advanceUntilIdle()

        type(viewModel, "kotlin")
        advanceUntilIdle()
        assertThat(viewModel.state.value.searchError).isEqualTo(UiText.Dynamic("search failed"))

        repo.shouldFailSearch = false
        viewModel.onIntent(PostListIntent.Retry)
        advanceUntilIdle()
        assertThat(viewModel.state.value.searchError).isNull()
        assertThat(viewModel.state.value.visiblePosts.map { it.id }).containsExactly(1)
    }

    @Test
    fun `search query survives process death`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts)
        val saved = SavedStateHandle()
        val first = vm(repo, saved)
        advanceUntilIdle()
        type(first, "compose")
        advanceUntilIdle()

        // The system recreates the ViewModel with the same SavedStateHandle contents.
        val restored = vm(repo, SavedStateHandle(mapOf("query" to saved.get<String>("query"))))
        assertThat(restored.state.value.query).isEqualTo("compose")
        advanceUntilIdle()
        assertThat(restored.state.value.visiblePosts.map { it.id }).containsExactly(2)
    }
}
