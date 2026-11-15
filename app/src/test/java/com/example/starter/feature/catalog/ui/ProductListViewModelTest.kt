package com.example.starter.feature.catalog.ui

import app.cash.turbine.test
import com.example.starter.R
import com.example.starter.core.ui.UiText
import com.example.starter.feature.catalog.data.CatalogRepositoryImpl
import com.example.starter.feature.catalog.data.FakeCatalogServer
import com.example.starter.feature.catalog.data.InMemoryCartRepository
import com.example.starter.feature.catalog.domain.SortOption
import com.example.starter.feature.catalog.ui.list.ProductListEffect
import com.example.starter.feature.catalog.ui.list.ProductListIntent
import com.example.starter.feature.catalog.ui.list.ProductListViewModel
import com.example.starter.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    // In the app the stock stream never ends; here it stops after 1 tick so runTest can finish.
    private val server = FakeCatalogServer(latencyMs = 100, stockTickMs = 10_000, maxTicks = 1)
    private val cart = InMemoryCartRepository()
    private fun vm() = ProductListViewModel(CatalogRepositoryImpl(server), cart)

    private fun TestScope.settle() {
        advanceTimeBy(200)
        runCurrent()
    }

    @Test
    fun `loads products once, with brands and live stock`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        settle()
        with(vm.state.value) {
            assertThat(isLoading).isFalse()
            assertThat(products).hasSize(36)
            assertThat(allBrands).hasSize(6)
            assertThat(products.first { it.product.id == 6 }.isSoldOut).isTrue() // id % 6 == 0 -> no stock
        }
    }

    @Test
    fun `filters and sort are applied to the combined state`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        settle()
        vm.onIntent(ProductListIntent.ToggleBrand("Plum"))
        vm.onIntent(ProductListIntent.SetSort(SortOption.PRICE_LOW_TO_HIGH))
        runCurrent()
        val prices = vm.state.value.products.map { it.product.price }
        assertThat(vm.state.value.products.all { it.product.brand == "Plum" }).isTrue()
        assertThat(prices).isInOrder()
        vm.onIntent(ProductListIntent.ClearFilters)
        runCurrent()
        assertThat(vm.state.value.products).hasSize(36)
    }

    @Test
    fun `adding beyond stock emits a message`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        settle()
        vm.effects.test {
            vm.onIntent(ProductListIntent.AddToCart(1)) // id 1 has 1 unit
            vm.onIntent(ProductListIntent.AddToCart(1))
            assertThat(awaitItem()).isEqualTo(ProductListEffect.ShowMessage(UiText.Res(R.string.catalog_stock_limit)))
        }
        assertThat(vm.state.value.cart.quantityOf(1)).isEqualTo(1)
    }

    @Test
    fun `real-time stock drop reconciles the cart`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        settle()
        repeat(4) { vm.onIntent(ProductListIntent.AddToCart(5)) } // id 5 has 5 units
        runCurrent()
        assertThat(cart.cart.value.quantityOf(5)).isEqualTo(4)

        server.setStock(5, 2) // someone else bought 3
        advanceTimeBy(10_001) // next push from the stream
        runCurrent()
        assertThat(cart.cart.value.quantityOf(5)).isLessThan(3)
    }

    @Test
    fun `load failure shows error, retry recovers`() = runTest(mainRule.dispatcher) {
        server.failuresRemaining = 1
        val vm = vm()
        settle()
        assertThat(vm.state.value.error).isEqualTo(UiText.Dynamic("Simulated network failure"))
        vm.onIntent(ProductListIntent.Retry)
        settle()
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.products).isNotEmpty()
    }
}
