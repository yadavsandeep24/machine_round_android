package com.example.starter.feature.posts.data

import com.example.starter.core.model.Post
import com.example.starter.core.network.api.PostDto
import com.example.starter.feature.posts.data.local.PostEntity

fun PostDto.toEntity() = PostEntity(id = id, userId = userId, title = title, body = body)

fun PostDto.toDomain() = Post(id = id, userId = userId, title = title, body = body)

fun PostEntity.toDomain() = Post(id = id, userId = userId, title = title, body = body)
