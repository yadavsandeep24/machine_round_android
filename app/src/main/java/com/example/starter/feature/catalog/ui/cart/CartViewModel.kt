package com.example.starter.feature.catalog.ui.cart

import androidx.lifecycle.ViewModel
import com.example.starter.feature.catalog.domain.Cart
import com.example.starter.feature.catalog.domain.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** MVVM: the cart screen just mirrors the shared cart and forwards actions. */
@HiltViewModel
class CartViewModel @Inject constructor(private val cartRepository: CartRepository) : ViewModel() {
    val cart: StateFlow<Cart> = cartRepository.cart

    fun decrement(productId: Int) = cartRepository.decrement(productId)

    fun remove(productId: Int) = cartRepository.remove(productId)
}
