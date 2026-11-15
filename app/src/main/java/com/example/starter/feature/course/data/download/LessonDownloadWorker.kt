package com.example.starter.feature.course.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Thin adapter: all logic lives in [LessonDownloadProcessor]. */
@HiltWorker
class LessonDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: LessonDownloadProcessor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lessonId = inputData.getInt(KEY_LESSON_ID, -1)
        val url = inputData.getString(KEY_URL)
        if (lessonId < 0 || url == null) return Result.failure()
        return when (processor.run(lessonId, url, runAttemptCount) { setProgress(workDataOf(KEY_PROGRESS to it)) }) {
            LessonDownloadProcessor.Outcome.SUCCESS -> Result.success()
            LessonDownloadProcessor.Outcome.RETRY -> Result.retry()
            LessonDownloadProcessor.Outcome.FAILED -> Result.failure()
        }
    }

    companion object {
        const val KEY_LESSON_ID = "lesson_id"
        const val KEY_URL = "url"
        const val KEY_PROGRESS = "progress"
    }
}
