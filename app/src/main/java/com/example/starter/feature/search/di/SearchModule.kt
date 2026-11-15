package com.example.starter.feature.search.di

import com.example.starter.feature.search.data.FakeAssetServer
import com.example.starter.feature.search.data.SearchRepositoryImpl
import com.example.starter.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    companion object {
        @Provides
        @Singleton
        fun provideFakeAssetServer(): FakeAssetServer = FakeAssetServer()
    }
}
