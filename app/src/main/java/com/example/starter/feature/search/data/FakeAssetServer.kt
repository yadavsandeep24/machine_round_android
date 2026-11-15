package com.example.starter.feature.search.data

import com.example.starter.feature.search.domain.Asset
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * In-app stand-in for the backend the interviewer would give you (swap for a Retrofit API).
 * Deterministic data, simulated latency, and failure injection for testing error paths.
 */
class FakeAssetServer(private val latencyMs: Long = DEFAULT_LATENCY_MS, assetCount: Int = DEFAULT_ASSET_COUNT) {
    /** Set to make the next call(s) throw, e.g. to demo the retry UI. */
    var failuresRemaining: Int = 0

    var suggestCalls = 0
        private set
    var searchCalls = 0
        private set

    private val assets: List<Asset> = List(assetCount) { i ->
        val subject = SUBJECTS[i % SUBJECTS.size]
        val style = STYLES[(i / SUBJECTS.size) % STYLES.size]
        val color = COLORS[i % COLORS.size]
        Asset(
            id = i + 1,
            title = "$style $subject",
            author = "Creator ${i % AUTHORS}",
            tags = listOf(subject.lowercase(), style.lowercase(), color)
        )
    }

    /** keyword -> how many assets use it (used to rank suggestions). */
    private val keywordFrequency: Map<String, Int> = assets.flatMap { it.tags }.groupingBy { it }.eachCount()

    suspend fun suggest(prefix: String, limit: Int = SUGGESTION_LIMIT): List<String> {
        suggestCalls++
        simulateNetwork()
        val p = prefix.trim().lowercase()
        return keywordFrequency.entries
            .filter { it.key.startsWith(p) }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key }
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<Asset> {
        searchCalls++
        simulateNetwork()
        val q = query.trim().lowercase()
        val matches = assets.filter { asset ->
            asset.title.lowercase().contains(q) || asset.tags.any { it.startsWith(q) }
        }
        return matches.drop((page - 1) * pageSize).take(pageSize)
    }

    private suspend fun simulateNetwork() {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 400L
        const val DEFAULT_ASSET_COUNT = 360
        const val SUGGESTION_LIMIT = 8
        const val AUTHORS = 17
        val SUBJECTS = listOf(
            "Mountain",
            "Ocean",
            "Forest",
            "City",
            "Desert",
            "Sunset",
            "Portrait",
            "Coffee",
            "Workspace",
            "Flowers",
            "Mountains",
            "Market"
        )
        val STYLES = listOf("Minimal", "Vintage", "Aerial", "Moody", "Bright", "Abstract", "Monochrome", "Watercolor", "Neon", "Macro")
        val COLORS = listOf("blue", "green", "golden", "black", "pastel", "red")
    }
}
