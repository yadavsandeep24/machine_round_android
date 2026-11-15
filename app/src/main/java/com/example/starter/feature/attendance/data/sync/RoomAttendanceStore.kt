package com.example.starter.feature.attendance.data.sync

import com.example.starter.feature.attendance.data.local.AttendanceDao
import com.example.starter.feature.attendance.data.local.AttendanceEntity
import com.example.starter.feature.attendance.domain.AttendanceRecord
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAttendanceStore @Inject constructor(private val dao: AttendanceDao) : AttendanceStore {
    override fun observe(classId: Int, date: String): Flow<List<AttendanceRecord>> =
        dao.observeAttendance(classId, date).map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(classId: Int, date: String, studentId: Int): AttendanceRecord? = dao.get(classId, date, studentId)?.toDomain()

    override suspend fun upsert(record: AttendanceRecord) = dao.upsert(record.toEntity())

    override suspend fun pending(limit: Int): List<AttendanceRecord> = dao.pending(limit).map { it.toDomain() }

    private fun AttendanceEntity.toDomain() =
        AttendanceRecord(classId, date, studentId, present, version, syncedVersion, attempts, lastError, updatedAt, overriddenByServer)

    private fun AttendanceRecord.toEntity() =
        AttendanceEntity(classId, date, studentId, present, version, syncedVersion, attempts, lastError, updatedAt, overriddenByServer)
}
