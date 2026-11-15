package com.example.starter.feature.course.data.download

import com.example.starter.feature.course.data.local.DownloadDao
import com.example.starter.feature.course.data.local.DownloadEntity
import com.example.starter.feature.course.domain.DownloadStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDownloadStore @Inject constructor(private val dao: DownloadDao) : DownloadStore {

    override fun observeAll(): Flow<Map<Int, DownloadStatus>> = dao.observeAll().map { rows ->
        rows.associate { it.lessonId to it.toStatus() }
    }

    override suspend fun get(lessonId: Int): DownloadStatus = dao.get(lessonId)?.toStatus() ?: DownloadStatus.NotDownloaded

    override suspend fun set(lessonId: Int, status: DownloadStatus) {
        val entity = when (status) {
            DownloadStatus.NotDownloaded -> return dao.delete(lessonId)
            DownloadStatus.Queued -> DownloadEntity(lessonId, QUEUED)
            is DownloadStatus.Downloading -> DownloadEntity(lessonId, DOWNLOADING, percent = status.percent)
            is DownloadStatus.Downloaded -> DownloadEntity(lessonId, DOWNLOADED, percent = FULL, filePath = status.filePath)
            is DownloadStatus.Failed -> DownloadEntity(lessonId, FAILED, error = status.reason)
        }
        dao.upsert(entity)
    }

    override suspend fun remove(lessonId: Int) = dao.delete(lessonId)

    private fun DownloadEntity.toStatus(): DownloadStatus = when (status) {
        QUEUED -> DownloadStatus.Queued
        DOWNLOADING -> DownloadStatus.Downloading(percent)
        DOWNLOADED -> DownloadStatus.Downloaded(filePath.orEmpty())
        FAILED -> DownloadStatus.Failed(error.orEmpty())
        else -> DownloadStatus.NotDownloaded
    }

    private companion object {
        const val QUEUED = "queued"
        const val DOWNLOADING = "downloading"
        const val DOWNLOADED = "downloaded"
        const val FAILED = "failed"
        const val FULL = 100
    }
}
