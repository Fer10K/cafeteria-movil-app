package com.example.cafeteriaapp.domain.model

import com.google.gson.annotations.SerializedName


data class OpcionExtraResponse(
    @SerializedName("id") val id: Int?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("precio_adicional") val precioAdicional: Double?,
    @SerializedName("disponible") val disponible: Boolean?
)

data class GrupoOpcionesResponse(
    @SerializedName("id") val id: Int?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("min_seleccion") val minSeleccion: Int?,
    @SerializedName("max_seleccion") val maxSeleccion: Int?,
    @SerializedName("opciones") val opciones: List<OpcionExtraResponse>?
)
data class ProductoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("disponible") val disponible: Boolean,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("categoria_id") val categoriaId: Int?,
    @SerializedName("categoria_nombre") val categoriaNombre: String,

    @SerializedName("grupos_opciones") val gruposOpciones: List<GrupoOpcionesResponse>?
)


data class ProductoModificado(
    val productoBase: ProductoResponse,
    val extrasSeleccionados: List<OpcionExtraResponse>,
    val cantidad: Int = 1
) {
    val precioTotal: Double
        get() {
            val base = productoBase.precio ?: 0.0
            val extras = extrasSeleccionados.sumOf { it.precioAdicional ?: 0.0 }
            return (base + extras) * cantidad
        }
}