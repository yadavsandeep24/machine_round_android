package com.example.starter.feature.course.data.download

import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson
import java.io.File
import kotlinx.coroutines.flow.Flow

/** Persistence for download state (Room in the app, in-memory in tests). */
interface DownloadStore {
    fun observeAll(): Flow<Map<Int, DownloadStatus>>
    suspend fun get(lessonId: Int): DownloadStatus
    suspend fun set(lessonId: Int, status: DownloadStatus)
    suspend fun remove(lessonId: Int)
}

/** Schedules background work (WorkManager in the app, a recorder in tests). */
interface DownloadScheduler {
    fun schedule(lesson: Lesson)
    fun cancel(lessonId: Int)
}

/** Where downloaded files live. */
interface LessonFileStore {
    fun fileFor(lessonId: Int): File
    fun delete(lessonId: Int)
}

/** Transfers bytes. A real one streams an OkHttp response body; it must be cancellable. */
interface LessonDownloader {
    suspend fun download(url: String, target: File, onProgress: suspend (downloaded: Long, total: Long) -> Unit)
}
