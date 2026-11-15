package com.example.signing

/**
 * Implemented by the host app. Called (at most once at a time) when the SDK needs a fresh token,
 * typically by asking the host's backend, which holds the integration secret. The SDK never
 * holds long-lived credentials itself.
 */
public fun interface TokenProvider {
    public suspend fun fetchToken(): AccessToken
}

public fun interface SigningEventListener {
    public fun onEvent(event: SigningEvent)
}
