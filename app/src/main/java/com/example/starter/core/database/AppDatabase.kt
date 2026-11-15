package com.example.starter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.starter.feature.posts.data.local.PostDao
import com.example.starter.feature.posts.data.local.PostEntity

@Database(entities = [PostEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
