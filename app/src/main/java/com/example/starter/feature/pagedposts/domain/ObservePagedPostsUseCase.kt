package com.example.starter.feature.pagedposts.domain

import androidx.paging.PagingData
import com.example.starter.core.model.Post
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePagedPostsUseCase @Inject constructor(private val repository: PagedPostRepository) {
    operator fun invoke(pageSize: Int = 20): Flow<PagingData<Post>> = repository.pagedPosts(pageSize)
}
