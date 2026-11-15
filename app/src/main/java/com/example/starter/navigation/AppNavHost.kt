@file:Suppress("MatchingDeclarationName") // routes live next to the graph

package com.example.starter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starter.feature.catalog.ui.cart.CartRoute
import com.example.starter.feature.catalog.ui.list.ProductListRoute
import kotlinx.serialization.Serializable

@Serializable data object ProductListDestination

@Serializable data object CartDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ProductListDestination) {
        composable<ProductListDestination> { ProductListRoute(onOpenCart = { navController.navigate(CartDestination) }) }
        composable<CartDestination> { CartRoute(onBack = { navController.popBackStack() }) }
    }
}
