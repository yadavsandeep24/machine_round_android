package com.example.starter.feature.course.data

import com.example.starter.feature.course.domain.Course
import com.example.starter.feature.course.domain.Lesson
import java.io.IOException
import kotlinx.coroutines.delay

/** In-app stand-in for the course API. Lesson URLs use a fake scheme the fake downloader understands. */
class FakeCourseServer(private val latencyMs: Long = DEFAULT_LATENCY_MS) {
    var failuresRemaining = 0

    suspend fun fetchCourse(courseId: Int): Course {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
        val lessons = TITLES.mapIndexed { i, title ->
            val size = (i + 2) * MEGABYTE
            // Lesson 5 is "flaky": its first attempt fails, to show retry with backoff.
            val flaky = if (i == FLAKY_INDEX) "&flaky=1" else ""
            Lesson(
                id = courseId * LESSON_ID_FACTOR + i + 1,
                courseId,
                title,
                durationMinutes = 6 + i * 3,
                sizeBytes = size,
                url = "fake://video/$i?size=$size$flaky"
            )
        }
        return Course(courseId, "Kotlin Coroutines for Android", lessons)
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 500L
        const val MEGABYTE = 1_048_576L
        const val LESSON_ID_FACTOR = 100
        const val FLAKY_INDEX = 4
        val TITLES = listOf(
            "Why coroutines?",
            "Suspend functions under the hood",
            "Structured concurrency",
            "Dispatchers and main-safety",
            "Cancellation and exceptions",
            "Flow basics",
            "StateFlow vs SharedFlow"
        )
    }
}
