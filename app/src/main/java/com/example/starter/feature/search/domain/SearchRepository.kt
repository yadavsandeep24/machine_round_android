package com.example.starter.feature.search.domain

import com.example.starter.core.common.AppResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    /** Type-ahead suggestions for a prefix. Implementations may cache. */
    suspend fun suggestions(prefix: String): AppResult<List<String>>

    /** One page of results for a submitted query. */
    suspend fun search(query: String, page: Int, pageSize: Int): AppResult<AssetPage>

    /** Most recent submitted queries, newest first. */
    val recentSearches: Flow<List<String>>

    fun addRecentSearch(query: String)
}
