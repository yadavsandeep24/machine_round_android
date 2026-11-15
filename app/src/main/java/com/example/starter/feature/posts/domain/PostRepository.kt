package com.example.starter.feature.posts.domain

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import kotlinx.coroutines.flow.Flow

/** Contract defined in domain, implemented in data (dependency inversion). */
interface PostRepository {
    /** Local DB is the source of truth for browsing; observe it. */
    fun observePosts(): Flow<List<Post>>

    /** Cached post, or fetched from network if it isn't cached (e.g. opened from search results). */
    fun observePost(id: Int): Flow<Post?>

    fun observeFavoriteIds(): Flow<Set<Int>>

    /**
     * Fetch one page from the network and write it into the DB.
     * Page 1 replaces the cache (refresh); later pages are appended.
     */
    suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult>

    /** Server-side search. Results are not cached: they are only valid for that query. */
    suspend fun searchPosts(query: String, page: Int, pageSize: Int): AppResult<PostPage>

    suspend fun toggleFavorite(post: Post)
}
