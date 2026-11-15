package com.example.starter.feature.catalog.domain

import com.example.starter.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CatalogRepository {
    suspend fun products(): AppResult<List<Product>>

    /** Real-time stock: productId -> units left. Emits whenever the server pushes a change. */
    fun observeStock(): Flow<Map<Int, Int>>
}

/**
 * App-wide cart (singleton). StateFlow, not SharedFlow: the cart is STATE (always has a current
 * value, new collectors need it immediately), not a one-off event.
 */
interface CartRepository {
    val cart: StateFlow<Cart>

    /** Adds one unit, never exceeding [stock]. Returns false if it couldn't add. */
    fun add(product: Product, stock: Int): Boolean
    fun decrement(productId: Int)
    fun remove(productId: Int)

    /** Stock dropped below what's in the cart (someone else bought it): clamp and drop sold-out lines. */
    fun reconcile(stock: Map<Int, Int>)
}
