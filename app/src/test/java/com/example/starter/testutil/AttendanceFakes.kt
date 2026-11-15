package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.feature.attendance.data.sync.AttendanceStore
import com.example.starter.feature.attendance.data.sync.SyncScheduler
import com.example.starter.feature.attendance.domain.AttendanceRecord
import com.example.starter.feature.attendance.domain.RosterRepository
import com.example.starter.feature.attendance.domain.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryAttendanceStore : AttendanceStore {
    val rows = MutableStateFlow<Map<String, AttendanceRecord>>(emptyMap())
    override fun observe(classId: Int, date: String): Flow<List<AttendanceRecord>> =
        rows.map { all -> all.values.filter { it.classId == classId && it.date == date } }
    override suspend fun get(classId: Int, date: String, studentId: Int): AttendanceRecord? = rows.value["$classId:$date:$studentId"]
    override suspend fun upsert(record: AttendanceRecord) = rows.update { it + (record.key to record) }
    override suspend fun pending(limit: Int): List<AttendanceRecord> =
        rows.value.values.filter { it.version > it.syncedVersion }.sortedBy { it.updatedAt }.take(limit)
}

class CountingSyncScheduler : SyncScheduler {
    var requests = 0
    override fun requestSync() {
        requests++
    }
}

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}

class FakeRosterRepository(private val students: List<Student>) : RosterRepository {
    private val cached = MutableStateFlow<List<Student>>(emptyList())
    var shouldFail = false
    override fun observeRoster(classId: Int): Flow<List<Student>> = cached
    override suspend fun refresh(classId: Int): AppResult<Unit> {
        if (shouldFail) return AppResult.Failure(java.io.IOException("offline"))
        cached.value = students
        return AppResult.Success(Unit)
    }
}
