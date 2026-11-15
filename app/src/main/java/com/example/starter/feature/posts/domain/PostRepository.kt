package com.example.starter.feature.posts.domain

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import kotlinx.coroutines.flow.Flow

/** Contract defined in domain, implemented in data (dependency inversion). */
interface PostRepository {
    /** Local DB is the source of truth; observe it. */
    fun observePosts(): Flow<List<Post>>

    /** Cached post, or fetched from network if it isn't cached (e.g. opened from a deep link). */
    fun observePost(id: Int): Flow<Post?>

    /** Fetch from network and replace the cache. */
    suspend fun refresh(): AppResult<Unit>
}
