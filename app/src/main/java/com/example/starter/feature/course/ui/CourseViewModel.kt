package com.example.starter.feature.course.ui

import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.course.domain.Course
import com.example.starter.feature.course.domain.CourseRepository
import com.example.starter.feature.course.domain.DownloadRepository
import com.example.starter.feature.course.domain.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The ViewModel never touches WorkManager or files: it observes Room (course + download state)
 * and network status, and sends commands to the repository. Downloads keep running when the
 * screen (or the app) goes away, and the UI simply re-observes their state.
 */
@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courses: CourseRepository,
    private val downloads: DownloadRepository,
    network: NetworkMonitor
) : MviViewModel<CourseIntent, CourseState, CourseEffect>(CourseState()) {

    private val downloadedOnly = MutableStateFlow(false)
    private var cachedCourse: Course? = null

    init {
        refresh()
        viewModelScope.launch {
            combine(courses.observeCourse(COURSE_ID), downloads.observeDownloads(), network.isOnline, downloadedOnly) {
                    course,
                    statuses,
                    online,
                    onlyDl
                ->
                cachedCourse = course
                Snapshot(course, statuses, online, onlyDl)
            }.collect(::reduce)
        }
    }

    override fun handleIntent(intent: CourseIntent) {
        when (intent) {
            CourseIntent.Retry -> refresh()
            is CourseIntent.Download -> lesson(intent.lessonId)?.let { viewModelScope.launch { downloads.enqueue(it) } }
            is CourseIntent.Cancel -> viewModelScope.launch { downloads.cancel(intent.lessonId) }
            is CourseIntent.Delete -> viewModelScope.launch { downloads.delete(intent.lessonId) }
            CourseIntent.ToggleDownloadedOnly -> downloadedOnly.update { !it }
        }
    }

    private fun lesson(id: Int) = cachedCourse?.lessons?.firstOrNull { it.id == id }

    private fun refresh() {
        viewModelScope.launch {
            setState { copy(isLoading = cachedCourse == null, error = null) }
            when (val result = courses.refresh(COURSE_ID)) {
                is AppResult.Success -> setState { copy(isLoading = false) }
                is AppResult.Failure -> {
                    val msg = result.throwable.toUserMessage()
                    if (cachedCourse != null) {
                        setState { copy(isLoading = false) } // offline with cache: keep going
                        sendEffect(CourseEffect.ShowMessage(msg))
                    } else {
                        setState { copy(isLoading = false, error = msg) }
                    }
                }
            }
        }
    }

    private fun reduce(s: Snapshot) {
        val lessons = s.course?.lessons.orEmpty()
        val all = lessons.map { LessonRow(it, s.statuses[it.id] ?: DownloadStatus.NotDownloaded) }
        val downloaded = all.filter { it.status is DownloadStatus.Downloaded }
        setState {
            copy(
                isLoading = isLoading && s.course == null,
                courseTitle = s.course?.title.orEmpty(),
                isOnline = s.online,
                downloadedOnly = s.downloadedOnly,
                rows = if (s.downloadedOnly || !s.online) downloaded else all,
                downloadedCount = downloaded.size,
                downloadedBytes = downloaded.sumOf { it.lesson.sizeBytes }
            )
        }
    }

    private data class Snapshot(
        val course: Course?,
        val statuses: Map<Int, DownloadStatus>,
        val online: Boolean,
        val downloadedOnly: Boolean
    )

    private companion object {
        const val COURSE_ID = 1
    }
}
