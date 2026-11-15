package com.example.starter.feature.search.data

import com.example.starter.core.common.AppResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchRepositoryImplTest {

    private val server = FakeAssetServer(latencyMs = 0)
    private val repo = SearchRepositoryImpl(server)

    @Test
    fun `suggestions are ranked prefix matches and cached per prefix`() = runTest {
        val first = repo.suggestions("mou") as AppResult.Success
        assertThat(first.data).containsExactly("mountain", "mountains")
        repo.suggestions("MOU ") // normalised to the same key
        assertThat(server.suggestCalls).isEqualTo(1)
    }

    @Test
    fun `failures are not cached`() = runTest {
        server.failuresRemaining = 1
        assertThat(repo.suggestions("oc")).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repo.suggestions("oc")).isInstanceOf(AppResult.Success::class.java)
        assertThat(server.suggestCalls).isEqualTo(2)
    }

    @Test
    fun `search pages until a short page, without duplicates`() = runTest {
        // "mountain" matches the Mountain and Mountains subjects: 30 + 30 = 60 assets = 3 pages of 20.
        val pages = mutableListOf<com.example.starter.feature.search.domain.AssetPage>()
        var page = 1
        do {
            pages += (repo.search("mountain", page++, 20) as AppResult.Success).data
        } while (!pages.last().endReached)
        val all = pages.flatMap { it.items }
        assertThat(all).hasSize(60)
        assertThat(all.map { it.id }.toSet()).hasSize(60)
    }

    @Test
    fun `recent searches are newest first, de-duplicated, capped at 5`() = runTest {
        listOf("a", "b", "c", "B", "d", "e", "f").forEach(repo::addRecentSearch)
        assertThat(repo.recentSearches.first()).containsExactly("f", "e", "d", "B", "c").inOrder()
    }
}
