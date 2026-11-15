package com.example.starter.feature.course.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.starter.feature.course.domain.Lesson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

class WorkManagerDownloadScheduler @Inject constructor(@ApplicationContext private val context: Context) : DownloadScheduler {

    override fun schedule(lesson: Lesson) {
        val request = OneTimeWorkRequestBuilder<LessonDownloadWorker>()
            .setInputData(workDataOf(LessonDownloadWorker.KEY_LESSON_ID to lesson.id, LessonDownloadWorker.KEY_URL to lesson.url))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // waits for network instead of failing
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        // Unique per lesson + KEEP: tapping Download twice never starts two transfers.
        WorkManager.getInstance(context).enqueueUniqueWork(workName(lesson.id), ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel(lessonId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(lessonId))
    }

    private fun workName(lessonId: Int) = "lesson-download-$lessonId"

    private companion object {
        const val TAG = "lesson-download"
        const val BACKOFF_SECONDS = 10L
    }
}

class AppFilesLessonStore @Inject constructor(@ApplicationContext private val context: Context) : LessonFileStore {
    override fun fileFor(lessonId: Int): File = File(context.filesDir, "lessons/$lessonId.mp4").apply { parentFile?.mkdirs() }

    override fun delete(lessonId: Int) {
        fileFor(lessonId).delete()
    }
}

/**
 * Writes a file of the size encoded in the fake URL, chunk by chunk, so progress, cancellation
 * and retry all behave like a real transfer. `flaky=1` fails the first attempt for each file.
 */
class FakeLessonDownloader @Inject constructor() : LessonDownloader {
    private val failedOnce = mutableSetOf<String>()

    override suspend fun download(url: String, target: File, onProgress: suspend (Long, Long) -> Unit) {
        val total = Regex("size=(\\d+)").find(url)?.groupValues?.get(1)?.toLong() ?: DEFAULT_SIZE
        val flaky = "flaky=1" in url
        val chunk = ByteArray(CHUNK_SIZE)
        var written = 0L
        target.outputStream().buffered().use { out ->
            while (written < total) {
                coroutineContext.ensureActive() // cooperative cancellation between chunks
                if (flaky && written > total / 2 && failedOnce.add(url)) throw IOException("Connection reset")
                val n = minOf(CHUNK_SIZE.toLong(), total - written).toInt()
                out.write(chunk, 0, n)
                written += n
                onProgress(written, total)
                delay(CHUNK_DELAY_MS)
            }
        }
    }

    private companion object {
        const val CHUNK_SIZE = 64 * 1024
        const val CHUNK_DELAY_MS = 40L
        const val DEFAULT_SIZE = 1_048_576L
    }
}
