package com.example.cafeteria.data.model

data class ProcesarCompraRequest(
    val usuario_id: String,
    val monto_total: Double,
    val cantidad_productos: Int,
    val es_primer_compra_dia: Boolean
)

// Estructura para el Logro
data class LogroDesbloqueado(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val icono_url: String?
)

// Lo que el Backend le va a responder a Android
data class ProcesarCompraResponse(
    val usuario_id: String,
    val xp_ganada: Int,
    val xp_actual: Int,
    val nivel_actual: Int,
    val subio_de_nivel: Boolean,
    val logros_nuevos: List<LogroDesbloqueado>
)