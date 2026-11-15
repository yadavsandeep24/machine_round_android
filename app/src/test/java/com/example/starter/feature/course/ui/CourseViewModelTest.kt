package com.example.starter.feature.course.ui

import app.cash.turbine.test
import com.example.starter.core.ui.UiText
import com.example.starter.feature.course.data.download.DownloadRepositoryImpl
import com.example.starter.feature.course.domain.Course
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson
import com.example.starter.testutil.FakeCourseRepository
import com.example.starter.testutil.FakeNetworkMonitor
import com.example.starter.testutil.InMemoryDownloadStore
import com.example.starter.testutil.MainDispatcherRule
import com.example.starter.testutil.RecordingScheduler
import com.example.starter.testutil.TempDirLessonFileStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CourseViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule val tmp = TemporaryFolder()

    private val lessons = (1..4).map { Lesson(it, 1, "Lesson $it", 5, it * 1_048_576L, "fake://video/$it") }
    private val courseRepo = FakeCourseRepository(Course(1, "Coroutines", lessons))
    private val store = InMemoryDownloadStore()
    private val scheduler = RecordingScheduler()
    private val network = FakeNetworkMonitor()

    private fun vm() = CourseViewModel(courseRepo, DownloadRepositoryImpl(store, scheduler, TempDirLessonFileStore(tmp.root)), network)

    @Test
    fun `shows every lesson with its download state`() = runTest(mainRule.dispatcher) {
        store.set(2, DownloadStatus.Downloaded("/x"))
        val vm = vm()
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(rows).hasSize(4)
            assertThat(rows.first { it.lesson.id == 2 }.status).isInstanceOf(DownloadStatus.Downloaded::class.java)
            assertThat(downloadedCount).isEqualTo(1)
            assertThat(downloadedBytes).isEqualTo(2 * 1_048_576L)
        }
    }

    @Test
    fun `download intent queues through the repository`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(CourseIntent.Download(3))
        advanceUntilIdle()
        assertThat(scheduler.scheduled).containsExactly(3)
        assertThat(vm.state.value.rows.first { it.lesson.id == 3 }.status).isEqualTo(DownloadStatus.Queued)
    }

    @Test
    fun `going offline shows only downloaded lessons`() = runTest(mainRule.dispatcher) {
        store.set(1, DownloadStatus.Downloaded("/a"))
        val vm = vm()
        advanceUntilIdle()
        network.online.value = false
        advanceUntilIdle()
        with(vm.state.value) {
            assertThat(isOnline).isFalse()
            assertThat(showingDownloadedOnly).isTrue()
            assertThat(rows.map { it.lesson.id }).containsExactly(1)
        }
    }

    @Test
    fun `refresh failure with a cached outline keeps the list and shows a message`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle() // first load caches the outline
        courseRepo.shouldFail = true
        vm.effects.test {
            vm.onIntent(CourseIntent.Retry)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(CourseEffect.ShowMessage(UiText.Dynamic("offline")))
        }
        assertThat(vm.state.value.rows).hasSize(4)
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `first load failure with no cache shows a full-screen error`() = runTest(mainRule.dispatcher) {
        courseRepo.shouldFail = true
        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("offline"))
    }
}
