package com.example.starter.feature.course.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE id = :courseId")
    fun observeCourse(courseId: Int): Flow<CourseEntity?>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY position")
    fun observeLessons(courseId: Int): Flow<List<LessonEntity>>

    @Upsert
    suspend fun upsertCourse(course: CourseEntity)

    @Upsert
    suspend fun upsertLessons(lessons: List<LessonEntity>)

    @Transaction
    suspend fun save(course: CourseEntity, lessons: List<LessonEntity>) {
        upsertCourse(course)
        upsertLessons(lessons)
    }
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE lessonId = :lessonId")
    suspend fun get(lessonId: Int): DownloadEntity?

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE lessonId = :lessonId")
    suspend fun delete(lessonId: Int)
}
