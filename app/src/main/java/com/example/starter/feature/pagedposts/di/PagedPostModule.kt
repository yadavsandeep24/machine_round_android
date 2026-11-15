package com.example.starter.feature.pagedposts.di

import com.example.starter.feature.pagedposts.data.PagedPostRepositoryImpl
import com.example.starter.feature.pagedposts.domain.PagedPostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PagedPostModule {
    @Binds
    abstract fun bindPagedPostRepository(impl: PagedPostRepositoryImpl): PagedPostRepository
}
