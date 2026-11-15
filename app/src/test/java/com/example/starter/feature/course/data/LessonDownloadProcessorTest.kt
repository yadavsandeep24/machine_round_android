package com.example.starter.feature.course.data

import com.example.starter.feature.course.data.download.LessonDownloadProcessor
import com.example.starter.feature.course.data.download.LessonDownloadProcessor.Outcome
import com.example.starter.feature.course.data.download.LessonDownloader
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.testutil.InMemoryDownloadStore
import com.example.starter.testutil.TempDirLessonFileStore
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LessonDownloadProcessorTest {

    @get:Rule val tmp = TemporaryFolder()

    private val store = InMemoryDownloadStore()

    /** Writes [total] bytes in [chunks] chunks, optionally failing halfway. */
    private class ScriptedDownloader(private val total: Long, private val chunks: Int, private val failHalfway: Boolean = false) :
        LessonDownloader {
        override suspend fun download(url: String, target: File, onProgress: suspend (Long, Long) -> Unit) {
            target.outputStream().use { out ->
                val step = total / chunks
                for (i in 1..chunks) {
                    if (failHalfway && i > chunks / 2) throw IOException("Connection reset")
                    out.write(ByteArray(step.toInt()))
                    onProgress(step * i, total)
                }
            }
        }
    }

    private fun processor(downloader: LessonDownloader) = LessonDownloadProcessor(downloader, store, TempDirLessonFileStore(tmp.root))

    @Test
    fun `success writes the file, reports throttled monotonic progress, ends Downloaded`() = runTest {
        val progress = mutableListOf<Int>()
        // 1000 chunks -> ~100 distinct percents, not 1000 writes.
        val outcome = processor(ScriptedDownloader(total = 100_000, chunks = 1_000)).run(7, "u", attempt = 0) { progress += it }

        assertThat(outcome).isEqualTo(Outcome.SUCCESS)
        assertThat(progress).isInStrictOrder()
        assertThat(progress.last()).isEqualTo(100)
        assertThat(progress.size).isAtMost(100)
        val final = store.state.value[7] as DownloadStatus.Downloaded
        assertThat(File(final.filePath).length()).isEqualTo(100_000)
    }

    @Test
    fun `failure before the last attempt deletes the partial file and asks to retry`() = runTest {
        val outcome = processor(ScriptedDownloader(10_000, 10, failHalfway = true)).run(7, "u", attempt = 0)
        assertThat(outcome).isEqualTo(Outcome.RETRY)
        assertThat(store.state.value[7]).isEqualTo(DownloadStatus.Queued)
        assertThat(File(tmp.root, "7.mp4").exists()).isFalse()
    }

    @Test
    fun `failure on the last attempt marks Failed`() = runTest {
        val outcome = processor(ScriptedDownloader(10_000, 10, failHalfway = true))
            .run(7, "u", attempt = LessonDownloadProcessor.MAX_ATTEMPTS - 1)
        assertThat(outcome).isEqualTo(Outcome.FAILED)
        assertThat(store.state.value[7]).isEqualTo(DownloadStatus.Failed("Connection reset"))
    }
}
