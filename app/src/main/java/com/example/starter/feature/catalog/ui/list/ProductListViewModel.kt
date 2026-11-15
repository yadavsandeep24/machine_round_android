package com.example.starter.feature.catalog.ui.list

import androidx.lifecycle.viewModelScope
import com.example.starter.R
import com.example.starter.core.common.AppResult
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.UiText
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.catalog.domain.CartRepository
import com.example.starter.feature.catalog.domain.CatalogRepository
import com.example.starter.feature.catalog.domain.Product
import com.example.starter.feature.catalog.domain.ProductFilter
import com.example.starter.feature.catalog.domain.applyFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Design answers baked in:
 *  - API is called in init (once per ViewModel), so rotation never re-fetches.
 *  - Four sources (products, filter, live stock, cart) are combined into ONE immutable state.
 *  - Live stock also reconciles the cart, so the cart can never hold more than is available.
 */
@HiltViewModel
class ProductListViewModel @Inject constructor(private val catalog: CatalogRepository, private val cartRepository: CartRepository) :
    MviViewModel<ProductListIntent, ProductListState, ProductListEffect>(ProductListState()) {

    private val products = MutableStateFlow<List<Product>>(emptyList())
    private val filter = MutableStateFlow(ProductFilter())
    private val stock = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            catalog.observeStock().collect { latest ->
                stock.value = latest
                cartRepository.reconcile(latest)
            }
        }
        viewModelScope.launch {
            combine(products, filter, stock, cartRepository.cart) { all, f, s, cart ->
                val visible = all.applyFilter(f).map { ProductUi(it, stock = s[it.id] ?: 0, inCart = cart.quantityOf(it.id)) }
                Triple(f, visible, cart)
            }.collect { (f, visible, cart) ->
                setState { copy(filter = f, products = visible, cart = cart) }
            }
        }
    }

    override fun handleIntent(intent: ProductListIntent) {
        when (intent) {
            ProductListIntent.Retry -> load()
            is ProductListIntent.ToggleBrand -> filter.update { f ->
                f.copy(brands = if (intent.brand in f.brands) f.brands - intent.brand else f.brands + intent.brand)
            }
            is ProductListIntent.SetMaxPrice -> filter.update { it.copy(maxPrice = intent.maxPrice) }
            is ProductListIntent.SetSort -> filter.update { it.copy(sort = intent.sort) }
            ProductListIntent.ClearFilters -> filter.value = ProductFilter()
            is ProductListIntent.AddToCart -> addToCart(intent.productId)
            is ProductListIntent.RemoveOne -> cartRepository.decrement(intent.productId)
            ProductListIntent.OpenCart -> sendEffect(ProductListEffect.NavigateToCart)
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = catalog.products()) {
                is AppResult.Success -> {
                    products.value = result.data
                    setState { copy(isLoading = false, allBrands = result.data.map { it.brand }.distinct().sorted()) }
                }
                is AppResult.Failure -> setState { copy(isLoading = false, error = result.throwable.toUserMessage()) }
            }
        }
    }

    private fun addToCart(productId: Int) {
        val product = products.value.firstOrNull { it.id == productId } ?: return
        val added = cartRepository.add(product, stock = stock.value[productId] ?: 0)
        if (!added) sendEffect(ProductListEffect.ShowMessage(UiText.Res(R.string.catalog_stock_limit)))
    }
}
