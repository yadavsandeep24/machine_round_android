package com.example.starter.feature.attendance.domain

data class Student(val id: Int, val classId: Int, val rollNo: Int, val name: String)

enum class SyncState { PENDING, SYNCED, FAILED }

/**
 * One row per (class, date, student). Every local edit bumps [version]; the server acknowledges a
 * version, recorded in [syncedVersion]. version > syncedVersion means "still in the outbox".
 */
data class AttendanceRecord(
    val classId: Int,
    val date: String,
    val studentId: Int,
    val present: Boolean,
    val version: Int,
    val syncedVersion: Int = 0,
    val attempts: Int = 0,
    val lastError: String? = null,
    /** When the teacher made this choice (client clock). Used for last-writer-wins. */
    val updatedAt: Long,
    /** True if the server had a newer value (e.g. edited on the web portal) and it replaced ours. */
    val overriddenByServer: Boolean = false
) {
    val key: String get() = "$classId:$date:$studentId"

    /** Deterministic per version: a retried upload of the same edit is recognised and de-duplicated by the server. */
    val requestId: String get() = "$key:v$version"

    val syncState: SyncState
        get() = when {
            version == syncedVersion -> SyncState.SYNCED
            lastError != null -> SyncState.FAILED
            else -> SyncState.PENDING
        }
}
