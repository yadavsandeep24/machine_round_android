package com.example.starter.feature.pagedposts.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PagedPostDao {
    /** Room generates the PagingSource and invalidates it whenever the table changes. */
    @Query("SELECT * FROM paged_posts ORDER BY id")
    fun pagingSource(): PagingSource<Int, PagedPostEntity>

    @Upsert
    suspend fun upsertAll(posts: List<PagedPostEntity>)

    @Query("DELETE FROM paged_posts")
    suspend fun clear()
}

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE label = :label")
    suspend fun get(label: String): RemoteKeyEntity?

    @Upsert
    suspend fun upsert(key: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE label = :label")
    suspend fun delete(label: String)
}
