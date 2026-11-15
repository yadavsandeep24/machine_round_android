package com.example.starter.feature.course.domain

data class Lesson(val id: Int, val courseId: Int, val title: String, val durationMinutes: Int, val sizeBytes: Long, val url: String)

data class Course(val id: Int, val title: String, val lessons: List<Lesson>)

sealed interface DownloadStatus {
    data object NotDownloaded : DownloadStatus

    /** Scheduled; may be waiting for network or a retry backoff. */
    data object Queued : DownloadStatus
    data class Downloading(val percent: Int) : DownloadStatus
    data class Downloaded(val filePath: String) : DownloadStatus
    data class Failed(val reason: String) : DownloadStatus
}
