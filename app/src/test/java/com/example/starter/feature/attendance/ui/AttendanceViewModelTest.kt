package com.example.starter.feature.attendance.ui

import app.cash.turbine.test
import com.example.starter.R
import com.example.starter.core.common.Clock
import com.example.starter.core.ui.UiText
import com.example.starter.feature.attendance.data.AttendanceRepositoryImpl
import com.example.starter.feature.attendance.domain.Student
import com.example.starter.feature.attendance.domain.SyncState
import com.example.starter.testutil.CountingSyncScheduler
import com.example.starter.testutil.FakeNetworkMonitor
import com.example.starter.testutil.FakeRosterRepository
import com.example.starter.testutil.InMemoryAttendanceStore
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val students = (1..4).map { Student(it, 8, it, "Student $it") }
    private val roster = FakeRosterRepository(students)
    private val store = InMemoryAttendanceStore()
    private val scheduler = CountingSyncScheduler()
    private val network = FakeNetworkMonitor()
    private val clock = Clock { 1_763_164_800_000L } // 2025-11-15 UTC

    private fun vm() = AttendanceViewModel(roster, AttendanceRepositoryImpl(store, scheduler, clock), network, clock)

    @Test
    fun `roster loads with everyone unmarked`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(rows).hasSize(4)
            assertThat(unmarkedCount).isEqualTo(4)
            assertThat(pendingCount).isEqualTo(0)
        }
    }

    @Test
    fun `marking is instant and shows as pending sync`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(AttendanceIntent.SetPresent(2, false))
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(absentCount).isEqualTo(1)
            assertThat(rows.first { it.student.id == 2 }.syncState).isEqualTo(SyncState.PENDING)
            assertThat(pendingCount).isEqualTo(1)
        }
        assertThat(scheduler.requests).isEqualTo(1)
    }

    @Test
    fun `mark rest present only touches unmarked students`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(AttendanceIntent.SetPresent(1, false))
        advanceUntilIdle()
        vm.onIntent(AttendanceIntent.MarkAllPresent)
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(presentCount).isEqualTo(3)
            assertThat(absentCount).isEqualTo(1)
            assertThat(unmarkedCount).isEqualTo(0)
        }
    }

    @Test
    fun `sync now while offline explains it will sync later`() = runTest(mainRule.dispatcher) {
        network.online.value = false
        val vm = vm()
        advanceUntilIdle()
        vm.effects.test {
            vm.onIntent(AttendanceIntent.SyncNow)
            assertThat(awaitItem()).isEqualTo(AttendanceEffect.ShowMessage(UiText.Res(R.string.attendance_sync_when_online)))
        }
        assertThat(vm.state.value.isOnline).isFalse()
        assertThat(scheduler.requests).isEqualTo(1) // still queued: WorkManager waits for network
    }

    @Test
    fun `roster failure with no cache shows a full-screen error`() = runTest(mainRule.dispatcher) {
        roster.shouldFail = true
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("offline"))
    }
}
