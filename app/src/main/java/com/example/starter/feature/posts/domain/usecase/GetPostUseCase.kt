package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetPostUseCase @Inject constructor(private val repository: PostRepository) {
    operator fun invoke(id: Int): Flow<Post?> = repository.observePost(id)
}
