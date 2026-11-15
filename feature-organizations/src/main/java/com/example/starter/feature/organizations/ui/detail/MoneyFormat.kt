package com.example.starter.feature.organizations.ui.detail

import java.util.Locale
import kotlin.math.abs

/** 6_534_759_844 -> "$6.5B". Null (not reported) -> an em dash, never "$0". */
fun formatUsdCompact(amount: Long?): String {
    amount ?: return "\u2014"
    val sign = if (amount < 0) "-" else ""
    val value = abs(amount.toDouble())
    val (scaled, suffix) = when {
        value >= BILLION -> value / BILLION to "B"
        value >= MILLION -> value / MILLION to "M"
        value >= THOUSAND -> value / THOUSAND to "K"
        else -> return "$sign$${value.toLong()}"
    }
    return "$sign$" + String.format(Locale.US, "%.1f", scaled).removeSuffix(".0") + suffix
}

/** 941340523 -> "94-1340523" (the IRS display format). */
fun formatEin(ein: Long): String = ein.toString().padStart(EIN_DIGITS, '0').let { "${it.take(2)}-${it.drop(2)}" }

private const val THOUSAND = 1_000.0
private const val MILLION = 1_000_000.0
private const val BILLION = 1_000_000_000.0
private const val EIN_DIGITS = 9
