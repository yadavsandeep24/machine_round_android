package com.example.starter.feature.quiz.domain

import com.example.starter.core.common.AppResult

interface QuizRepository {
    suspend fun quiz(id: Int): AppResult<Quiz>
}
