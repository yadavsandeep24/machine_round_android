package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Fakes > mocks for repositories: simpler, and they behave like the real thing. */
class FakePostRepository(
    var remote: List<Post> = emptyList(),
    var shouldFail: Boolean = false,
    var shouldFailSearch: Boolean = false,
    /** Simulated network latency in virtual ms. */
    var delayMs: Long = 0L,
    /** Simulated latency per search query, in virtual ms. */
    val searchDelays: MutableMap<String, Long> = mutableMapOf()
) : PostRepository {
    /** Acts as the Room cache. Tests can pre-fill it to simulate an offline start. */
    val db = MutableStateFlow<List<Post>>(emptyList())
    val searchRequests = mutableListOf<String>() // started
    val completedSearches = mutableListOf<String>() // finished without being cancelled

    override fun observePosts(): Flow<List<Post>> = db

    override suspend fun refresh(): AppResult<Unit> {
        delay(delayMs)
        if (shouldFail) return AppResult.Failure(RuntimeException("boom"))
        db.value = remote
        return AppResult.Success(Unit)
    }

    override suspend fun searchPosts(query: String): AppResult<List<Post>> {
        searchRequests += query
        delay(searchDelays[query] ?: 0L) // cancellation lands here
        completedSearches += query
        if (shouldFailSearch) return AppResult.Failure(RuntimeException("search failed"))
        return AppResult.Success(remote.filter { it.title.contains(query, ignoreCase = true) })
    }
}
