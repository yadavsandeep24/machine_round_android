package com.example.starter.feature.quiz.di

import com.example.starter.feature.quiz.data.FakeQuizServer
import com.example.starter.feature.quiz.data.QuizRepositoryImpl
import com.example.starter.feature.quiz.domain.QuizRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuizModule {
    @Binds
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    companion object {
        @Provides
        @Singleton
        fun provideFakeQuizServer(): FakeQuizServer = FakeQuizServer()
    }
}
