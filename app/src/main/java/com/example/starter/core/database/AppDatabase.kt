package com.example.starter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.starter.feature.attendance.data.local.AttendanceDao
import com.example.starter.feature.attendance.data.local.AttendanceEntity
import com.example.starter.feature.attendance.data.local.StudentEntity

@Database(entities = [StudentEntity::class, AttendanceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
