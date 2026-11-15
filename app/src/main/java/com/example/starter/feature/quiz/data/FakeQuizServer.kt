package com.example.starter.feature.quiz.data

import com.example.starter.feature.quiz.domain.Question
import com.example.starter.feature.quiz.domain.Quiz
import java.io.IOException
import kotlinx.coroutines.delay

/** In-app stand-in for the course backend. */
class FakeQuizServer(private val latencyMs: Long = DEFAULT_LATENCY_MS) {
    var failuresRemaining = 0

    suspend fun fetchQuiz(@Suppress("UNUSED_PARAMETER") id: Int): Quiz {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
        return QUIZ
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 500L
        val QUIZ = Quiz(
            id = 1,
            title = "Android fundamentals: lesson 4 check",
            durationSeconds = 90,
            questions = listOf(
                Question(
                    1,
                    "Which component survives configuration changes?",
                    listOf("Activity", "ViewModel", "Fragment view", "Composable"),
                    1
                ),
                Question(2, "Which Flow always has a current value?", listOf("SharedFlow", "Channel", "StateFlow", "callbackFlow"), 2),
                Question(
                    3,
                    "Where should a one-time navigation event go?",
                    listOf("StateFlow", "Channel / effect", "LiveData", "remember"),
                    1
                ),
                Question(
                    4,
                    "What does SavedStateHandle protect against?",
                    listOf("Rotation only", "Process death", "Memory leaks", "ANRs"),
                    1
                ),
                Question(5, "Which dispatcher suits JSON parsing of a big file?", listOf("Main", "IO", "Default", "Unconfined"), 2)
            )
        )
    }
}
