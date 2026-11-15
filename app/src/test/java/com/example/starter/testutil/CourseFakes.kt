package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.feature.course.data.download.DownloadScheduler
import com.example.starter.feature.course.data.download.DownloadStore
import com.example.starter.feature.course.data.download.LessonFileStore
import com.example.starter.feature.course.domain.Course
import com.example.starter.feature.course.domain.CourseRepository
import com.example.starter.feature.course.domain.DownloadStatus
import com.example.starter.feature.course.domain.Lesson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryDownloadStore : DownloadStore {
    val state = MutableStateFlow<Map<Int, DownloadStatus>>(emptyMap())
    val history = mutableListOf<Pair<Int, DownloadStatus>>()
    override fun observeAll(): Flow<Map<Int, DownloadStatus>> = state
    override suspend fun get(lessonId: Int): DownloadStatus = state.value[lessonId] ?: DownloadStatus.NotDownloaded
    override suspend fun set(lessonId: Int, status: DownloadStatus) {
        history += lessonId to status
        state.update { if (status == DownloadStatus.NotDownloaded) it - lessonId else it + (lessonId to status) }
    }
    override suspend fun remove(lessonId: Int) = state.update { it - lessonId }
}

class RecordingScheduler : DownloadScheduler {
    val scheduled = mutableListOf<Int>()
    val cancelled = mutableListOf<Int>()
    override fun schedule(lesson: Lesson) {
        scheduled += lesson.id
    }
    override fun cancel(lessonId: Int) {
        cancelled += lessonId
    }
}

class TempDirLessonFileStore(private val dir: File) : LessonFileStore {
    override fun fileFor(lessonId: Int): File = File(dir, "$lessonId.mp4")
    override fun delete(lessonId: Int) {
        fileFor(lessonId).delete()
    }
}

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}

class FakeCourseRepository(private val course: Course) : CourseRepository {
    val cached = MutableStateFlow<Course?>(null)
    var shouldFail = false
    override fun observeCourse(courseId: Int): Flow<Course?> = cached
    override suspend fun refresh(courseId: Int): AppResult<Unit> {
        if (shouldFail) return AppResult.Failure(java.io.IOException("offline"))
        cached.value = course
        return AppResult.Success(Unit)
    }
}
