package com.example.cafeteriaapp.domain.repository

import com.example.cafeteriaapp.domain.model.Producto
import com.example.cafeteriaapp.domain.model.ProductoResponse
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getMenu(): Flow<List<ProductoResponse>>

    fun getProductById(id: String): Flow<ProductoResponse?>

    suspend fun updateProductStock(productId: String, newStock: Int)
}