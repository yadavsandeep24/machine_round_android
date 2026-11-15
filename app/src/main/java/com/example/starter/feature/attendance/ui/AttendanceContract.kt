package com.example.starter.feature.attendance.ui

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText
import com.example.starter.feature.attendance.domain.AttendanceRecord
import com.example.starter.feature.attendance.domain.Student
import com.example.starter.feature.attendance.domain.SyncState

data class StudentRow(val student: Student, val record: AttendanceRecord?) {
    val present: Boolean? get() = record?.present
    val syncState: SyncState? get() = record?.syncState
}

data class AttendanceState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val date: String = "",
    val rows: List<StudentRow> = emptyList(),
    val isOnline: Boolean = true
) : MviState {
    val presentCount: Int get() = rows.count { it.present == true }
    val absentCount: Int get() = rows.count { it.present == false }
    val unmarkedCount: Int get() = rows.count { it.present == null }
    val pendingCount: Int get() = rows.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.FAILED }
}

sealed interface AttendanceIntent : UiIntent {
    data object Retry : AttendanceIntent
    data class SetPresent(val studentId: Int, val present: Boolean) : AttendanceIntent
    data object MarkAllPresent : AttendanceIntent
    data object SyncNow : AttendanceIntent
}

sealed interface AttendanceEffect : UiEffect {
    data class ShowMessage(val message: UiText) : AttendanceEffect
}
