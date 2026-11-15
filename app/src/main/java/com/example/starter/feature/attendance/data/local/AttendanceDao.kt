package com.example.starter.feature.attendance.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY rollNo")
    fun observeStudents(classId: Int): Flow<List<StudentEntity>>

    @Upsert
    suspend fun upsertStudents(students: List<StudentEntity>)

    @Query("SELECT * FROM attendance WHERE classId = :classId AND date = :date")
    fun observeAttendance(classId: Int, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE classId = :classId AND date = :date AND studentId = :studentId")
    suspend fun get(classId: Int, date: String, studentId: Int): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE version > syncedVersion ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<AttendanceEntity>

    @Upsert
    suspend fun upsert(entity: AttendanceEntity)
}
