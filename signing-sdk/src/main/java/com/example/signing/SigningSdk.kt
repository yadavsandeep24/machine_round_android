package com.example.signing

import android.content.Context
import com.example.signing.internal.DefaultSigningClient
import com.example.signing.internal.EnvelopeService
import com.example.signing.internal.InMemoryEnvelopeService
import com.example.signing.internal.OfflineStore
import com.example.signing.internal.TokenManager
import java.io.File
import kotlinx.coroutines.Dispatchers

public object SigningSdk {
    public const val VERSION: String = "1.0.0"

    /**
     * No DI framework required by the host: plain builder.
     * Create one [SigningClient] per process (e.g. in the host's DI graph) and reuse it.
     */
    public class Builder(context: Context) {
        private val appContext = context.applicationContext
        private var tokenProvider: TokenProvider? = null
        private var listener = SigningEventListener { }

        // Test/demo seams, deliberately not public API.
        internal var service: EnvelopeService = InMemoryEnvelopeService()
        internal var clock: () -> Long = System::currentTimeMillis

        public fun tokenProvider(provider: TokenProvider): Builder = apply { tokenProvider = provider }

        public fun eventListener(eventListener: SigningEventListener): Builder = apply { listener = eventListener }

        public fun build(): SigningClient {
            val provider = checkNotNull(tokenProvider) { "SigningSdk: tokenProvider(...) is required" }
            // noBackupFilesDir: signed documents must not end up in cloud backups.
            val storeFile = File(appContext.noBackupFilesDir, "docusign_sdk/offline.json")
            return DefaultSigningClient(
                service = service,
                store = OfflineStore(storeFile, Dispatchers.IO),
                tokens = TokenManager(provider, clock, onRefreshed = { listener.onEvent(SigningEvent.TokenRefreshed) }),
                events = listener,
                clock = clock
            )
        }
    }
}
