package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.common.AppResult
import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject

class SearchPostsUseCase @Inject constructor(private val repository: PostRepository) {
    suspend operator fun invoke(query: String): AppResult<List<Post>> = repository.searchPosts(query.trim())
}
