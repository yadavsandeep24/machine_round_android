package com.example.starter.feature.comments.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.safeCall
import com.example.starter.feature.comments.domain.Comment
import com.example.starter.feature.comments.domain.CommentRepository
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(private val server: FakeCommentServer) : CommentRepository {
    override suspend fun comments(postId: Int): AppResult<List<Comment>> = safeCall { server.fetchComments(postId) }
}
