package com.example.starter.feature.course.data.download

import com.example.starter.feature.course.domain.DownloadStatus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * The worker's logic as a plain class, so it's unit-testable without WorkManager.
 * The worker just maps [Outcome] to Result.success/retry/failure.
 */
class LessonDownloadProcessor @Inject constructor(
    private val downloader: LessonDownloader,
    private val store: DownloadStore,
    private val files: LessonFileStore
) {
    enum class Outcome { SUCCESS, RETRY, FAILED }

    suspend fun run(lessonId: Int, url: String, attempt: Int, onProgress: suspend (Int) -> Unit = {}): Outcome {
        val target = files.fileFor(lessonId)
        store.set(lessonId, DownloadStatus.Downloading(0))
        return try {
            var lastPercent = 0
            downloader.download(url, target) { downloaded, total ->
                val percent = (downloaded * FULL / total).toInt()
                // Throttle: one DB write per percent, not per chunk.
                if (percent > lastPercent) {
                    lastPercent = percent
                    store.set(lessonId, DownloadStatus.Downloading(percent))
                    onProgress(percent)
                }
            }
            store.set(lessonId, DownloadStatus.Downloaded(target.path))
            Outcome.SUCCESS
        } catch (e: CancellationException) {
            files.delete(lessonId) // never leave a half-written file behind
            throw e
        } catch (e: IOException) {
            files.delete(lessonId)
            if (attempt + 1 < MAX_ATTEMPTS) {
                store.set(lessonId, DownloadStatus.Queued) // WorkManager retries with backoff
                Outcome.RETRY
            } else {
                store.set(lessonId, DownloadStatus.Failed(e.message ?: "Download failed"))
                Outcome.FAILED
            }
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        private const val FULL = 100L
    }
}
