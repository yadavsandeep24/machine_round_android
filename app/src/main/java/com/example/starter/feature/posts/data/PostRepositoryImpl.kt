package com.example.starter.feature.posts.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.common.IoDispatcher
import com.example.starter.core.model.Post
import com.example.starter.core.network.api.PostApi
import com.example.starter.core.network.safeCall
import com.example.starter.feature.posts.data.local.PostDao
import com.example.starter.feature.posts.domain.PageResult
import com.example.starter.feature.posts.domain.PostPage
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

/** Offline-first: browsing observes Room, network only writes into Room. Search goes straight to network. */
class PostRepositoryImpl @Inject constructor(
    private val api: PostApi,
    private val dao: PostDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePost(id: Int): Flow<Post?> = dao.observeById(id).transformLatest { entity ->
        if (entity != null) {
            emit(entity.toDomain())
        } else {
            val remote = withContext(io) { safeCall { api.getPost(id) } }
            emit((remote as? AppResult.Success)?.data?.toDomain())
        }
    }

    override fun observeFavoriteIds(): Flow<Set<Int>> = dao.observeFavoriteIds().map { it.toSet() }

    override suspend fun loadPage(page: Int, pageSize: Int): AppResult<PageResult> = withContext(io) {
        safeCall {
            val entities = api.getPosts(page = page, limit = pageSize).map { it.toEntity() }
            if (page == 1) {
                dao.replaceAllKeepingFavorites(entities)
            } else {
                dao.upsertAllKeepingFavorites(entities)
            }
            // Fewer items than requested => no more pages. (Use total/hasMore/nextCursor if your API returns one.)
            PageResult(page = page, endReached = entities.size < pageSize)
        }
    }

    override suspend fun searchPosts(query: String, page: Int, pageSize: Int): AppResult<PostPage> = withContext(io) {
        safeCall {
            val items = api.searchPosts(query = query, page = page, limit = pageSize).map { it.toDomain() }
            PostPage(items = items, page = page, endReached = items.size < pageSize)
        }
    }

    override suspend fun toggleFavorite(post: Post) = withContext(io) {
        dao.toggleFavorite(post.toEntity())
    }
}
