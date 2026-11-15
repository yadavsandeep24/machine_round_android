package com.example.starter.feature.posts.ui.list

import app.cash.turbine.test
import com.example.starter.core.model.Post
import com.example.starter.core.ui.UiText
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.testutil.FakePostRepository
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostListViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val posts = listOf(Post(1, 1, "Kotlin flows", "b"), Post(2, 1, "Compose tips", "b"))

    private fun vm(repo: FakePostRepository) = PostListViewModel(ObservePostsUseCase(repo), LoadPostsPageUseCase(repo))

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

    // ---------- pagination ----------

    private val many = List(45) { Post(it + 1, 1, "Post ${it + 1}", "b") } // 20 + 20 + 5

    @Test
    fun `first load fetches only page 1`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = many))
        advanceUntilIdle()
        assertThat(viewModel.state.value.posts).hasSize(20)
        assertThat(viewModel.state.value.paging.endReached).isFalse()
    }

    @Test
    fun `load next page appends until end reached`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = many)
        val viewModel = vm(repo)
        advanceUntilIdle()

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(viewModel.state.value.posts).hasSize(40)

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(viewModel.state.value.posts).hasSize(45)
        assertThat(viewModel.state.value.paging.endReached).isTrue()

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(repo.requestedPages).containsExactly(1, 2, 3).inOrder() // no request past the end
    }

    @Test
    fun `duplicate load more while loading only requests once`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = many)
        val viewModel = vm(repo)
        advanceUntilIdle()
        repeat(3) { viewModel.onIntent(PostListIntent.LoadNextPage) }
        advanceUntilIdle()
        assertThat(repo.requestedPages).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `load more failure keeps items, shows footer error, retry recovers`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = many, failingPages = mutableSetOf(2))
        val viewModel = vm(repo)
        advanceUntilIdle()

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(viewModel.state.value.paging.loadMoreError).isEqualTo(UiText.Dynamic("boom"))
        assertThat(viewModel.state.value.posts).hasSize(20)

        viewModel.onIntent(PostListIntent.LoadNextPage) // scrolling again must NOT auto-retry
        advanceUntilIdle()
        assertThat(repo.requestedPages).containsExactly(1, 2).inOrder()

        repo.failingPages.clear()
        viewModel.onIntent(PostListIntent.RetryLoadMore)
        advanceUntilIdle()
        assertThat(viewModel.state.value.paging.loadMoreError).isNull()
        assertThat(viewModel.state.value.posts).hasSize(40)
    }

    @Test
    fun `refresh resets to page 1`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = many)
        val viewModel = vm(repo)
        advanceUntilIdle()
        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()

        viewModel.onIntent(PostListIntent.Refresh)
        advanceUntilIdle()
        assertThat(viewModel.state.value.posts).hasSize(20)

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(repo.requestedPages).containsExactly(1, 2, 1, 2).inOrder()
    }
}
