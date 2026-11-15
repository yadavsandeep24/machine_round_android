package com.example.starter.feature.pagedposts.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.starter.core.database.AppDatabase
import com.example.starter.core.network.api.PostApi
import com.example.starter.feature.pagedposts.data.local.PagedPostEntity
import com.example.starter.feature.pagedposts.data.local.RemoteKeyEntity
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Network -> Room. The UI never reads the network directly:
 * Room's PagingSource feeds the list, and this mediator is called when it runs out of data.
 */
@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(private val api: PostApi, private val db: AppDatabase, private val pageSize: Int) :
    RemoteMediator<Int, PagedPostEntity>() {

    private val postDao = db.pagedPostDao()
    private val keyDao = db.remoteKeyDao()

    /** Always refresh from network on open. Use SKIP_INITIAL_REFRESH + a cache timeout for stale-while-revalidate. */
    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PagedPostEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> keyDao.get(LABEL)?.nextPage
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val items = api.getPosts(page = page, limit = pageSize)
            val endReached = items.size < pageSize

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    postDao.clear()
                    keyDao.delete(LABEL)
                }
                keyDao.upsert(RemoteKeyEntity(LABEL, nextPage = if (endReached) null else page + 1))
                postDao.upsertAll(items.map { PagedPostEntity(it.id, it.userId, it.title, it.body) })
            }
            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: SerializationException) {
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val LABEL = "posts"
    }
}
