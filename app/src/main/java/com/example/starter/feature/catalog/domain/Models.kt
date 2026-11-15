package com.example.starter.feature.catalog.domain

data class Product(
    val id: Int,
    val name: String,
    val brand: String,
    /** Price in rupees. Use a money type / paise in production. */
    val price: Int,
    val rating: Double
)

data class CartLine(val product: Product, val quantity: Int) {
    val subtotal: Int get() = product.price * quantity
}

data class Cart(val lines: List<CartLine> = emptyList()) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val total: Int get() = lines.sumOf { it.subtotal }
    fun quantityOf(productId: Int): Int = lines.firstOrNull { it.product.id == productId }?.quantity ?: 0
}

enum class SortOption { RELEVANCE, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, RATING }

data class ProductFilter(
    val brands: Set<String> = emptySet(),
    /** Inclusive upper bound; null = no limit. */
    val maxPrice: Int? = null,
    val sort: SortOption = SortOption.RELEVANCE
) {
    val isActive: Boolean get() = brands.isNotEmpty() || maxPrice != null || sort != SortOption.RELEVANCE
}
