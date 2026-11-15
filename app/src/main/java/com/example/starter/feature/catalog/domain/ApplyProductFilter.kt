package com.example.starter.feature.catalog.domain

/**
 * Pure function: easy to unit-test, reusable on the server or in a worker.
 * Relevance keeps the server's order (the backend ranks), other sorts are stable.
 */
fun List<Product>.applyFilter(filter: ProductFilter): List<Product> {
    val filtered = filter { product ->
        (filter.brands.isEmpty() || product.brand in filter.brands) &&
            (filter.maxPrice == null || product.price <= filter.maxPrice)
    }
    return when (filter.sort) {
        SortOption.RELEVANCE -> filtered
        SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
        SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
        SortOption.RATING -> filtered.sortedByDescending { it.rating }
    }
}
