package com.example.starter.feature.posts.domain.usecase

import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Lets uncached lists (search results) show live favorite state. */
class ObserveFavoriteIdsUseCase @Inject constructor(private val repository: PostRepository) {
    operator fun invoke(): Flow<Set<Int>> = repository.observeFavoriteIds()
}
