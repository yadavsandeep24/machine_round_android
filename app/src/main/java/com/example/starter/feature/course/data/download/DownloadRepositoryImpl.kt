package com.example.starter.feature.course.data.download

import com.example.starter.feature.course.domain.DownloadRepository
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DownloadRepositoryImpl @Inject constructor(
    private val store: DownloadStore,
    private val scheduler: DownloadScheduler,
    private val files: LessonFileStore
) : DownloadRepository {

    override fun observeDownloads(): Flow<Map<Int, DownloadStatus>> = store.observeAll()

    override suspend fun enqueue(lesson: Lesson) {
        when (store.get(lesson.id)) {
            DownloadStatus.Queued, is DownloadStatus.Downloading, is DownloadStatus.Downloaded -> return // idempotent
            DownloadStatus.NotDownloaded, is DownloadStatus.Failed -> {
                store.set(lesson.id, DownloadStatus.Queued) // UI updates instantly, before the worker starts
                scheduler.schedule(lesson)
            }
        }
    }

    override suspend fun cancel(lessonId: Int) {
        scheduler.cancel(lessonId)
        files.delete(lessonId)
        store.remove(lessonId)
    }

    override suspend fun delete(lessonId: Int) {
        files.delete(lessonId)
        store.remove(lessonId)
    }
}
