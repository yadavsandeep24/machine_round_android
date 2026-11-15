package com.example.starter.feature.comments.ui

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText
import com.example.starter.feature.comments.domain.CommentRow
import com.example.starter.feature.comments.domain.Preference

data class CommentThreadState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val preference: Preference = Preference.SHOW_ALL,
    /** null = follow-up rule off. */
    val scoreThreshold: Int? = null,
    val rows: List<CommentRow> = emptyList(),
    val hiddenCount: Int = 0
) : MviState

sealed interface CommentThreadIntent : UiIntent {
    data object Retry : CommentThreadIntent
    data class SetPreference(val preference: Preference) : CommentThreadIntent
    data class SetScoreThreshold(val threshold: Int?) : CommentThreadIntent
    data class ToggleCollapse(val commentId: Int) : CommentThreadIntent
}

/** No one-off effects on this screen. */
sealed interface CommentThreadEffect : UiEffect
