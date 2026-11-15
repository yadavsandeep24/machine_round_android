package com.example.starter.feature.attendance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(@PrimaryKey val id: Int, val classId: Int, val rollNo: Int, val name: String)

/** The outbox: rows with version > syncedVersion still need uploading. */
@Entity(tableName = "attendance", primaryKeys = ["classId", "date", "studentId"])
data class AttendanceEntity(
    val classId: Int,
    val date: String,
    val studentId: Int,
    val present: Boolean,
    val version: Int,
    val syncedVersion: Int,
    val attempts: Int,
    val lastError: String?,
    val updatedAt: Long,
    val overriddenByServer: Boolean
)
