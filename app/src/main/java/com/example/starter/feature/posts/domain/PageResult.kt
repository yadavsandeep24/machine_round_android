package com.example.starter.feature.posts.domain

import com.example.starter.core.model.Post
/** Result of loading a page into the cache (browse mode). */
data class PageResult(val page: Int, val endReached: Boolean)

/** A page returned straight from the network (search mode, not cached). */
data class PostPage(val items: List<Post>, val page: Int, val endReached: Boolean)
