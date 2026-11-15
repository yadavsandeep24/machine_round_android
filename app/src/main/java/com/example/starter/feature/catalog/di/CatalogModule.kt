package com.example.starter.feature.catalog.di

import com.example.starter.feature.catalog.data.CatalogRepositoryImpl
import com.example.starter.feature.catalog.data.FakeCatalogServer
import com.example.starter.feature.catalog.data.InMemoryCartRepository
import com.example.starter.feature.catalog.domain.CartRepository
import com.example.starter.feature.catalog.domain.CatalogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    abstract fun bindCartRepository(impl: InMemoryCartRepository): CartRepository

    companion object {
        @Provides
        @Singleton
        fun provideFakeCatalogServer(): FakeCatalogServer = FakeCatalogServer()
    }
}
