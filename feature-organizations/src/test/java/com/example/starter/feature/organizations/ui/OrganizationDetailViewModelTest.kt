package com.example.starter.feature.organizations.ui

import androidx.lifecycle.SavedStateHandle
import com.example.starter.core.ui.UiState
import com.example.starter.feature.organizations.MainDispatcherRule
import com.example.starter.feature.organizations.domain.OrganizationDetail
import com.example.starter.feature.organizations.ui.detail.OrganizationDetailViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repo = FakeOrganizationRepository()
    private fun vm() = OrganizationDetailViewModel(SavedStateHandle(mapOf("ein" to 100_000_003L)), repo)

    @Test
    fun `loads the organization from the route's ein`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertThat((vm.state.value as UiState.Success<OrganizationDetail>).data.name).isEqualTo("Org 3")
    }

    @Test
    fun `error then retry`() = runTest(mainRule.dispatcher) {
        repo.failDetail = true
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
        repo.failDetail = false
        vm.retry()
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }
}
