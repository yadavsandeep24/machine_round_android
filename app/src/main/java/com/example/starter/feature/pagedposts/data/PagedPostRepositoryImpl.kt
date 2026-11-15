package com.example.starter.feature.pagedposts.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.starter.core.database.AppDatabase
import com.example.starter.core.model.Post
import com.example.starter.core.network.api.PostApi
import com.example.starter.feature.pagedposts.domain.PagedPostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PagedPostRepositoryImpl @Inject constructor(private val api: PostApi, private val db: AppDatabase) : PagedPostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedPosts(pageSize: Int): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = pageSize / 4,
            initialLoadSize = pageSize,
            enablePlaceholders = false
        ),
        remoteMediator = PostRemoteMediator(api, db, pageSize),
        pagingSourceFactory = { db.pagedPostDao().pagingSource() }
    ).flow.map { pagingData ->
        pagingData.map { Post(id = it.id, userId = it.userId, title = it.title, body = it.body) }
    }
}
