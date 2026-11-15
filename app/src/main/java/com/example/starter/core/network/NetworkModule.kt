package com.example.starter.core.network

import com.example.starter.BuildConfig
import com.example.starter.core.network.api.PostApi
import com.example.starter.core.network.auth.AuthApi
import com.example.starter.core.network.auth.AuthInterceptor
import com.example.starter.core.network.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber

/** Client without auth: used only for the token refresh call, so the Authenticator can't recurse. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // API adds fields? Don't crash.
        coerceInputValues = true // null for non-null field with default -> use default
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    @Provides
    @Singleton
    @AuthClient
    fun provideAuthOkHttp(logging: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(@AuthClient base: OkHttpClient, authInterceptor: AuthInterceptor, authenticator: TokenAuthenticator): OkHttpClient =
        base.newBuilder() // shares the connection pool and dispatcher
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    fun provideAuthApi(@AuthClient client: OkHttpClient, json: Json): AuthApi = retrofit(client, json).create(AuthApi::class.java)

    // Add one provider per API interface.
    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): PostApi = retrofit.create(PostApi::class.java)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private const val TIMEOUT_SECONDS = 15L
}
