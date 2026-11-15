package com.example.starter.feature.envelopes.di

import android.content.Context
import com.example.signing.AccessToken
import com.example.signing.SigningClient
import com.example.signing.SigningSdk
import com.example.signing.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton
import kotlinx.coroutines.delay
import timber.log.Timber

/** The host app owns DI; the SDK is just a builder call. */
@Module
@InstallIn(SingletonComponent::class)
object SigningModule {

    @Provides
    @Singleton
    fun provideSigningClient(@ApplicationContext context: Context): SigningClient = SigningSdk.Builder(context)
        .tokenProvider(DemoTokenProvider())
        .eventListener { event -> Timber.tag("SigningSdk").d("event: %s", event) } // events carry no PII
        .build()
}

/**
 * Stands in for "ask OUR backend for a DocuSign access token" (the backend holds the integration
 * secret; the app never does). Short 2-minute tokens so you can watch refreshes in Logcat.
 */
private class DemoTokenProvider : TokenProvider {
    private val counter = AtomicInteger()

    override suspend fun fetchToken(): AccessToken {
        delay(BACKEND_LATENCY_MS)
        return AccessToken("demo-token-${counter.incrementAndGet()}", System.currentTimeMillis() + TOKEN_LIFETIME_MS)
    }

    private companion object {
        const val BACKEND_LATENCY_MS = 300L
        const val TOKEN_LIFETIME_MS = 2 * 60_000L
    }
}
