package com.example.starter.feature.attendance.data

import com.example.starter.feature.attendance.domain.Student
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * In-app stand-in for the school ERP backend.
 * Attendance writes are idempotent by requestId and resolved last-writer-wins by updatedAt.
 */
class FakeErpServer(private val latencyMs: Long = DEFAULT_LATENCY_MS) {
    var failuresRemaining = 0

    data class ServerRecord(val present: Boolean, val updatedAt: Long)
    data class Upload(val key: String, val present: Boolean, val updatedAt: Long, val requestId: String)

    sealed interface UploadResult {
        val key: String
        data class Accepted(override val key: String) : UploadResult
        data class Conflict(override val key: String, val serverPresent: Boolean, val serverUpdatedAt: Long) : UploadResult
    }

    val records = mutableMapOf<String, ServerRecord>()
    private val seenRequestIds = mutableSetOf<String>()
    var writes = 0
        private set

    suspend fun roster(classId: Int): List<Student> {
        simulateNetwork()
        return NAMES.mapIndexed { i, name -> Student(id = classId * ID_FACTOR + i + 1, classId = classId, rollNo = i + 1, name = name) }
    }

    suspend fun submit(batch: List<Upload>): List<UploadResult> {
        simulateNetwork()
        return batch.map { upload ->
            val existing = records[upload.key]
            when {
                upload.requestId in seenRequestIds -> UploadResult.Accepted(upload.key) // retry of an applied write
                existing != null && existing.updatedAt > upload.updatedAt ->
                    UploadResult.Conflict(upload.key, existing.present, existing.updatedAt)
                else -> {
                    records[upload.key] = ServerRecord(upload.present, upload.updatedAt)
                    seenRequestIds += upload.requestId
                    writes++
                    UploadResult.Accepted(upload.key)
                }
            }
        }
    }

    /** Test/demo hook: someone edited this record on the web portal. */
    fun simulateWebEdit(key: String, present: Boolean, at: Long) {
        records[key] = ServerRecord(present, at)
    }

    private suspend fun simulateNetwork() {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 400L
        const val ID_FACTOR = 100
        val NAMES = listOf(
            "Aarav Sharma", "Diya Patel", "Ishaan Gupta", "Ananya Iyer", "Kabir Singh", "Meera Nair",
            "Vihaan Reddy", "Saanvi Joshi", "Arjun Mehta", "Kiara Das", "Reyansh Kulkarni", "Aditi Rao"
        )
    }
}
