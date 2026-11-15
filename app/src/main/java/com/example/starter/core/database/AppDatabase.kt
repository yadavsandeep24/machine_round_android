package com.example.starter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.starter.feature.pagedposts.data.local.PagedPostDao
import com.example.starter.feature.pagedposts.data.local.PagedPostEntity
import com.example.starter.feature.pagedposts.data.local.RemoteKeyDao
import com.example.starter.feature.pagedposts.data.local.RemoteKeyEntity
import com.example.starter.feature.posts.data.local.PostDao
import com.example.starter.feature.posts.data.local.PostEntity

@Database(
    entities = [PostEntity::class, PagedPostEntity::class, RemoteKeyEntity::class],
    version = 2, // bumped for the Paging 3 tables (fallbackToDestructiveMigration in DatabaseModule)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun pagedPostDao(): PagedPostDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
