package com.example.cafeteriaapp.domain.model

import com.google.gson.annotations.SerializedName

data class Producto(
    val id: String,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val categoria: String
)

data class HistorialCompraItem(
    @SerializedName("producto_nombre")
    val productoNombre: String,

    @SerializedName("categoria")
    val categoria: String,

    @SerializedName("fecha")
    val fecha: String
)

data class RecomendacionRequest(
    @SerializedName("usuario_id")
    val usuario_id: String,

    @SerializedName("historial")
    val historial: List<HistorialCompraItem>,

    @SerializedName("productos_disponibles")
    val productos_disponibles: List<Producto>
)

data class RecomendacionResponse(
    @SerializedName("usuario_id")
    val usuario_id: String,

    @SerializedName("recomendacion")
    val recomendacion: String
)