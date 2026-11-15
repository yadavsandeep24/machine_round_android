package com.example.starter.feature.attendance.data.sync

import com.example.starter.feature.attendance.data.FakeErpServer
import com.example.starter.feature.attendance.data.FakeErpServer.UploadResult
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Drains the outbox in batches. Plain class, so it's unit-testable without WorkManager.
 *
 * Guarantees:
 *  - Idempotent: each upload carries requestId = key + version, so a retry after a lost response
 *    doesn't double-apply.
 *  - Edits made while a batch is in flight are not lost: an ack only marks the version that was sent.
 *  - Conflicts: last-writer-wins by updatedAt; if the server is newer we adopt its value and flag it.
 */
class AttendanceSyncer @Inject constructor(private val store: AttendanceStore, private val server: FakeErpServer) {
    enum class Outcome { DONE, RETRY }

    suspend fun drain(): Outcome {
        while (true) {
            val batch = store.pending(BATCH_SIZE)
            if (batch.isEmpty()) return Outcome.DONE
            val results = try {
                server.submit(batch.map { FakeErpServer.Upload(it.key, it.present, it.updatedAt, it.requestId) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                batch.forEach { sent ->
                    val current = store.get(sent.classId, sent.date, sent.studentId) ?: sent
                    store.upsert(current.copy(attempts = current.attempts + 1, lastError = e.message ?: "Sync failed"))
                }
                return Outcome.RETRY
            }
            val sentByKey = batch.associateBy { it.key }
            results.forEach { result -> apply(result, sentByKey.getValue(result.key)) }
        }
    }

    private suspend fun apply(result: UploadResult, sent: com.example.starter.feature.attendance.domain.AttendanceRecord) {
        val current = store.get(sent.classId, sent.date, sent.studentId) ?: return
        val editedSince = current.version != sent.version
        val updated = when (result) {
            is UploadResult.Accepted -> current.copy(syncedVersion = sent.version, attempts = 0, lastError = null)
            // A newer local edit stays pending and will be compared again on the next pass.
            is UploadResult.Conflict -> if (editedSince) {
                current.copy(syncedVersion = sent.version, lastError = null)
            } else {
                current.copy(
                    present = result.serverPresent,
                    updatedAt = result.serverUpdatedAt,
                    syncedVersion = current.version,
                    attempts = 0,
                    lastError = null,
                    overriddenByServer = true
                )
            }
        }
        store.upsert(updated)
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}
