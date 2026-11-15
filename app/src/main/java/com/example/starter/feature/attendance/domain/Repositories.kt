package com.example.starter.feature.attendance.domain

import com.example.starter.core.common.AppResult
import kotlinx.coroutines.flow.Flow

interface RosterRepository {
    fun observeRoster(classId: Int): Flow<List<Student>>
    suspend fun refresh(classId: Int): AppResult<Unit>
}

interface AttendanceRepository {
    fun observe(classId: Int, date: String): Flow<Map<Int, AttendanceRecord>>

    /** Writes locally (instant, works offline) and asks for a background sync. */
    suspend fun mark(classId: Int, date: String, studentId: Int, present: Boolean)
    fun requestSync()
}
