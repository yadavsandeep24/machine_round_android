package com.example.starter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.starter.feature.course.data.local.CourseDao
import com.example.starter.feature.course.data.local.CourseEntity
import com.example.starter.feature.course.data.local.DownloadDao
import com.example.starter.feature.course.data.local.DownloadEntity
import com.example.starter.feature.course.data.local.LessonEntity

@Database(entities = [CourseEntity::class, LessonEntity::class, DownloadEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun downloadDao(): DownloadDao
}
