package com.example.starter.feature.attendance.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class AttendanceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: AttendanceSyncer
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (syncer.drain()) {
        AttendanceSyncer.Outcome.DONE -> Result.success()
        AttendanceSyncer.Outcome.RETRY -> Result.retry()
    }
}

class WorkManagerSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) : SyncScheduler {
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: marks made during a running sync get a follow-up run instead of being dropped (KEEP would drop them).
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "attendance-sync"
        const val BACKOFF_SECONDS = 15L
    }
}
