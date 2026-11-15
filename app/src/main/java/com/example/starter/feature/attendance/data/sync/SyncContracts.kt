package com.example.starter.feature.attendance.data.sync

import com.example.starter.feature.attendance.domain.AttendanceRecord
import kotlinx.coroutines.flow.Flow

/** Outbox persistence (Room in the app, in-memory in tests). */
interface AttendanceStore {
    fun observe(classId: Int, date: String): Flow<List<AttendanceRecord>>
    suspend fun get(classId: Int, date: String, studentId: Int): AttendanceRecord?
    suspend fun upsert(record: AttendanceRecord)
    suspend fun pending(limit: Int): List<AttendanceRecord>
}

interface SyncScheduler {
    fun requestSync()
}
