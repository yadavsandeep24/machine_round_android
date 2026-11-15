package com.example.starter.feature.catalog.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApplyProductFilterTest {
    private val products = listOf(
        Product(1, "A", "Plum", 300, 4.1),
        Product(2, "B", "Lakmé", 900, 4.8),
        Product(3, "C", "Plum", 1_200, 3.9),
        Product(4, "D", "Minimalist", 450, 4.5)
    )

    @Test
    fun `no filter keeps server order`() {
        assertThat(products.applyFilter(ProductFilter()).map { it.id }).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun `brands are OR-ed, price cap is inclusive, filters are AND-ed`() {
        val result = products.applyFilter(ProductFilter(brands = setOf("Plum", "Lakmé"), maxPrice = 900))
        assertThat(result.map { it.id }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `sorts`() {
        assertThat(
            products.applyFilter(ProductFilter(sort = SortOption.PRICE_LOW_TO_HIGH)).map {
                it.id
            }
        ).containsExactly(1, 4, 2, 3).inOrder()
        assertThat(
            products.applyFilter(ProductFilter(sort = SortOption.PRICE_HIGH_TO_LOW)).map {
                it.id
            }
        ).containsExactly(3, 2, 4, 1).inOrder()
        assertThat(products.applyFilter(ProductFilter(sort = SortOption.RATING)).map { it.id }).containsExactly(2, 4, 1, 3).inOrder()
    }
}
