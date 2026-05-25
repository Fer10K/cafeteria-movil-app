package com.example.cafeteriaapp.data.remote

import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteriaapp.domain.model.RecomendacionRequest
import com.example.cafeteriaapp.domain.model.RecomendacionResponse
import com.example.cafeteriaapp.domain.model.RegistroRequest
import com.example.cafeteriaapp.domain.model.RegistroResponse
import com.example.cafeteriaapp.domain.model.LoginRequest
import com.example.cafeteriaapp.domain.model.LoginResponse
import com.example.cafeteriaapp.domain.model.PedidoCreateRequest
import com.example.cafeteriaapp.domain.model.PedidoResponse
import com.example.cafeteriaapp.domain.model.ProductoResponse
import com.example.cafeteriaapp.ui.screen.PedidoStatusResponse
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST( "/auth/login")
    suspend fun loginUsuario(
        @Body request: LoginRequest
    ): retrofit2.Response<LoginResponse>

    @GET("productos")
    suspend fun obtenerProductos(): Response<List<ProductoResponse>>

    @POST("pedidos")
    suspend fun crearPedido(
        @Body pedido: PedidoCreateRequest
    ): PedidoResponse


    @GET("pedidos/{pedidoId}/status")
    suspend fun verificarEstadoPedido(
        @Path("pedidoId") pedidoId: String
    ):Response<PedidoStatusResponse>

    @GET("gamificacion/perfil/{usuario_id}")
    suspend fun obtenerPerfilGamificacion(
        @Path("usuario_id") usuarioId: String
    ): Response<ProcesarCompraResponse>
}