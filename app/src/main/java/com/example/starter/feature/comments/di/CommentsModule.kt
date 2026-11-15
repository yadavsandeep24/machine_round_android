package com.example.starter.feature.comments.di

import com.example.starter.feature.comments.data.CommentRepositoryImpl
import com.example.starter.feature.comments.data.FakeCommentServer
import com.example.starter.feature.comments.domain.CommentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommentsModule {
    @Binds
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository

    companion object {
        @Provides
        @Singleton
        fun provideFakeCommentServer(): FakeCommentServer = FakeCommentServer()
    }
}
