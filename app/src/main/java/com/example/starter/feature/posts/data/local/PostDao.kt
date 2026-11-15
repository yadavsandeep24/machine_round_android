package com.example.starter.feature.posts.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id")
    fun observeAll(): Flow<List<PostEntity>>

    @Upsert
    suspend fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clear()

    /** Refresh: swap the whole cache in one transaction, so observers never see an empty list in between. */
    @Transaction
    suspend fun replaceAll(posts: List<PostEntity>) {
        clear()
        upsertAll(posts)
    }
}
