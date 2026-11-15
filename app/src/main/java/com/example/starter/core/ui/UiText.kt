package com.example.starter.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text produced in a ViewModel without needing a Context.
 * Resolved to a String only in the UI, so it follows locale / config changes.
 */
@Suppress("SpreadOperator") // args lists are tiny; the copy is irrelevant
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Res -> stringResource(id, *args.toTypedArray())
    }

    /** For non-composable places, e.g. inside LaunchedEffect when showing a snackbar. */
    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Res -> context.getString(id, *args.toTypedArray())
    }
}
