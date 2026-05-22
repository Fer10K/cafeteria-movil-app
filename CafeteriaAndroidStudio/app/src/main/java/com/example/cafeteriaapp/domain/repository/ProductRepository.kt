package com.example.cafeteriaapp.domain.repository

import com.example.cafeteriaapp.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getMenu(): Flow<List<Product>>

    fun getProductById(id: String): Flow<Product?>

    suspend fun updateProductStock(productId: String, newStock: Int)
}