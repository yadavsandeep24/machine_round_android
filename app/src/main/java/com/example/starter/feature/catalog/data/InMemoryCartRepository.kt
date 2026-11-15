package com.example.starter.feature.catalog.data

import com.example.starter.feature.catalog.domain.Cart
import com.example.starter.feature.catalog.domain.CartLine
import com.example.starter.feature.catalog.domain.CartRepository
import com.example.starter.feature.catalog.domain.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory cart shared by every screen. Production: persist with Room and sync with the server cart. */
@Singleton
class InMemoryCartRepository @Inject constructor() : CartRepository {

    private val _cart = MutableStateFlow(Cart())
    override val cart: StateFlow<Cart> = _cart.asStateFlow()

    override fun add(product: Product, stock: Int): Boolean {
        var added = false
        _cart.update { cart ->
            val current = cart.quantityOf(product.id)
            if (current >= stock) return@update cart
            added = true
            if (current == 0) {
                cart.copy(lines = cart.lines + CartLine(product, 1))
            } else {
                cart.copy(lines = cart.lines.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it })
            }
        }
        return added
    }

    override fun decrement(productId: Int) = _cart.update { cart ->
        cart.copy(
            lines = cart.lines.mapNotNull { line ->
                when {
                    line.product.id != productId -> line
                    line.quantity > 1 -> line.copy(quantity = line.quantity - 1)
                    else -> null
                }
            }
        )
    }

    override fun remove(productId: Int) = _cart.update { cart ->
        cart.copy(lines = cart.lines.filterNot { it.product.id == productId })
    }

    override fun reconcile(stock: Map<Int, Int>) = _cart.update { cart ->
        cart.copy(
            lines = cart.lines.mapNotNull { line ->
                val available = stock[line.product.id] ?: return@mapNotNull line
                when {
                    available <= 0 -> null
                    line.quantity > available -> line.copy(quantity = available)
                    else -> line
                }
            }
        )
    }
}
