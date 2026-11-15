package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fakes > mocks for repositories: simpler, and they behave like the real thing. */
class FakePostRepository(
    var remote: List<Post> = emptyList(),
    var shouldFail: Boolean = false,
    /** Simulated network latency in virtual ms. */
    var delayMs: Long = 0L
) : PostRepository {
    /** Acts as the Room cache. Tests can pre-fill it to simulate an offline start. */
    val db = MutableStateFlow<List<Post>>(emptyList())

    override fun observePosts(): Flow<List<Post>> = db

    /** Same contract as the real repo: cached row first, otherwise "network" (remote). */
    override fun observePost(id: Int): Flow<Post?> = db.map { list -> list.find { it.id == id } ?: remote.find { it.id == id } }

    override suspend fun refresh(): AppResult<Unit> {
        delay(delayMs)
        if (shouldFail) return AppResult.Failure(RuntimeException("boom"))
        db.value = remote
        return AppResult.Success(Unit)
    }
}
