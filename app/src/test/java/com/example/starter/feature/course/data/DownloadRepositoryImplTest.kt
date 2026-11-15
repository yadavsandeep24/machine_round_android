package com.example.starter.feature.course.data

import com.example.starter.feature.course.data.download.DownloadRepositoryImpl
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson
import com.example.starter.testutil.InMemoryDownloadStore
import com.example.starter.testutil.RecordingScheduler
import com.example.starter.testutil.TempDirLessonFileStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadRepositoryImplTest {

    @get:Rule val tmp = TemporaryFolder()

    private val store = InMemoryDownloadStore()
    private val scheduler = RecordingScheduler()
    private val lesson = Lesson(7, 1, "L", 5, 1_000, "fake://video/0")
    private fun repo() = DownloadRepositoryImpl(store, scheduler, TempDirLessonFileStore(tmp.root))

    @Test
    fun `enqueue marks Queued immediately and schedules once`() = runTest {
        val repo = repo()
        repo.enqueue(lesson)
        repo.enqueue(lesson) // double tap
        assertThat(store.state.value[7]).isEqualTo(DownloadStatus.Queued)
        assertThat(scheduler.scheduled).containsExactly(7)
    }

    @Test
    fun `failed downloads can be enqueued again, finished ones cannot`() = runTest {
        val repo = repo()
        store.set(7, DownloadStatus.Failed("x"))
        repo.enqueue(lesson)
        store.set(7, DownloadStatus.Downloaded("/f"))
        repo.enqueue(lesson)
        assertThat(scheduler.scheduled).containsExactly(7)
    }

    @Test
    fun `cancel stops the work, deletes the partial file and clears state`() = runTest {
        val repo = repo()
        repo.enqueue(lesson)
        tmp.newFile("7.mp4").writeText("partial")
        repo.cancel(7)
        assertThat(scheduler.cancelled).containsExactly(7)
        assertThat(store.state.value).doesNotContainKey(7)
        assertThat(tmp.root.resolve("7.mp4").exists()).isFalse()
    }
}
