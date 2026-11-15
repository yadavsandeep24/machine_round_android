package com.example.starter.feature.search.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.safeCall
import com.example.starter.feature.search.domain.AssetPage
import com.example.starter.feature.search.domain.SearchRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SearchRepositoryImpl @Inject constructor(private val server: FakeAssetServer) : SearchRepository {

    /** Prefix -> suggestions. Typing "mou", deleting, retyping "mou" costs zero network calls. */
    private val suggestionCache = LruCache<String, List<String>>(SUGGESTION_CACHE_SIZE)

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    override val recentSearches: Flow<List<String>> = _recentSearches.asStateFlow()

    override suspend fun suggestions(prefix: String): AppResult<List<String>> {
        val key = prefix.trim().lowercase()
        suggestionCache[key]?.let { return AppResult.Success(it) }
        val result = safeCall { server.suggest(key) }
        if (result is AppResult.Success) suggestionCache[key] = result.data // never cache failures
        return result
    }

    override suspend fun search(query: String, page: Int, pageSize: Int): AppResult<AssetPage> = safeCall {
        val items = server.search(query, page, pageSize)
        AssetPage(items = items, endReached = items.size < pageSize)
    }

    override fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _recentSearches.update { current -> (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) }).take(MAX_RECENT) }
    }

    private companion object {
        const val SUGGESTION_CACHE_SIZE = 50
        const val MAX_RECENT = 5
    }
}
