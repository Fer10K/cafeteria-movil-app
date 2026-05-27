package com.example.cafeteriaapp.domain.model

import com.google.gson.annotations.SerializedName

data class PedidoCreateRequest(
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("metodo_pago") val metodoPago: String, // "EFECTIVO" o "NFC"
    @SerializedName("estado") val estado: String = "PENDIENTE_PAGO",
    @SerializedName("items") val items: List<ItemPedidoRequest>
)

data class ItemPedidoRequest(
    @SerializedName("producto_id") val productoId: Int,
    @SerializedName("nombre_producto") val nombreProducto: String,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio_unitario_base") val precioUnitarioBase: Double,
    @SerializedName("extras") val extras: List<ExtraPedidoRequest>
)

data class ExtraPedidoRequest(
    @SerializedName("extra_id") val extraId: Int,
    @SerializedName("nombre_extra") val nombreExtra: String,
    @SerializedName("precio_adicional") val precioAdicional: Double
)

// ========================

data class PedidoResponse(
    @SerializedName("pedido_id") val pedidoId: String,
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("monto_total") val montoTotal: Double,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("mensaje") val mensaje: String
)


data class PedidoStatusResponse(
    val pedido_id: String,
    val estado: String,
    val mensaje: String
)
fun List<ProductoModificado>.toNetworkPayload(usuarioId: String, metodoPago: String): PedidoCreateRequest {
    val itemsRequest = this.map { itemLocal ->
        ItemPedidoRequest(
            productoId = itemLocal.productoBase.id ?: 0,
            nombreProducto = itemLocal.productoBase.nombre ?: "Producto",
            cantidad = itemLocal.cantidad,
            precioUnitarioBase = itemLocal.productoBase.precio ?: 0.0,
            extras = itemLocal.extrasSeleccionados.map { extraLocal ->
                ExtraPedidoRequest(
                    extraId = extraLocal.id ?: 0,
                    nombreExtra = extraLocal.nombre ?: "Extra",
                    precioAdicional = extraLocal.precioAdicional ?: 0.0
                )
            }
        )
    }

    return PedidoCreateRequest(
        usuarioId = usuarioId,
        metodoPago = metodoPago,
        items = itemsRequest
    )
}