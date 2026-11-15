package com.example.starter.feature.comments.ui

import com.example.starter.core.ui.UiText
import com.example.starter.feature.comments.data.CommentRepositoryImpl
import com.example.starter.feature.comments.data.FakeCommentServer
import com.example.starter.feature.comments.domain.PetLabel
import com.example.starter.feature.comments.domain.Preference
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentThreadViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val server = FakeCommentServer(latencyMs = 100)
    private fun vm() = CommentThreadViewModel(CommentRepositoryImpl(server), computeDispatcher = mainRule.dispatcher)

    @Test
    fun `loads the whole thread by default`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.rows).hasSize(21)
        assertThat(vm.state.value.hiddenCount).isEqualTo(0)
    }

    @Test
    fun `dog person sees no cat comments and a hidden count`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(CommentThreadIntent.SetPreference(Preference.DOG_PERSON))
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(rows.none { it.comment.label == PetLabel.CAT }).isTrue()
            assertThat(hiddenCount).isEqualTo(21 - rows.size)
        }
    }

    @Test
    fun `score threshold brings popular comments back`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(CommentThreadIntent.SetPreference(Preference.DOG_PERSON))
        advanceUntilIdle()
        val before = vm.state.value.rows.size
        vm.onIntent(CommentThreadIntent.SetScoreThreshold(50))
        advanceUntilIdle()
        assertThat(vm.state.value.rows.size).isGreaterThan(before)
        assertThat(vm.state.value.rows.any { it.comment.id == 6 }).isTrue() // 210-pt cat comment
    }

    @Test
    fun `collapsing a comment hides its replies, toggling restores them`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(CommentThreadIntent.ToggleCollapse(6))
        advanceUntilIdle()
        assertThat(vm.state.value.rows.map { it.comment.id }).containsNoneOf(7, 8, 9, 10)
        vm.onIntent(CommentThreadIntent.ToggleCollapse(6))
        advanceUntilIdle()
        assertThat(vm.state.value.rows.map { it.comment.id }).containsAtLeast(7, 8, 9, 10)
    }

    @Test
    fun `load failure shows error, retry recovers`() = runTest(mainRule.dispatcher) {
        server.failuresRemaining = 1
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("Simulated network failure"))
        vm.onIntent(CommentThreadIntent.Retry)
        advanceUntilIdle()
        assertThat(vm.state.value.rows).hasSize(21)
    }
}
