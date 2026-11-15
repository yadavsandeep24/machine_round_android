package com.example.starter.testutil

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PageResult
import com.example.starter.feature.posts.domain.PostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Fakes > mocks for repositories: simpler, and they behave like the real thing. */
class FakePostRepository(
    var remote: List<Post> = emptyList(),
    var shouldFail: Boolean = false,
    val failingPages: MutableSet<Int> = mutableSetOf(),
    /** Simulated network latency in virtual ms. */
    var delayMs: Long = 0L
) : PostRepository {
    /** Acts as the Room cache. Tests can pre-fill it to simulate an offline start. */
    val db = MutableStateFlow<List<Post>>(emptyList())
    val requestedPages = mutableListOf<Int>()

    override fun observePosts(): Flow<List<Post>> = db

    override suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult> {
        requestedPages += page
        delay(delayMs)
        if (shouldFail || page in failingPages) return AppResult.Failure(RuntimeException("boom"))
        val items = remote.drop((page - 1) * pageSize).take(pageSize)
        db.update { current -> if (page == 1) items else (current + items).distinctBy { it.id } }
        return AppResult.Success(PageResult(page, endReached = items.size < pageSize))
    }
}
