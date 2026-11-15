package com.example.starter.feature.search.ui

import app.cash.turbine.test
import com.example.starter.core.ui.UiText
import com.example.starter.feature.search.data.FakeAssetServer
import com.example.starter.feature.search.data.SearchRepositoryImpl
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
class SearchViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val server = FakeAssetServer(latencyMs = 100)
    private fun vm() = SearchViewModel(SearchRepositoryImpl(server))

    private fun TestScope.type(vm: SearchViewModel, text: String) {
        vm.onIntent(SearchIntent.QueryChanged(text))
        runCurrent()
    }

    @Test
    fun `suggestions are debounced and need 2 characters`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        type(vm, "m")
        advanceUntilIdle()
        assertThat(server.suggestCalls).isEqualTo(0)

        for (t in listOf("mo", "mou", "moun")) {
            type(vm, t)
            advanceTimeBy(50)
        }
        advanceUntilIdle()
        assertThat(server.suggestCalls).isEqualTo(1) // only "moun"
        assertThat(vm.state.value.suggestions).containsExactly("mountain", "mountains")
    }

    @Test
    fun `slow suggestion request is cancelled by newer typing`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        type(vm, "oc")
        advanceTimeBy(250) // debounce fired, request in flight (100ms latency)
        type(vm, "fo")
        advanceUntilIdle()
        assertThat(vm.state.value.suggestions).containsExactly("forest")
    }

    @Test
    fun `clicking a suggestion submits it, hides suggestions, loads page 1`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(SearchIntent.SuggestionClicked("mountain"))
            assertThat(awaitItem()).isEqualTo(SearchEffect.HideKeyboard)
        }
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(showSuggestions).isFalse()
            assertThat(submittedQuery).isEqualTo("mountain")
            assertThat(results).hasSize(20)
            assertThat(recentSearches.first()).isEqualTo("mountain")
        }
    }

    @Test
    fun `infinite scroll appends until the end`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        vm.onIntent(SearchIntent.SuggestionClicked("mountain"))
        advanceUntilIdle()
        repeat(5) {
            vm.onIntent(SearchIntent.LoadNextPage)
            advanceUntilIdle()
        }
        assertThat(vm.state.value.paging.endReached).isTrue()
        val total = vm.state.value.results.size
        assertThat(total).isEqualTo(60)
        vm.onIntent(SearchIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(vm.state.value.results).hasSize(total)
    }

    @Test
    fun `new submit replaces results and cancels stale load-more`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        vm.onIntent(SearchIntent.SuggestionClicked("mountain"))
        advanceUntilIdle()
        vm.onIntent(SearchIntent.LoadNextPage) // in flight...
        runCurrent()
        vm.onIntent(SearchIntent.SuggestionClicked("ocean")) // ...replaced
        advanceUntilIdle()
        assertThat(vm.state.value.results.all { "ocean" in it.tags }).isTrue()
    }

    @Test
    fun `search error shows retry, retry re-runs the same query`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        server.failuresRemaining = 1
        vm.onIntent(SearchIntent.SuggestionClicked("forest"))
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("Simulated network failure"))

        vm.onIntent(SearchIntent.Retry)
        advanceUntilIdle()
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.results).isNotEmpty()
    }
}
