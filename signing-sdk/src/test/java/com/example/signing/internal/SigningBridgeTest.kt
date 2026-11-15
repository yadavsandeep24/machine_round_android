package com.example.signing.internal

import com.example.signing.SigningResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SigningBridgeTest {

    @Test
    fun `parses known messages and ignores malformed or unknown ones`() {
        assertThat(BridgeMessageParser.parse("""{"type":"signing_complete"}""")).isEqualTo(BridgeEvent.Completed)
        assertThat(BridgeMessageParser.parse("""{"type":"declined","reason":""}""")).isEqualTo(BridgeEvent.Declined("No reason given"))
        assertThat(
            BridgeMessageParser.parse("""{"type":"declined","reason":"Wrong amount","extra":1}""")
        ).isEqualTo(BridgeEvent.Declined("Wrong amount"))
        assertThat(BridgeMessageParser.parse("""{"type":"format_disk"}""")).isNull()
        assertThat(BridgeMessageParser.parse("<script>")).isNull()
        assertThat(BridgeMessageParser.parse(null)).isNull()
    }

    @Test
    fun `gate delivers exactly one terminal result`() {
        val gate = SigningResultGate("env-1")
        assertThat(gate.onEvent(BridgeEvent.PageViewed)).isNull() // progress isn't terminal
        assertThat(gate.onEvent(BridgeEvent.Completed)).isEqualTo(SigningResult.Completed("env-1"))
        assertThat(gate.onEvent(BridgeEvent.Completed)).isNull() // duplicate post
        assertThat(gate.onEvent(BridgeEvent.Cancelled)).isNull() // back press after completing
        assertThat(gate.fail("late error")).isNull()
    }

    @Test
    fun `gate restored after recreation stays delivered`() {
        val restored = SigningResultGate("env-1", delivered = true)
        assertThat(restored.onEvent(BridgeEvent.Completed)).isNull()
    }

    @Test
    fun `origin policy allows only https on allow-listed hosts`() {
        val policy = OriginPolicy.DEFAULT
        assertThat(policy.isAllowed("https://appassets.androidplatform.net/assets/docusign_sdk/signing.html?x=1")).isTrue()
        assertThat(policy.isAllowed("http://appassets.androidplatform.net/assets/x.html")).isFalse()
        assertThat(policy.isAllowed("https://evil.example.com/?next=https://appassets.androidplatform.net")).isFalse()
        assertThat(policy.isAllowed("https://appassets.androidplatform.net@evil.example.com/")).isFalse()
        assertThat(policy.isAllowed("file:///android_asset/docusign_sdk/signing.html")).isFalse()
        assertThat(policy.isAllowed("javascript:alert(1)")).isFalse()
        assertThat(policy.origins).containsExactly("https://appassets.androidplatform.net")
    }
}
