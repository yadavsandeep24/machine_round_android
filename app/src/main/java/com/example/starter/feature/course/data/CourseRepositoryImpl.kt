package com.example.starter.feature.course.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.safeCall
import com.example.starter.feature.course.data.local.CourseDao
import com.example.starter.feature.course.data.local.CourseEntity
import com.example.starter.feature.course.data.local.LessonEntity
import com.example.starter.feature.course.domain.Course
import com.example.starter.feature.course.domain.CourseRepository
import com.example.starter.feature.course.domain.Lesson
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Offline-first: the course outline is cached in Room so it's browsable with no network. */
class CourseRepositoryImpl @Inject constructor(private val server: FakeCourseServer, private val dao: CourseDao) : CourseRepository {

    override fun observeCourse(courseId: Int): Flow<Course?> = combine(dao.observeCourse(courseId), dao.observeLessons(courseId)) {
            course,
            lessons
        ->
        course?.let { c -> Course(c.id, c.title, lessons.map { it.toDomain() }) }
    }

    override suspend fun refresh(courseId: Int): AppResult<Unit> = safeCall {
        val course = server.fetchCourse(courseId)
        dao.save(
            CourseEntity(course.id, course.title),
            course.lessons.mapIndexed { i, l -> LessonEntity(l.id, l.courseId, i, l.title, l.durationMinutes, l.sizeBytes, l.url) }
        )
    }

    private fun LessonEntity.toDomain() = Lesson(id, courseId, title, durationMinutes, sizeBytes, url)
}
