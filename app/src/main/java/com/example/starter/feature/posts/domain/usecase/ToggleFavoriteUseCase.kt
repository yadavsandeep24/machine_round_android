package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(private val repository: PostRepository) {
    suspend operator fun invoke(post: Post) = repository.toggleFavorite(post)
}
