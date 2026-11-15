package com.example.starter.feature.catalog.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.ErrorView
import com.example.starter.feature.catalog.domain.SortOption

@Composable
fun ProductListRoute(onOpenCart: () -> Unit, viewModel: ProductListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductListEffect.NavigateToCart -> onOpenCart()
                is ProductListEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
            }
        }
    }
    ProductListScreen(state, snackbar, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(state: ProductListState, snackbar: SnackbarHostState, onIntent: (ProductListIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    IconButton(onClick = { onIntent(ProductListIntent.OpenCart) }) {
                        BadgedBox(badge = { if (state.cart.itemCount > 0) Badge { Text("${state.cart.itemCount}") } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.catalog_open_cart))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(ProductListIntent.Retry) }
                else -> {
                    FilterBar(state, onIntent)
                    if (state.products.isEmpty()) {
                        CenteredText(stringResource(R.string.catalog_no_match))
                    } else {
                        ProductList(state.products, onIntent)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(state: ProductListState, onIntent: (ProductListIntent) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.allBrands) { brand ->
            FilterChip(
                selected = brand in state.filter.brands,
                onClick = { onIntent(ProductListIntent.ToggleBrand(brand)) },
                label = { Text(brand) }
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        PRICE_CAPS.forEach { cap ->
            FilterChip(
                selected = state.filter.maxPrice == cap,
                onClick = { onIntent(ProductListIntent.SetMaxPrice(if (state.filter.maxPrice == cap) null else cap)) },
                label = { Text(stringResource(R.string.catalog_under_price, cap)) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        SortMenu(state.filter.sort) { onIntent(ProductListIntent.SetSort(it)) }
    }
    if (state.filter.isActive) {
        TextButton(onClick = { onIntent(ProductListIntent.ClearFilters) }, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.catalog_clear_filters))
        }
    }
}

@Composable
private fun SortMenu(current: SortOption, onSelect: (SortOption) -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text(stringResource(current.label())) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        SortOption.entries.forEach { option ->
            DropdownMenuItem(text = { Text(stringResource(option.label())) }, onClick = {
                onSelect(option)
                open = false
            })
        }
    }
}

private fun SortOption.label(): Int = when (this) {
    SortOption.RELEVANCE -> R.string.catalog_sort_relevance
    SortOption.PRICE_LOW_TO_HIGH -> R.string.catalog_sort_price_low
    SortOption.PRICE_HIGH_TO_LOW -> R.string.catalog_sort_price_high
    SortOption.RATING -> R.string.catalog_sort_rating
}

@Composable
private fun ProductList(products: List<ProductUi>, onIntent: (ProductListIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(products, key = { it.product.id }) { item -> ProductCard(item, onIntent) }
    }
}

@Composable
private fun ProductCard(item: ProductUi, onIntent: (ProductListIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                Text("₹${item.product.price} · ★ ${item.product.rating}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                when {
                    item.isSoldOut -> Text(stringResource(R.string.catalog_sold_out), color = MaterialTheme.colorScheme.error)
                    item.isLowStock -> Text(stringResource(R.string.catalog_only_left, item.stock), color = MaterialTheme.colorScheme.error)
                }
            }
            if (item.inCart == 0) {
                Button(onClick = { onIntent(ProductListIntent.AddToCart(item.product.id)) }, enabled = !item.isSoldOut) {
                    Text(stringResource(R.string.catalog_add))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onIntent(ProductListIntent.RemoveOne(item.product.id)) }) { Text("−") }
                    Text("${item.inCart}", Modifier.padding(horizontal = 8.dp))
                    OutlinedButton(onClick = { onIntent(ProductListIntent.AddToCart(item.product.id)) }, enabled = item.canAddMore) {
                        Text("+")
                    }
                }
            }
        }
    }
}

private val PRICE_CAPS = listOf(500, 1_000)
