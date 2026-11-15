package com.example.starter.feature.quiz.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.safeCall
import com.example.starter.feature.quiz.domain.Quiz
import com.example.starter.feature.quiz.domain.QuizRepository
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(private val server: FakeQuizServer) : QuizRepository {
    override suspend fun quiz(id: Int): AppResult<Quiz> = safeCall { server.fetchQuiz(id) }
}
