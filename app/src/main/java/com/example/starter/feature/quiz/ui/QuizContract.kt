package com.example.starter.feature.quiz.ui

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText
import com.example.starter.feature.quiz.domain.Quiz
import com.example.starter.feature.quiz.domain.QuizResult

data class QuizState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val quiz: Quiz? = null,
    val currentIndex: Int = 0,
    /** By question position; null = not answered yet. */
    val answers: List<Int?> = emptyList(),
    val remainingSeconds: Int = 0,
    /** Non-null once submitted (manually or by the timer). Answers are locked from then on. */
    val result: QuizResult? = null
) : MviState {
    val isSubmitted: Boolean get() = result != null
    val unansweredCount: Int get() = answers.count { it == null }
    val isLastQuestion: Boolean get() = quiz != null && currentIndex == quiz.questions.lastIndex
}

sealed interface QuizIntent : UiIntent {
    data object Retry : QuizIntent
    data class SelectOption(val optionIndex: Int) : QuizIntent
    data object Next : QuizIntent
    data object Previous : QuizIntent
    data class JumpTo(val questionIndex: Int) : QuizIntent
    data object Submit : QuizIntent
    data object Retake : QuizIntent
}

sealed interface QuizEffect : UiEffect {
    data object TimeUp : QuizEffect
}
