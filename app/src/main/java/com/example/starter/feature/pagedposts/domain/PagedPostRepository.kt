package com.example.starter.feature.pagedposts.domain

import androidx.paging.PagingData
import com.example.starter.core.model.Post
import kotlinx.coroutines.flow.Flow

/**
 * PagingData in the domain layer is a common, accepted compromise: paging-common is pure Kotlin
 * (no Android types). If an interviewer pushes back, the alternative is the manual approach in feature/posts.
 */
interface PagedPostRepository {
    fun pagedPosts(pageSize: Int): Flow<PagingData<Post>>
}
