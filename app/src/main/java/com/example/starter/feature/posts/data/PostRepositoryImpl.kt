package com.example.starter.feature.posts.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.common.IoDispatcher
import com.example.starter.core.model.Post
import com.example.starter.core.network.api.PostApi
import com.example.starter.core.network.safeCall
import com.example.starter.feature.posts.data.local.PostDao
import com.example.starter.feature.posts.domain.PageResult
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Offline-first: the UI observes Room; the network only writes into Room. */
class PostRepositoryImpl @Inject constructor(
    private val api: PostApi,
    private val dao: PostDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult> = withContext(io) {
        safeCall {
            val entities = api.getPosts(page = page, limit = pageSize).map { it.toEntity() }
            if (page == 1) dao.replaceAll(entities) else dao.upsertAll(entities)
            // Fewer items than requested => no more pages. (Use total/hasMore/nextCursor if your API returns one.)
            PageResult(page = page, endReached = entities.size < pageSize)
        }
    }
}
