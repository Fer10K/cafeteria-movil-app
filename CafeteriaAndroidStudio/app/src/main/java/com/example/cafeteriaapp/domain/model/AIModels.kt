package com.example.cafeteria.data.model

import com.google.gson.annotations.SerializedName

// Estructura simplificada para enviar el historial de compras o el carrito actual
data class HistorialCompraSchema(
    @SerializedName("producto_nombre") val productoNombre: String
)

// Estructura simplificada para enviar el menú/inventario disponible de la cafetería
data class ProductoDisponibleSchema(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("categoria") val categoria: String
)

// El esquema de petición maestro unificado que espera tu Backend
data class RecomendacionRequest(
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("tipo_contexto") val tipoContexto: String,
    @SerializedName("historial") val historial: List<HistorialCompraSchema> = emptyList(),
    @SerializedName("productos_disponibles") val productosDisponibles: List<ProductoDisponibleSchema> = emptyList(),

    // Campos opcionales específicos para el flujo de Gamificación
    @SerializedName("puntos_usuario_actual") val puntosUsuarioActual: Int = 0,
    @SerializedName("puntos_siguiente_usuario") val puntosSiguienteUsuario: Int = 0,
    @SerializedName("nombre_siguiente_usuario") val nombreSiguienteUsuario: String = ""
)

// El esquema de respuesta oficial con el texto limpio de Gemini
data class RecomendacionResponse(
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("recomendacion") val recomendacion: String
)