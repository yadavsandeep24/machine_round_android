package com.example.starter.feature.attendance.ui

import androidx.lifecycle.viewModelScope
import com.example.starter.R
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.Clock
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.core.ui.UiText
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.attendance.domain.AttendanceRepository
import com.example.starter.feature.attendance.domain.RosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Marking is local-first: every tap writes to Room instantly (works offline), and the outbox
 * syncs in the background. The UI never waits for the network.
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val roster: RosterRepository,
    private val attendance: AttendanceRepository,
    network: NetworkMonitor,
    clock: Clock
) : MviViewModel<AttendanceIntent, AttendanceState, AttendanceEffect>(AttendanceState()) {

    // java.time needs API 26 (or desugaring); minSdk is 24, so SimpleDateFormat it is.
    private val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clock.nowMillis()))

    init {
        refreshRoster()
        viewModelScope.launch {
            combine(roster.observeRoster(CLASS_ID), attendance.observe(CLASS_ID, date), network.isOnline) { students, records, online ->
                Triple(students.map { StudentRow(it, records[it.id]) }, online, students.isNotEmpty())
            }.collect { (rows, online, hasRoster) ->
                setState { copy(rows = rows, isOnline = online, date = date, isLoading = isLoading && !hasRoster) }
            }
        }
    }

    override fun handleIntent(intent: AttendanceIntent) {
        when (intent) {
            AttendanceIntent.Retry -> refreshRoster()
            is AttendanceIntent.SetPresent -> viewModelScope.launch { attendance.mark(CLASS_ID, date, intent.studentId, intent.present) }
            AttendanceIntent.MarkAllPresent -> viewModelScope.launch {
                currentState.rows.filter { it.present == null }.forEach { attendance.mark(CLASS_ID, date, it.student.id, present = true) }
            }
            AttendanceIntent.SyncNow -> {
                attendance.requestSync()
                if (!currentState.isOnline) sendEffect(AttendanceEffect.ShowMessage(UiText.Res(R.string.attendance_sync_when_online)))
            }
        }
    }

    private fun refreshRoster() {
        viewModelScope.launch {
            setState { copy(error = null) }
            when (val result = roster.refresh(CLASS_ID)) {
                is AppResult.Success -> setState { copy(isLoading = false) }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (currentState.rows.isNotEmpty()) {
                        setState { copy(isLoading = false) }
                        sendEffect(AttendanceEffect.ShowMessage(msg))
                    } else {
                        setState { copy(isLoading = false, error = msg) }
                    }
                }
            }
        }
    }

    private companion object {
        const val CLASS_ID = 8
    }
}
