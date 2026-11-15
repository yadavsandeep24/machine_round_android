package com.example.starter.feature.catalog.data

import com.example.starter.feature.catalog.domain.Product
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-app stand-in for the backend (swap for Retrofit + a WebSocket/SSE stream for stock).
 * Deterministic data; the stock "push" stream simulates other shoppers buying items.
 */
class FakeCatalogServer(
    private val latencyMs: Long = DEFAULT_LATENCY_MS,
    private val stockTickMs: Long = DEFAULT_STOCK_TICK_MS,
    private val random: Random = Random(SEED),
    /** Number of simulated purchases before the stream completes. Tests pass a small number. */
    private val maxTicks: Int = Int.MAX_VALUE
) {
    var failuresRemaining = 0

    private val products: List<Product> = List(PRODUCT_COUNT) { i ->
        val brand = BRANDS[i % BRANDS.size]
        val category = CATEGORIES[i % CATEGORIES.size]
        Product(
            id = i + 1,
            name = "$brand $category ${i / BRANDS.size + 1}",
            brand = brand,
            price = BASE_PRICE + (i * PRICE_STEP) % PRICE_SPREAD,
            rating = RATING_FLOOR + (i * RATING_STEP) % RATING_SPREAD / RATING_DIVISOR
        )
    }

    private val stock: MutableMap<Int, Int> = products.associate { it.id to (it.id % MAX_STOCK) }.toMutableMap()

    suspend fun fetchProducts(): List<Product> {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
        return products
    }

    /** First emission is the full snapshot, then one simulated purchase per tick. */
    fun stockStream(): Flow<Map<Int, Int>> = flow {
        emit(stock.toMap())
        repeat(maxTicks) {
            delay(stockTickMs)
            val inStock = stock.filterValues { it > 0 }.keys.toList()
            if (inStock.isNotEmpty()) {
                val id = inStock[random.nextInt(inStock.size)]
                stock[id] = stock.getValue(id) - 1
            }
            emit(stock.toMap())
        }
    }

    /** Test hook: force a stock level, as if the server pushed it. */
    fun setStock(productId: Int, units: Int) {
        stock[productId] = units
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 600L
        const val DEFAULT_STOCK_TICK_MS = 3_000L
        const val SEED = 42
        const val PRODUCT_COUNT = 36
        const val MAX_STOCK = 6
        const val BASE_PRICE = 199
        const val PRICE_STEP = 137
        const val PRICE_SPREAD = 2_400
        const val RATING_FLOOR = 3.0
        const val RATING_STEP = 7
        const val RATING_SPREAD = 20
        const val RATING_DIVISOR = 10.0
        val BRANDS = listOf("Lakmé", "Maybelline", "Nykaa Cosmetics", "Minimalist", "The Ordinary", "Plum")
        val CATEGORIES = listOf("Lipstick", "Serum", "Moisturiser", "Kajal", "Sunscreen", "Face wash")
    }
}
