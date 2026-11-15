package com.example.starter.feature.catalog.ui.list

import com.example.starter.core.mvi.MviState
import com.example.starter.core.mvi.UiEffect
import com.example.starter.core.mvi.UiIntent
import com.example.starter.core.ui.UiText
import com.example.starter.feature.catalog.domain.Cart
import com.example.starter.feature.catalog.domain.Product
import com.example.starter.feature.catalog.domain.ProductFilter
import com.example.starter.feature.catalog.domain.SortOption

data class ProductUi(val product: Product, val stock: Int, val inCart: Int) {
    val isSoldOut: Boolean get() = stock <= 0
    val isLowStock: Boolean get() = stock in 1..LOW_STOCK
    val canAddMore: Boolean get() = inCart < stock

    private companion object {
        const val LOW_STOCK = 2
    }
}

data class ProductListState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val allBrands: List<String> = emptyList(),
    val filter: ProductFilter = ProductFilter(),
    val products: List<ProductUi> = emptyList(),
    val cart: Cart = Cart()
) : MviState

sealed interface ProductListIntent : UiIntent {
    data object Retry : ProductListIntent
    data class ToggleBrand(val brand: String) : ProductListIntent
    data class SetMaxPrice(val maxPrice: Int?) : ProductListIntent
    data class SetSort(val sort: SortOption) : ProductListIntent
    data object ClearFilters : ProductListIntent
    data class AddToCart(val productId: Int) : ProductListIntent
    data class RemoveOne(val productId: Int) : ProductListIntent
    data object OpenCart : ProductListIntent
}

sealed interface ProductListEffect : UiEffect {
    data object NavigateToCart : ProductListEffect
    data class ShowMessage(val message: UiText) : ProductListEffect
}
