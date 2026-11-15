package com.example.starter.feature.catalog.data

import com.example.starter.feature.catalog.domain.Product
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InMemoryCartRepositoryTest {
    private val repo = InMemoryCartRepository()
    private val lipstick = Product(1, "Lipstick", "Plum", 300, 4.0)
    private val serum = Product(2, "Serum", "Minimalist", 600, 4.5)

    @Test
    fun `add never exceeds stock`() {
        assertThat(repo.add(lipstick, stock = 2)).isTrue()
        assertThat(repo.add(lipstick, stock = 2)).isTrue()
        assertThat(repo.add(lipstick, stock = 2)).isFalse()
        assertThat(repo.cart.value.quantityOf(1)).isEqualTo(2)
    }

    @Test
    fun `totals and decrement down to removal`() {
        repo.add(lipstick, 5)
        repo.add(lipstick, 5)
        repo.add(serum, 5)
        assertThat(repo.cart.value.itemCount).isEqualTo(3)
        assertThat(repo.cart.value.total).isEqualTo(1_200)
        repo.decrement(1)
        repo.decrement(1)
        assertThat(repo.cart.value.lines.map { it.product.id }).containsExactly(2)
    }

    @Test
    fun `reconcile clamps to live stock and drops sold-out lines`() {
        repeat(3) { repo.add(lipstick, 5) }
        repo.add(serum, 5)
        repo.reconcile(mapOf(1 to 1, 2 to 0))
        assertThat(repo.cart.value.lines).hasSize(1)
        assertThat(repo.cart.value.quantityOf(1)).isEqualTo(1)
    }
}
