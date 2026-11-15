package com.example.starter.feature.comments.domain

import com.example.starter.core.common.AppResult

interface CommentRepository {
    suspend fun comments(postId: Int): AppResult<List<Comment>>
}
