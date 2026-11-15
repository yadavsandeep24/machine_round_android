package com.example.signing.internal

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.signing.SigningResult

/**
 * Hosts the online signing ceremony in a hardened WebView.
 *
 * Security choices (interview talking points):
 *  - addWebMessageListener with an origin allow-list instead of addJavascriptInterface, which
 *    exposes the bridge to EVERY frame and origin, including injected iframes.
 *  - Navigation to non-allow-listed URLs is blocked; no file:// or content:// access; no mixed content.
 *  - Local assets are served via WebViewAssetLoader on an https origin, never file://.
 *  - Results go through [SigningResultGate], so the host receives exactly one result.
 */
internal class SigningActivity : ComponentActivity() {

    private var webView: WebView? = null
    private lateinit var gate: SigningResultGate
    private val policy = OriginPolicy.DEFAULT

    @SuppressLint("SetJavaScriptEnabled") // required by the ceremony; mitigated by the origin rules below
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL)
        val envelopeId = intent.getStringExtra(EXTRA_ENVELOPE_ID)
        if (url == null || envelopeId == null) {
            finishWith(SigningResult.Cancelled(envelopeId))
            return
        }
        gate = SigningResultGate(envelopeId, delivered = savedInstanceState?.getBoolean(KEY_DELIVERED) ?: false)
        when {
            !policy.isAllowed(url) -> gate.fail("Blocked signing URL")?.let(::finishWith)
            !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ->
                gate.fail("Please update Android System WebView to sign")?.let(::finishWith)
            else -> setUpWebView(url, savedInstanceState)
        }
        onBackPressedDispatcher.addCallback(this) {
            gate.onEvent(BridgeEvent.Cancelled)?.let(::finishWith) ?: finish()
        }
    }

    private fun setUpWebView(url: String, savedInstanceState: Bundle?) {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        val view = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = HardenedClient(assetLoader, policy)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(view.settings, true)
        }
        WebViewCompat.addWebMessageListener(view, BRIDGE_NAME, policy.origins) { _, message, _, isMainFrame, _ ->
            if (isMainFrame) {
                BridgeMessageParser.parse(message.data)?.let(gate::onEvent)?.let(::finishWith)
            }
        }
        webView = view
        setContentView(view)
        if (savedInstanceState != null) view.restoreState(savedInstanceState) else view.loadUrl(url)
    }

    private fun finishWith(result: SigningResult) {
        setResult(RESULT_OK, ResultCodec.toIntent(result))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::gate.isInitialized) outState.putBoolean(KEY_DELIVERED, gate.delivered)
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            destroy() // WebViews leak the Activity if not destroyed
        }
        webView = null
        super.onDestroy()
    }

    private class HardenedClient(private val assets: WebViewAssetLoader, private val policy: OriginPolicy) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !policy.isAllowed(request.url.toString())

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            assets.shouldInterceptRequest(request.url)
    }

    companion object {
        const val EXTRA_URL = "ds_signing_url"
        const val EXTRA_ENVELOPE_ID = "ds_envelope_id"
        private const val KEY_DELIVERED = "ds_result_delivered"
        private const val BRIDGE_NAME = "DSBridge"
    }
}
