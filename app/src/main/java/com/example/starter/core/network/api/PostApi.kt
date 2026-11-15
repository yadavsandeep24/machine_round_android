package com.example.starter.core.network.api

import retrofit2.http.GET
import retrofit2.http.Query

interface PostApi {
    /**
     * Without arguments returns every post. With page/limit returns one page
     * (JSONPlaceholder uses `_page` / `_limit`; Retrofit drops null query params).
     */
    @GET("posts")
    suspend fun getPosts(@Query("_page") page: Int? = null, @Query("_limit") limit: Int? = null): List<PostDto>
}
