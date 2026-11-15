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

    @Query("SELECT * FROM posts WHERE id = :id")
    fun observeById(id: Int): Flow<PostEntity?>

    @Query("SELECT id FROM posts WHERE isFavorite = 1")
    fun observeFavoriteIds(): Flow<List<Int>>

    @Query("SELECT id FROM posts WHERE isFavorite = 1")
    suspend fun favoriteIds(): List<Int>

    @Upsert
    suspend fun upsertAll(posts: List<PostEntity>)

    /** Returns the number of rows changed (0 = row not in DB). */
    @Query("UPDATE posts SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavoriteById(id: Int): Int

    @Query("DELETE FROM posts WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()

    /**
     * Refresh: drop cached rows but KEEP favorites (they may come from later pages or search results),
     * then write page 1.
     */
    @Transaction
    suspend fun replaceAllKeepingFavorites(posts: List<PostEntity>) {
        deleteNonFavorites()
        upsertAllKeepingFavorites(posts)
    }

    /** Append a page without wiping favorites on rows we already have. */
    @Transaction
    suspend fun upsertAllKeepingFavorites(posts: List<PostEntity>) {
        val favs = favoriteIds().toSet()
        upsertAll(posts.map { it.copy(isFavorite = it.id in favs) })
    }

    /** Toggle if cached; otherwise (e.g. a search result) insert it as a favorite. */
    @Transaction
    suspend fun toggleFavorite(post: PostEntity) {
        if (toggleFavoriteById(post.id) == 0) upsertAll(listOf(post.copy(isFavorite = true)))
    }
}
