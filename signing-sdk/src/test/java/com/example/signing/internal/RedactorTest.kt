package com.example.signing.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RedactorTest {
    @Test
    fun `masks emails and long numbers`() {
        assertThat(Redactor.redact("Failed for asha.rao@example.com, acct 123456789012"))
            .isEqualTo("Failed for a***@example.com, acct ********9012")
    }
}
