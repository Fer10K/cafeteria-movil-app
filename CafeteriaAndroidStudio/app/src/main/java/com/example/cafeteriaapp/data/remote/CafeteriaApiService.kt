package com.example.cafeteriaapp.data.remote

import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteriaapp.domain.model.RecomendacionRequest
import com.example.cafeteriaapp.domain.model.RecomendacionResponse
import com.example.cafeteriaapp.domain.model.RegistroRequest
import com.example.cafeteriaapp.domain.model.RegistroResponse

import retrofit2.http.Body
import retrofit2.http.POST

interface CafeteriaApiService {

    // Definimos la ruta exacta que creamos en FastAPI para la gamificación
    @POST("gamificacion/procesar-compra")
    suspend fun procesarCompra(
        @Body request: ProcesarCompraRequest
    ): retrofit2.Response<ProcesarCompraResponse>

    @POST("ai/recomendar")
    suspend fun obtenerRecomendacionIA(
        @Body request: RecomendacionRequest
    ): retrofit2.Response<RecomendacionResponse>

    @POST("auth/registro")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): retrofit2.Response<RegistroResponse>
}