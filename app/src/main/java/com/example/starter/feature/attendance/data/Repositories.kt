package com.example.starter.feature.attendance.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.common.Clock
import com.example.starter.core.network.safeCall
import com.example.starter.feature.attendance.data.local.AttendanceDao
import com.example.starter.feature.attendance.data.local.StudentEntity
import com.example.starter.feature.attendance.data.sync.AttendanceStore
import com.example.starter.feature.attendance.data.sync.SyncScheduler
import com.example.starter.feature.attendance.domain.AttendanceRecord
import com.example.starter.feature.attendance.domain.AttendanceRepository
import com.example.starter.feature.attendance.domain.RosterRepository
import com.example.starter.feature.attendance.domain.Student
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RosterRepositoryImpl @Inject constructor(private val server: FakeErpServer, private val dao: AttendanceDao) : RosterRepository {
    override fun observeRoster(classId: Int): Flow<List<Student>> =
        dao.observeStudents(classId).map { rows -> rows.map { Student(it.id, it.classId, it.rollNo, it.name) } }

    override suspend fun refresh(classId: Int): AppResult<Unit> = safeCall {
        dao.upsertStudents(server.roster(classId).map { StudentEntity(it.id, it.classId, it.rollNo, it.name) })
    }
}

class AttendanceRepositoryImpl @Inject constructor(
    private val store: AttendanceStore,
    private val scheduler: SyncScheduler,
    private val clock: Clock
) : AttendanceRepository {

    override fun observe(classId: Int, date: String): Flow<Map<Int, AttendanceRecord>> =
        store.observe(classId, date).map { list -> list.associateBy { it.studentId } }

    override suspend fun mark(classId: Int, date: String, studentId: Int, present: Boolean) {
        val existing = store.get(classId, date, studentId)
        if (existing?.present == present) return // no-op edits don't create outbox work
        val now = clock.nowMillis()
        val record =
            existing?.copy(present = present, version = existing.version + 1, updatedAt = now, lastError = null, overriddenByServer = false)
                ?: AttendanceRecord(classId, date, studentId, present, version = 1, updatedAt = now)
        store.upsert(record)
        scheduler.requestSync()
    }

    override fun requestSync() = scheduler.requestSync()
}
