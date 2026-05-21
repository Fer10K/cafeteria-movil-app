package com.example.cafeteriaapp.domain.model

import android.R
import java.util.UUID

enum class CoffeeCategory{
    BEBIDAS_CALIENTES,
    BEBIDAS_FRIAS,
    METODOS_EXTRACCION,
    REPOSTERIA
}
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Double,
    val category: CoffeeCategory,
    val stock: Int,
    val imageUrl: String? = null
){
    val isAvalible: Boolean get() = stock > 0
}