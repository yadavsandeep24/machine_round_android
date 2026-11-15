package com.example.starter.feature.course.domain

import com.example.starter.core.common.AppResult
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    /** Cached course (Room). Emits null until something is cached. */
    fun observeCourse(courseId: Int): Flow<Course?>
    suspend fun refresh(courseId: Int): AppResult<Unit>
}

interface DownloadRepository {
    /** lessonId -> status, for lessons that have any download state. */
    fun observeDownloads(): Flow<Map<Int, DownloadStatus>>
    suspend fun enqueue(lesson: Lesson)
    suspend fun cancel(lessonId: Int)
    suspend fun delete(lessonId: Int)
}
