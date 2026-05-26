package com.example.cafeteriaapp.data.remote

import com.example.cafeteria.data.model.PosicionLeaderboard
import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteria.data.model.RecomendacionRequest
import com.example.cafeteria.data.model.RecomendacionResponse
import com.example.cafeteriaapp.domain.model.RegistroRequest
import com.example.cafeteriaapp.domain.model.RegistroResponse
import com.example.cafeteriaapp.domain.model.LoginRequest
import com.example.cafeteriaapp.domain.model.LoginResponse
import com.example.cafeteriaapp.domain.model.PedidoBaristaResponse
import com.example.cafeteriaapp.domain.model.PedidoCreateRequest
import com.example.cafeteriaapp.domain.model.PedidoResponse
import com.example.cafeteriaapp.domain.model.ProductoResponse
import com.example.cafeteriaapp.ui.screen.PedidoStatusResponse
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CafeteriaApiService {

    // Definimos la ruta exacta que creamos en FastAPI para la gamificación
    @POST("gamificacion/procesar-compra")
    suspend fun procesarCompra(
        @Body request: ProcesarCompraRequest
    ): retrofit2.Response<ProcesarCompraResponse>

    @POST("ai/recomendar")
    suspend fun obtenerRecomendacionIa(
        @Body payload: RecomendacionRequest
    ): Response<RecomendacionResponse>

    @POST("auth/registro")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): retrofit2.Response<RegistroResponse>

    @POST( "auth/login")
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

    @GET("gamificacion/leaderboard/{usuario_id}")
    suspend fun obtenerLeaderboard(
        @Path("usuario_id") usuarioId: String
    ): Response<List<PosicionLeaderboard>>


    @GET("pedidos/usuario/{usuario_id}/historial-nombres")
    suspend fun obtenerPedidosUsuario(
        @Path("usuario_id") usuarioId: String
    ): Response<List<String>>

    @GET("barista/pedidos")
    suspend fun obtenerPedidosBarista(): Response<List<PedidoBaristaResponse>>

    @PATCH("barista/pedidos/{pedido_id}/estado")
    suspend fun actualizarEstadoPedido(
        @Path("pedido_id") pedidoId: String,
        @Query("nuevo_estado") nuevoEstado: String
    ): Response<Unit>

    @GET("barista/pedidos/entregados")
    suspend fun obtenerPedidosEntregados(): Response<List<PedidoBaristaResponse>>

    @GET("barista/pedidos/cancelados")
    suspend fun obtenerPedidosCancelados(): Response<List<PedidoBaristaResponse>>

}