package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.model.Post
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Business rule lives here: favorites pinned to the top, then by id. */
class ObservePostsUseCase @Inject constructor(private val repository: PostRepository) {
    operator fun invoke(): Flow<List<Post>> = repository.observePosts().map { posts ->
        posts.sortedWith(compareByDescending<Post> { it.isFavorite }.thenBy { it.id })
    }
}
