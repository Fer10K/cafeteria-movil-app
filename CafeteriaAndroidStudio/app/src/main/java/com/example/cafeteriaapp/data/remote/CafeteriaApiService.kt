package com.example.cafeteriaapp.data.remote

import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CafeteriaApiService {

    // Definimos la ruta exacta que creamos en FastAPI para la gamificación
    @POST("gamificacion/procesar-compra")
    suspend fun procesarCompra(
        @Body request: ProcesarCompraRequest
    ): retrofit2.Response<ProcesarCompraResponse>
}