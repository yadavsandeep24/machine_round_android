package com.example.starter.feature.organizations.di

import com.example.starter.feature.organizations.data.OrganizationRepositoryImpl
import com.example.starter.feature.organizations.data.remote.NonprofitApi
import com.example.starter.feature.organizations.domain.OrganizationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class OrganizationsModule {
    @Binds
    abstract fun bindRepository(impl: OrganizationRepositoryImpl): OrganizationRepository

    companion object {
        /** The feature owns its base URL; :core only supplies the shared OkHttp/Json stack. */
        @Provides
        @Singleton
        fun provideNonprofitApi(builder: Retrofit.Builder): NonprofitApi = builder
            .baseUrl(NonprofitApi.BASE_URL)
            .build()
            .create(NonprofitApi::class.java)
    }
}
