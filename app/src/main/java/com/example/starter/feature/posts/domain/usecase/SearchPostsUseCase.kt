package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.common.AppResult
import com.example.starter.feature.posts.domain.PostPage
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject

class SearchPostsUseCase @Inject constructor(private val repository: PostRepository) {
    suspend operator fun invoke(query: String, page: Int, pageSize: Int = LoadPostsPageUseCase.PAGE_SIZE): AppResult<PostPage> =
        repository.searchPosts(query.trim(), page, pageSize)
}
