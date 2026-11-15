package com.example.starter.core.network.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PostApi {
    /** JSONPlaceholder uses `_page` / `_limit`. Change the names to match your API (page/size, offset/limit, cursor...). */
    @GET("posts")
    suspend fun getPosts(@Query("_page") page: Int, @Query("_limit") limit: Int): List<PostDto>

    /** Server-side full-text search, paginated the same way. */
    @GET("posts")
    suspend fun searchPosts(@Query("q") query: String, @Query("_page") page: Int, @Query("_limit") limit: Int): List<PostDto>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto
}
