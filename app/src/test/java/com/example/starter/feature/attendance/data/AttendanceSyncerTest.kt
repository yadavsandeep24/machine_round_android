package com.example.starter.feature.attendance.data

import com.example.starter.core.common.Clock
import com.example.starter.feature.attendance.data.sync.AttendanceSyncer
import com.example.starter.feature.attendance.data.sync.AttendanceSyncer.Outcome
import com.example.starter.feature.attendance.domain.SyncState
import com.example.starter.testutil.CountingSyncScheduler
import com.example.starter.testutil.InMemoryAttendanceStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AttendanceSyncerTest {

    private var now = 1_000L
    private val store = InMemoryAttendanceStore()
    private val server = FakeErpServer(latencyMs = 0)
    private val scheduler = CountingSyncScheduler()
    private val repo = AttendanceRepositoryImpl(store, scheduler, Clock { now })
    private val syncer = AttendanceSyncer(store, server)

    private suspend fun mark(studentId: Int, present: Boolean) {
        now += 1_000
        repo.mark(8, "2026-11-15", studentId, present)
    }

    private suspend fun state(studentId: Int) = store.get(8, "2026-11-15", studentId)!!

    @Test
    fun `marks go to the outbox, sync drains it`() = runTest {
        mark(1, true)
        mark(2, false)
        assertThat(state(1).syncState).isEqualTo(SyncState.PENDING)
        assertThat(scheduler.requests).isEqualTo(2)

        assertThat(syncer.drain()).isEqualTo(Outcome.DONE)
        assertThat(state(1).syncState).isEqualTo(SyncState.SYNCED)
        assertThat(server.records.getValue("8:2026-11-15:2").present).isFalse()
    }

    @Test
    fun `network failure keeps records pending with attempts and error, retry succeeds`() = runTest {
        mark(1, true)
        server.failuresRemaining = 1
        assertThat(syncer.drain()).isEqualTo(Outcome.RETRY)
        with(state(1)) {
            assertThat(syncState).isEqualTo(SyncState.FAILED)
            assertThat(attempts).isEqualTo(1)
        }
        assertThat(syncer.drain()).isEqualTo(Outcome.DONE)
        assertThat(state(1).syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `retrying an already-applied upload does not write twice`() = runTest {
        mark(1, true)
        val sent = state(1)
        server.submit(listOf(FakeErpServer.Upload(sent.key, sent.present, sent.updatedAt, sent.requestId))) // response "lost"
        syncer.drain() // same requestId again
        assertThat(server.writes).isEqualTo(1)
        assertThat(state(1).syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `no-op edits create no outbox work`() = runTest {
        mark(1, true)
        syncer.drain()
        mark(1, true)
        assertThat(state(1).syncState).isEqualTo(SyncState.SYNCED)
        assertThat(scheduler.requests).isEqualTo(1)
    }

    @Test
    fun `newer edit on the web portal wins and is flagged`() = runTest {
        mark(1, true) // teacher, offline, at t=2000
        server.simulateWebEdit("8:2026-11-15:1", present = false, at = 5_000) // office corrects it later
        syncer.drain()
        with(state(1)) {
            assertThat(present).isFalse()
            assertThat(overriddenByServer).isTrue()
            assertThat(syncState).isEqualTo(SyncState.SYNCED)
        }
    }

    @Test
    fun `an edit made while its upload is in flight is not lost`() = runTest {
        mark(1, true)
        val inFlight = state(1)
        // Simulate: batch sent with v1, teacher flips to absent (v2) before the ack arrives.
        server.submit(listOf(FakeErpServer.Upload(inFlight.key, inFlight.present, inFlight.updatedAt, inFlight.requestId)))
        mark(1, false)
        syncer.drain()
        assertThat(server.records.getValue(inFlight.key).present).isFalse()
        assertThat(state(1).syncState).isEqualTo(SyncState.SYNCED)
        assertThat(state(1).version).isEqualTo(2)
    }
}
