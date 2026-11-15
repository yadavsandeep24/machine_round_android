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
    /** Simulated network latency in virtual ms. */
    var delayMs: Long = 0L
) : PostRepository {
    /** Acts as the Room cache. Tests can pre-fill it to simulate an offline start. */
    val db = MutableStateFlow<List<Post>>(emptyList())

    override fun observePosts(): Flow<List<Post>> = db

    override suspend fun refresh(): AppResult<Unit> {
        delay(delayMs)
        if (shouldFail) return AppResult.Failure(RuntimeException("boom"))
        db.value = remote
        return AppResult.Success(Unit)
    }
}
