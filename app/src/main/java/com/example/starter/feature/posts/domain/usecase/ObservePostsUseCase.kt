package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePostsUseCase @Inject constructor(private val repository: PostRepository) {
    operator fun invoke(): Flow<List<Post>> = repository.observePosts()
}
