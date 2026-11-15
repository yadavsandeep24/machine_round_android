package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.common.AppResult
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject

class RefreshPostsUseCase @Inject constructor(private val repository: PostRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
