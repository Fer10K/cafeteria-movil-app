package com.example.cafeteriaapp.domain.model

import com.google.gson.annotations.SerializedName

// 1. Maestro: Estructura principal de la comanda
data class PedidoBaristaResponse(
    @SerializedName("pedido_id") val pedidoId: String,
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("monto_total") val montoTotal: Double,
    @SerializedName("productos") val productos: List<ItemPedidoResponse>
)

// 2. Detalle: Cada producto dentro de la comanda
data class ItemPedidoResponse(
    @SerializedName("pedido_item_id") val pedidoItemId: Int,
    @SerializedName("nombre_producto") val nombreProducto: String,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("extras") val extras: List<ExtraResponse>
)

// 3. Subdetalle: Los agregados/modificaciones del producto
data class ExtraResponse(
    @SerializedName("nombre_extra") val nombreExtra: String
)