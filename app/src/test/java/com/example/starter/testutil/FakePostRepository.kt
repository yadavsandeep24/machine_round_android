package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PageResult
import com.example.starter.feature.posts.domain.PostPage
import com.example.starter.feature.posts.domain.PostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Fakes > mocks for repositories: simpler, and they behave like the real thing. */
class FakePostRepository(
    var remote: List<Post> = emptyList(),
    var shouldFail: Boolean = false,
    val failingPages: MutableSet<Int> = mutableSetOf(),
    var shouldFailSearch: Boolean = false,
    /** Simulated network latency per search query, in virtual ms. */
    val searchDelays: MutableMap<String, Long> = mutableMapOf()
) : PostRepository {
    private val db = MutableStateFlow<List<Post>>(emptyList())
    val requestedPages = mutableListOf<Int>()
    val searchRequests = mutableListOf<Pair<String, Int>>() // (query, page) started
    val completedSearches = mutableListOf<String>() // finished without being cancelled

    override fun observePosts(): Flow<List<Post>> = db

    /** Same contract as the real repo: cached row first, otherwise "network" (remote). */
    override fun observePost(id: Int): Flow<Post?> = db.map { list -> list.find { it.id == id } ?: remote.find { it.id == id } }
    override fun observeFavoriteIds(): Flow<Set<Int>> = db.map { list -> list.filter { it.isFavorite }.map { it.id }.toSet() }

    override suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult> {
        requestedPages += page
        if (shouldFail || page in failingPages) return AppResult.Failure(RuntimeException("boom"))
        val items = remote.drop((page - 1) * pageSize).take(pageSize)
        db.update { current -> if (page == 1) items else (current + items).distinctBy { it.id } }
        return AppResult.Success(PageResult(page, endReached = items.size < pageSize))
    }

    override suspend fun searchPosts(query: String, page: Int, pageSize: Int): AppResult<PostPage> {
        searchRequests += query to page
        delay(searchDelays[query] ?: 0L) // cancellation lands here
        completedSearches += query
        if (shouldFailSearch) return AppResult.Failure(RuntimeException("search failed"))
        val matches = remote.filter { it.title.contains(query, ignoreCase = true) }
        val items = matches.drop((page - 1) * pageSize).take(pageSize)
        return AppResult.Success(PostPage(items, page, endReached = items.size < pageSize))
    }

    override suspend fun toggleFavorite(post: Post) = db.update { list ->
        if (list.any { it.id == post.id }) {
            list.map { if (it.id == post.id) it.copy(isFavorite = !it.isFavorite) else it }
        } else {
            list + post.copy(isFavorite = true)
        }
    }
}
