package com.example.starter.feature.course.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(@PrimaryKey val id: Int, val title: String)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Int,
    val courseId: Int,
    val position: Int,
    val title: String,
    val durationMinutes: Int,
    val sizeBytes: Long,
    val url: String
)

/** One row per lesson that has download state. No row = not downloaded. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val lessonId: Int,
    val status: String,
    val percent: Int = 0,
    val filePath: String? = null,
    val error: String? = null
)
