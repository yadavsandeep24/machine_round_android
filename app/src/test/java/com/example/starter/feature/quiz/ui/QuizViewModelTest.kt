package com.example.starter.feature.quiz.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.starter.core.common.Clock
import com.example.starter.feature.quiz.data.FakeQuizServer
import com.example.starter.feature.quiz.data.QuizRepositoryImpl
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

/** Quiz is 90 s with 5 questions; correct answers: 1, 2, 1, 1, 2. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val server = FakeQuizServer(latencyMs = 0)

    // Virtual time as the wall clock: delay() and "now" move together.
    private fun TestScope.vm(saved: SavedStateHandle = SavedStateHandle()) =
        QuizViewModel(saved, QuizRepositoryImpl(server), Clock { testScheduler.currentTime })

    private fun TestScope.answerAll(vm: QuizViewModel, picks: List<Int>) {
        picks.forEachIndexed { i, pick ->
            vm.onIntent(QuizIntent.JumpTo(i))
            vm.onIntent(QuizIntent.SelectOption(pick))
        }
        runCurrent()
    }

    @Test
    fun `timer counts down in whole seconds`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        runCurrent()
        assertThat(vm.state.value.remainingSeconds).isEqualTo(90)
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(vm.state.value.remainingSeconds).isEqualTo(60)
        vm.onIntent(QuizIntent.Submit) // stop the timer so the test can finish
    }

    @Test
    fun `time up auto-submits with whatever was answered`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        runCurrent()
        answerAll(vm, listOf(1, 2)) // 2 of 5, both correct
        vm.effects.test {
            advanceTimeBy(90_001)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(QuizEffect.TimeUp)
        }
        with(vm.state.value.result!!) {
            assertThat(timedOut).isTrue()
            assertThat(score).isEqualTo(2)
            assertThat(unanswered).isEqualTo(3)
        }
    }

    @Test
    fun `manual submit grades, stops the timer and locks answers`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        runCurrent()
        answerAll(vm, listOf(1, 2, 1, 0, 2)) // one wrong
        vm.onIntent(QuizIntent.Submit)
        runCurrent()
        assertThat(vm.state.value.result!!.score).isEqualTo(4)

        vm.onIntent(QuizIntent.JumpTo(3))
        vm.onIntent(QuizIntent.SelectOption(1)) // ignored after submit
        advanceUntilIdle() // would hang if the timer were still running
        assertThat(vm.state.value.result!!.score).isEqualTo(4)
        assertThat(vm.state.value.answers[3]).isEqualTo(0)
    }

    @Test
    fun `process death restores answers, position and exact remaining time`() = runTest(mainRule.dispatcher) {
        val saved = SavedStateHandle()
        val first = vm(saved)
        runCurrent()
        answerAll(first, listOf(1, 2, 1))
        first.onIntent(QuizIntent.JumpTo(2))
        advanceTimeBy(20_000)
        runCurrent()

        // Simulate process death: a new ViewModel gets only the saved state; 10 more seconds pass meanwhile.
        val restoredState = SavedStateHandle(saved.keys().associateWith { saved.get<Any>(it) })
        first.onIntent(QuizIntent.Submit) // stop the old instance's timer (it's "dead")
        advanceTimeBy(10_000)
        val restored = vm(restoredState)
        runCurrent()
        with(restored.state.value) {
            assertThat(answers.take(3)).containsExactly(1, 2, 1).inOrder()
            assertThat(currentIndex).isEqualTo(2)
            assertThat(remainingSeconds).isEqualTo(60) // 90 - 20 - 10
        }
        restored.onIntent(QuizIntent.Submit)
    }

    @Test
    fun `retake starts a fresh attempt`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        runCurrent()
        answerAll(vm, listOf(1))
        vm.onIntent(QuizIntent.Submit)
        advanceTimeBy(5_000)
        vm.onIntent(QuizIntent.Retake)
        runCurrent()
        with(vm.state.value) {
            assertThat(result).isNull()
            assertThat(answers.all { it == null }).isTrue()
            assertThat(remainingSeconds).isEqualTo(90)
        }
        vm.onIntent(QuizIntent.Submit)
    }

    @Test
    fun `load failure shows error, retry recovers`() = runTest(mainRule.dispatcher) {
        server.failuresRemaining = 1
        val vm = vm()
        runCurrent()
        assertThat(vm.state.value.error).isNotNull()
        vm.onIntent(QuizIntent.Retry)
        runCurrent()
        assertThat(vm.state.value.quiz).isNotNull()
        vm.onIntent(QuizIntent.Submit)
    }
}
