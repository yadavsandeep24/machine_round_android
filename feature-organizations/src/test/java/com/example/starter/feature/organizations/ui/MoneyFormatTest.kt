package com.example.starter.feature.organizations.ui

import com.example.starter.feature.organizations.ui.detail.formatEin
import com.example.starter.feature.organizations.ui.detail.formatUsdCompact
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun `compact USD`() {
        assertThat(formatUsdCompact(6_534_759_844)).isEqualTo("$6.5B")
        assertThat(formatUsdCompact(28_000_000_000)).isEqualTo("$28B")
        assertThat(formatUsdCompact(1_250_000)).isEqualTo("$1.3M")
        assertThat(formatUsdCompact(9_999)).isEqualTo("$10K")
        assertThat(formatUsdCompact(950)).isEqualTo("$950")
        assertThat(formatUsdCompact(-2_500_000)).isEqualTo("-$2.5M")
        assertThat(formatUsdCompact(null)).isEqualTo("—")
    }

    @Test
    fun `EIN display format, with leading zeros kept`() {
        assertThat(formatEin(941340523)).isEqualTo("94-1340523")
        assertThat(formatEin(12345678)).isEqualTo("01-2345678")
    }
}
