package com.example.starter.feature.posts.domain

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import kotlinx.coroutines.flow.Flow

/** Contract defined in domain, implemented in data (dependency inversion). */
interface PostRepository {
    /** Local DB is the source of truth; observe it. */
    fun observePosts(): Flow<List<Post>>

    /**
     * Fetch one page from the network and write it into the DB.
     * Page 1 replaces the cache (refresh); later pages are appended.
     */
    suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult>
}
