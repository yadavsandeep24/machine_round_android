package com.example.starter.feature.posts.domain.usecase

import com.example.starter.core.common.AppResult
import com.example.starter.feature.posts.domain.PageResult
import com.example.starter.feature.posts.domain.PostRepository
import javax.inject.Inject

class LoadPostsPageUseCase @Inject constructor(private val repository: PostRepository) {
    suspend operator fun invoke(page: Int, pageSize: Int = PAGE_SIZE): AppResult<PageResult> = repository.loadPage(page, pageSize)

    companion object {
        const val PAGE_SIZE = 20
    }
}
