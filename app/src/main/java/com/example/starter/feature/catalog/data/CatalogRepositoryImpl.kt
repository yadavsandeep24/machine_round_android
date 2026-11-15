package com.example.starter.feature.catalog.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.safeCall
import com.example.starter.feature.catalog.domain.CatalogRepository
import com.example.starter.feature.catalog.domain.Product
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CatalogRepositoryImpl @Inject constructor(private val server: FakeCatalogServer) : CatalogRepository {
    override suspend fun products(): AppResult<List<Product>> = safeCall { server.fetchProducts() }

    override fun observeStock(): Flow<Map<Int, Int>> = server.stockStream()
}
