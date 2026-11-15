package com.example.starter.feature.posts.ui.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.starter.core.model.Post
import com.example.starter.core.ui.UiText
import com.example.starter.feature.posts.domain.usecase.LoadPostsPageUseCase
import com.example.starter.feature.posts.domain.usecase.ObserveFavoriteIdsUseCase
import com.example.starter.feature.posts.domain.usecase.ObservePostsUseCase
import com.example.starter.feature.posts.domain.usecase.SearchPostsUseCase
import com.example.starter.feature.posts.domain.usecase.ToggleFavoriteUseCase
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

    private val posts = listOf(
        Post(1, 1, "Kotlin flows", "b"),
        Post(2, 1, "Compose tips", "b")
    )
    private val many = List(45) { Post(it + 1, 1, "Post ${it + 1}", "b") } // 20 + 20 + 5

    private fun vm(repo: FakePostRepository, savedState: SavedStateHandle = SavedStateHandle()) = PostListViewModel(
        savedStateHandle = savedState,
        observePosts = ObservePostsUseCase(repo),
        observeFavoriteIds = ObserveFavoriteIdsUseCase(repo),
        loadPostsPage = LoadPostsPageUseCase(repo),
        searchPosts = SearchPostsUseCase(repo),
        toggleFavorite = ToggleFavoriteUseCase(repo)
    )

    private fun TestScope.type(viewModel: PostListViewModel, text: String) {
        viewModel.onIntent(PostListIntent.QueryChanged(text))
        runCurrent()
    }

    // ---------- browse ----------

    @Test
    fun `load success shows posts`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts))
        advanceUntilIdle()
        with(viewModel.state.value) {
            assertThat(isLoading).isFalse()
            assertThat(error).isNull()
            assertThat(visiblePosts.map { it.id }).containsExactly(1, 2).inOrder()
        }
    }

    @Test
    fun `load failure with empty cache shows error`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(shouldFail = true))
        advanceUntilIdle()
        assertThat(viewModel.state.value.error).isEqualTo(UiText.Dynamic("boom"))
    }

    @Test
    fun `favorite moves post to top`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts))
        advanceUntilIdle()
        viewModel.onIntent(PostListIntent.ToggleFavorite(posts[1]))
        advanceUntilIdle()
        assertThat(viewModel.state.value.posts.first().id).isEqualTo(2)
    }

    @Test
    fun `click emits navigate effect`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = posts))
        viewModel.effects.test {
            viewModel.onIntent(PostListIntent.PostClicked(7))
            assertThat(awaitItem()).isEqualTo(PostListEffect.NavigateToDetail(7))
        }
    }

    // ---------- browse pagination ----------

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

        // scrolling again must NOT auto-retry
        viewModel.onIntent(PostListIntent.LoadNextPage)
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
        assertThat(repo.searchRequests).containsExactly("kotlin" to 1)
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
        assertThat(repo.searchRequests).containsExactly("kotlin" to 1) // slow request started

        type(viewModel, "compose")
        advanceUntilIdle()

        assertThat(repo.completedSearches).containsExactly("compose") // "kotlin" never finished
        with(viewModel.state.value) {
            assertThat(searchedQuery).isEqualTo("compose")
            assertThat(visiblePosts.map { it.id }).containsExactly(2)
        }
    }

    @Test
    fun `retyping the same query does not search again`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts)
        val viewModel = vm(repo)
        advanceUntilIdle()

        type(viewModel, "kotlin")
        advanceUntilIdle()
        type(viewModel, "kotlin ")
        advanceUntilIdle() // trimmed -> same query
        type(viewModel, "kotli")
        advanceTimeBy(100)
        type(viewModel, "kotlin")
        advanceUntilIdle() // back before the debounce fired

        assertThat(repo.searchRequests).containsExactly("kotlin" to 1)
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
    fun `search results are paginated`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = many) // every title contains "Post"
        val viewModel = vm(repo)
        advanceUntilIdle()

        type(viewModel, "post")
        advanceUntilIdle()
        assertThat(viewModel.state.value.visiblePosts).hasSize(20)

        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        viewModel.onIntent(PostListIntent.LoadNextPage)
        advanceUntilIdle()
        with(viewModel.state.value) {
            assertThat(visiblePosts).hasSize(45)
            assertThat(paging.endReached).isTrue()
        }
        assertThat(repo.requestedPages).containsExactly(1) // browse paging untouched
        assertThat(repo.searchRequests.map { it.second }).containsExactly(1, 2, 3).inOrder()
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
        with(viewModel.state.value) {
            assertThat(searchError).isNull()
            assertThat(visiblePosts.map { it.id }).containsExactly(1)
        }
    }

    @Test
    fun `favoriting a search result is reflected live`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(FakePostRepository(remote = many))
        advanceUntilIdle()
        type(viewModel, "post 45")
        advanceUntilIdle() // post 45 is not in the browse cache (page 3)

        val hit = viewModel.state.value.visiblePosts.single()
        viewModel.onIntent(PostListIntent.ToggleFavorite(hit))
        advanceUntilIdle()

        assertThat(viewModel.state.value.visiblePosts.single().isFavorite).isTrue()
    }

    @Test
    fun `search query survives process death`() = runTest(mainRule.dispatcher) {
        val repo = FakePostRepository(remote = posts)
        val saved = SavedStateHandle()
        val first = vm(repo, saved)
        advanceUntilIdle()
        type(first, "compose")
        advanceUntilIdle()

        // System recreates the ViewModel with the same SavedStateHandle contents.
        val restored = vm(repo, SavedStateHandle(mapOf("query" to saved.get<String>("query"))))
        assertThat(restored.state.value.query).isEqualTo("compose")
        advanceUntilIdle()
        assertThat(restored.state.value.visiblePosts.map { it.id }).containsExactly(2)
    }
}
