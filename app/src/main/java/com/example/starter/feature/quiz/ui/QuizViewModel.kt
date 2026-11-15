package com.example.starter.feature.quiz.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.Clock
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.quiz.domain.QuizRepository
import com.example.starter.feature.quiz.domain.grade
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timer design: we persist the DEADLINE (wall-clock millis), not a countdown counter.
 *  - Rotation: ViewModel survives anyway.
 *  - Process death: the deadline comes back from SavedStateHandle and the remaining time is exact,
 *    including the time the app spent dead. A tick counter would reset or drift.
 * Answers and the current question are saved too, so the attempt resumes where the user left it.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repository: QuizRepository,
    private val clock: Clock
) : MviViewModel<QuizIntent, QuizState, QuizEffect>(QuizState()) {

    private var timerJob: Job? = null

    init {
        load()
    }

    override fun handleIntent(intent: QuizIntent) {
        val s = currentState
        when (intent) {
            QuizIntent.Retry -> load()
            is QuizIntent.SelectOption -> if (!s.isSubmitted) {
                val answers = s.answers.toMutableList().also { it[s.currentIndex] = intent.optionIndex }
                savedState[KEY_ANSWERS] = ArrayList(answers.map { it ?: UNANSWERED })
                setState { copy(answers = answers) }
            }
            QuizIntent.Next -> goTo(s.currentIndex + 1)
            QuizIntent.Previous -> goTo(s.currentIndex - 1)
            is QuizIntent.JumpTo -> goTo(intent.questionIndex)
            QuizIntent.Submit -> submit(timedOut = false)
            QuizIntent.Retake -> {
                listOf(KEY_DEADLINE, KEY_ANSWERS, KEY_INDEX, KEY_SUBMITTED).forEach { savedState.remove<Any>(it) }
                load()
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { QuizState(isLoading = true) }
            when (val result = repository.quiz(QUIZ_ID)) {
                is AppResult.Success -> start(result.data)
                is AppResult.Failure -> setState { copy(isLoading = false, error = result.throwable.toUserMessage()) }
            }
        }
    }

    private fun start(quiz: com.example.starter.feature.quiz.domain.Quiz) {
        val deadline = savedState.get<Long>(KEY_DEADLINE)
            ?: (clock.nowMillis() + quiz.durationSeconds * MILLIS).also { savedState[KEY_DEADLINE] = it }
        val answers = savedState.get<ArrayList<Int>>(KEY_ANSWERS)?.map { it.takeIf { a -> a != UNANSWERED } }
            ?: List(quiz.questions.size) { null }
        setState {
            copy(isLoading = false, error = null, quiz = quiz, answers = answers, currentIndex = savedState[KEY_INDEX] ?: 0)
        }
        if (savedState.get<Boolean>(KEY_SUBMITTED) == true) {
            setState { copy(result = quiz.grade(answers, timedOut = remainingSeconds(deadline) == 0)) }
        } else {
            startTimer(deadline)
        }
    }

    private fun startTimer(deadline: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = remainingSeconds(deadline)
                setState { copy(remainingSeconds = remaining) }
                if (remaining == 0) {
                    submit(timedOut = true)
                    break
                }
                // Wake exactly when the displayed second changes (no drift from fixed 1s sleeps).
                val untilNextSecond = (deadline - clock.nowMillis()) % MILLIS
                delay(if (untilNextSecond == 0L) MILLIS else untilNextSecond)
            }
        }
    }

    private fun remainingSeconds(deadline: Long): Int = ceil((deadline - clock.nowMillis()).coerceAtLeast(0) / MILLIS.toDouble()).toInt()

    private fun goTo(index: Int) {
        val quiz = currentState.quiz ?: return
        if (index !in quiz.questions.indices) return
        savedState[KEY_INDEX] = index
        setState { copy(currentIndex = index) }
    }

    private fun submit(timedOut: Boolean) {
        val s = currentState
        val quiz = s.quiz ?: return
        if (s.isSubmitted) return
        timerJob?.cancel()
        savedState[KEY_SUBMITTED] = true
        setState { copy(result = quiz.grade(answers, timedOut)) }
        if (timedOut) sendEffect(QuizEffect.TimeUp)
    }

    private companion object {
        const val QUIZ_ID = 1
        const val MILLIS = 1_000L
        const val UNANSWERED = -1
        const val KEY_DEADLINE = "quiz_deadline"
        const val KEY_ANSWERS = "quiz_answers"
        const val KEY_INDEX = "quiz_index"
        const val KEY_SUBMITTED = "quiz_submitted"
    }
}
