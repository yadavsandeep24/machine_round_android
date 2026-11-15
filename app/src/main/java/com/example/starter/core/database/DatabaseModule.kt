package com.example.starter.core.database

import android.content.Context
import androidx.room.Room
import com.example.starter.feature.attendance.data.local.AttendanceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            // Never destructive here: the outbox holds unsynced user data. Write real migrations.
            .build()

    @Provides
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()
}
