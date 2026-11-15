package com.example.starter.feature.posts.di

import com.example.starter.feature.posts.data.PostRepositoryImpl
import com.example.starter.feature.posts.domain.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostModule {
    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository
}
