package com.example.starter.feature.course.ui

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson

data class LessonRow(val lesson: Lesson, val status: DownloadStatus)

data class CourseState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val courseTitle: String = "",
    val rows: List<LessonRow> = emptyList(),
    val isOnline: Boolean = true,
    val downloadedOnly: Boolean = false,
    val downloadedCount: Int = 0,
    val downloadedBytes: Long = 0
) : MviState {
    /** Offline forces the downloaded-only view: nothing else is playable. */
    val showingDownloadedOnly: Boolean get() = downloadedOnly || !isOnline
}

sealed interface CourseIntent : UiIntent {
    data object Retry : CourseIntent
    data class Download(val lessonId: Int) : CourseIntent
    data class Cancel(val lessonId: Int) : CourseIntent
    data class Delete(val lessonId: Int) : CourseIntent
    data object ToggleDownloadedOnly : CourseIntent
}

sealed interface CourseEffect : UiEffect {
    data class ShowMessage(val message: UiText) : CourseEffect
}
