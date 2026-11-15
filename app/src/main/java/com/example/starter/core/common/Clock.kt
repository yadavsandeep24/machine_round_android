package com.example.starter.core.common

/** Injectable wall clock, so time-based logic is testable. */
fun interface Clock {
    fun nowMillis(): Long
}
