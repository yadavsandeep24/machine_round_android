package com.example.signing.internal

/** Strips PII before anything leaves the SDK (events, logs, crash reports). */
internal object Redactor {
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val longDigits = Regex("\\d{6,}")

    fun redact(text: String): String = text
        .replace(email) { m -> m.value.first() + "***@" + m.value.substringAfter('@') }
        .replace(longDigits) { m -> "*".repeat(m.value.length - VISIBLE_DIGITS) + m.value.takeLast(VISIBLE_DIGITS) }

    private const val VISIBLE_DIGITS = 4
}
