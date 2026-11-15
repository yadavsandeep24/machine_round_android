package com.example.starter.feature.organizations.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.starter.core.ui.UiText
import com.example.starter.feature.organizations.MainDispatcherRule
import com.example.starter.feature.organizations.ui.list.OrganizationListEffect
import com.example.starter.feature.organizations.ui.list.OrganizationListIntent
import com.example.starter.feature.organizations.ui.list.OrganizationListViewModel
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
class OrganizationListViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repo = FakeOrganizationRepository()
    private fun vm(saved: SavedStateHandle = SavedStateHandle()) = OrganizationListViewModel(saved, repo)

    private fun TestScope.type(vm: OrganizationListViewModel, text: String) {
        vm.onIntent(OrganizationListIntent.QueryChanged(text))
        runCurrent()
    }

    @Test
    fun `opens on the first page of all organizations`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(items).hasSize(25)
            assertThat(totalResults).isEqualTo(60)
            assertThat(isLoading).isFalse()
        }
        assertThat(repo.requests).containsExactly("" to 0)
    }

    @Test
    fun `infinite scroll loads 0-based pages until the end`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        repeat(4) {
            vm.onIntent(OrganizationListIntent.LoadNextPage)
            advanceUntilIdle()
        }
        assertThat(vm.state.value.items).hasSize(60)
        assertThat(vm.state.value.paging.endReached).isTrue()
        assertThat(repo.requests).containsExactly("" to 0, "" to 1, "" to 2).inOrder()
    }

    @Test
    fun `search is debounced and restarts paging for the new query`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(OrganizationListIntent.LoadNextPage)
        advanceUntilIdle()
        for (t in listOf("O", "Or", "Org 1")) {
            type(vm, t)
            advanceTimeBy(100)
        }
        advanceUntilIdle()
        assertThat(repo.requests).containsExactly("" to 0, "" to 1, "Org 1" to 0).inOrder()
        assertThat(vm.state.value.items.map { it.name }).contains("Org 10")
        assertThat(vm.state.value.items.all { "Org 1" in it.name }).isTrue()
    }

    @Test
    fun `no matches shows an empty result, not an error`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        type(vm, "nothing like this")
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(items).isEmpty()
            assertThat(error).isNull()
            assertThat(loadedQuery).isEqualTo("nothing like this")
        }
    }

    @Test
    fun `first page error shows retry, retry reloads`() = runTest(mainRule.dispatcher) {
        repo.failingPages += 0
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("boom"))
        repo.failingPages.clear()
        vm.onIntent(OrganizationListIntent.Retry)
        advanceUntilIdle()
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.items).hasSize(25)
    }

    @Test
    fun `load-more error keeps items and needs an explicit retry`() = runTest(mainRule.dispatcher) {
        repo.failingPages += 1
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(OrganizationListIntent.LoadNextPage)
        advanceUntilIdle()
        assertThat(vm.state.value.paging.loadMoreError).isNotNull()
        vm.onIntent(OrganizationListIntent.LoadNextPage) // scrolling doesn't auto-retry
        advanceUntilIdle()
        assertThat(repo.requests.count { it.second == 1 }).isEqualTo(1)

        repo.failingPages.clear()
        vm.onIntent(OrganizationListIntent.RetryLoadMore)
        advanceUntilIdle()
        assertThat(vm.state.value.items).hasSize(50)
    }

    @Test
    fun `tapping a row navigates to its detail`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.effects.test {
            vm.onIntent(OrganizationListIntent.OrganizationClicked(100_000_007))
            assertThat(awaitItem()).isEqualTo(OrganizationListEffect.NavigateToDetail(100_000_007))
        }
    }

    @Test
    fun `search query survives process death`() = runTest(mainRule.dispatcher) {
        val restored = vm(SavedStateHandle(mapOf("query" to "Org 5")))
        assertThat(restored.state.value.query).isEqualTo("Org 5")
        advanceUntilIdle()
        assertThat(repo.requests).containsExactly("Org 5" to 0)
    }
}
