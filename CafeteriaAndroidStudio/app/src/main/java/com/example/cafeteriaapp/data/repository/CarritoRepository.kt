package com.example.cafeteriaapp.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cafeteriaapp.domain.model.ProductoModificado

object CarritoRepository {
    val items = mutableStateListOf<ProductoModificado>()

    // Añadir al carrito (si el mismo producto con los mismos extras ya existe, suma la cantidad)
    fun agregar(producto: ProductoModificado) {
        println("DEBUG: Intentando agregar ${producto.productoBase.nombre}")

        // Buscamos si ya existe exactamente el mismo producto con los mismos extras
        val existente = items.find { it ->
            val mismoId = it.productoBase.id == producto.productoBase.id
            val mismosExtras = it.extrasSeleccionados.map { e -> e.id ?: 0 }.sorted() ==
                    producto.extrasSeleccionados.map { e -> e.id ?: 0 }.sorted()
            mismoId && mismosExtras
        }

        if (existente != null) {
            println("DEBUG: Producto repetido encontrado, sumando cantidad")
            val index = items.indexOf(existente)
            items[index] = existente.copy(cantidad = existente.cantidad + producto.cantidad)
        } else {
            println("DEBUG: Producto nuevo, agregando a la lista")
            items.add(producto)
        }

        println("DEBUG: Ahora hay ${items.size} items en el repositorio")
    }

    fun eliminar(producto: ProductoModificado) {
        items.remove(producto)
    }

    fun limpiar() {
        items.clear()
    }

    val precioTotalCarrito: Double
        get() = items.sumOf { it.precioTotal }
}