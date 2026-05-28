package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteriaapp.domain.model.PedidoBaristaResponse
import com.example.cafeteriaapp.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

sealed interface BaristaUiState {
    object Loading : BaristaUiState
    data class Success(val pedidos: List<PedidoBaristaResponse>) : BaristaUiState
    data class Error(val mensaje: String) : BaristaUiState
}

sealed interface HistorialUiState {
    data object Loading : HistorialUiState
    data class Success(val pedidos: List<PedidoBaristaResponse>) : HistorialUiState
    data class Error(val mensaje: String) : HistorialUiState
}

class BaristaViewModel : ViewModel() {

    // --- ESTADOS DE LA INTERFAZ (UI STATE) ---
    private val _uiState = MutableStateFlow<BaristaUiState>(BaristaUiState.Loading)
    val uiState: StateFlow<BaristaUiState> = _uiState.asStateFlow()

    private val _canceladosUiState = MutableStateFlow<HistorialUiState>(HistorialUiState.Loading)
    val canceladosUiState: StateFlow<HistorialUiState> = _canceladosUiState.asStateFlow()

    private val _entregadosUiState = MutableStateFlow<HistorialUiState>(HistorialUiState.Loading)
    val entregadosUiState: StateFlow<HistorialUiState> = _entregadosUiState.asStateFlow()

    private val _pedidosPorConfirmar = MutableStateFlow<List<PedidoBaristaResponse>>(emptyList())
    val pedidosPorConfirmar: StateFlow<List<PedidoBaristaResponse>> = _pedidosPorConfirmar.asStateFlow()

    // --- VARIABLES DE RED Y WEBSOCKET ---
    private var webSocket: WebSocket? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Mantiene la conexión WS abierta indefinidamente
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    // --- BLOQUE DE INICIALIZACIÓN ÚNICO ---
    init {
        android.util.Log.d("CAFETERIA_WS", "⚙️ BaristaViewModel Inicializado. Levantando servicios...")

        // 1. Cargar las comandas generales activas
        cargarPedidos()

        // 2. Encender el radar asíncrono del WebSocket en hilo secundario
        viewModelScope.launch(Dispatchers.IO) {
            conectarWebSocket()
        }
    }

    // --- FUNCIONES HTTP (RETROFIT) ---
    fun cargarPedidos() {
        viewModelScope.launch {
            _uiState.value = BaristaUiState.Loading
            try {
                // Modificado para usar tu función unificada que configuramos en el backend
                val response = RetrofitClient.apiService.obtenerPedidosBarista()
                if (response.isSuccessful && response.body() != null) {
                    val listaPedidos = response.body()!!
                    android.util.Log.d("BARISTA_DEBUG", "Pedidos crudos llegados: ${listaPedidos.size}")
                    _uiState.value = BaristaUiState.Success(pedidos = listaPedidos)
                } else {
                    _uiState.value = BaristaUiState.Error(
                        mensaje = "Error del servidor: ${response.code()} - No se pudieron obtener las comandas."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BaristaUiState.Error(mensaje = "Error de conexión con el servidor.")
            }
        }
    }

    fun cargarPedidosCancelados() {
        viewModelScope.launch {
            _canceladosUiState.value = HistorialUiState.Loading
            try {
                val response = RetrofitClient.apiService.obtenerPedidosCancelados()
                if (response.isSuccessful && response.body() != null) {
                    _canceladosUiState.value = HistorialUiState.Success(pedidos = response.body()!!)
                } else {
                    _canceladosUiState.value = HistorialUiState.Error("Error al obtener los cancelados: ${response.code()}")
                }
            } catch (e: Exception) {
                _canceladosUiState.value = HistorialUiState.Error("Error de conexión.")
            }
        }
    }

    fun cargarPedidosEntregados() {
        viewModelScope.launch {
            _entregadosUiState.value = HistorialUiState.Loading
            try {
                val response = RetrofitClient.apiService.obtenerPedidosEntregados()
                if (response.isSuccessful && response.body() != null) {
                    _entregadosUiState.value = HistorialUiState.Success(pedidos = response.body()!!)
                } else {
                    _entregadosUiState.value = HistorialUiState.Error("Error al obtener los entregados: ${response.code()}")
                }
            } catch (e: Exception) {
                _entregadosUiState.value = HistorialUiState.Error("Error de conexión.")
            }
        }
    }

    fun avanzarEstadoPedido(pedidoId: String, estadoActual: String) {
        viewModelScope.launch {
            val siguienteEstado = when (estadoActual) {
                "PENDIENTE_PAGO" -> "PROCESANDO"
                "PROCESANDO" -> "LISTO"
                "LISTO" -> "ENTREGADO"
                else -> "CANCELADO"
            }

            try {
                val response = RetrofitClient.apiService.actualizarEstadoPedido(pedidoId, siguienteEstado)
                if (response.isSuccessful) {
                    // Actualiza el flujo principal para refrescar los carriles de la UI
                    cargarPedidos()
                } else {
                    _uiState.value = BaristaUiState.Error("No se pudo actualizar el estado en el servidor.")
                }
            } catch (e: Exception) {
                _uiState.value = BaristaUiState.Error("Error de red al intentar cambiar el estado.")
            }
        }
    }

    fun cancelarPedido(pedidoId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.actualizarEstadoPedido(pedidoId, "CANCELADO")
                if (response.isSuccessful) {
                    cargarPedidos()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CONFIGURACIÓN Y CLIENTE DEL WEBSOCKET ---
    private fun conectarWebSocket() {
        android.util.Log.d("CAFETERIA_WS", "📡 Intentando abrir túnel WebSocket...")

        val request = Request.Builder()
            .url("wss://cafeteria-movil-app.onrender.com/ws/baristas") // 💡 Recuerda colocar tu URL real de Render aquí
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("CAFETERIA_WS", "🚀 ¡WebSocket conectado con éxito a Render!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("CAFETERIA_WS", "☕ Mensaje recibido desde Render: $text")

                cargarPedidos()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("CAFETERIA_WS", "❌ Fallo en la conexión de Render: ${t.message}")
                t.printStackTrace()

                // Intento de reconexión automática tras 5 segundos en hilo IO
                viewModelScope.launch(Dispatchers.IO) {
                    delay(5000)
                    android.util.Log.d("CAFETERIA_WS", "🔄 Reintentando conectar...")
                    conectarWebSocket()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.w("CAFETERIA_WS", "⚠️ El servidor está cerrando el canal: $reason")
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    override fun onCleared() {
        super.onCleared()
        // Cierre limpio para evitar fugas de memoria en la app
        webSocket?.close(1000, "Pantalla destruida")
    }
}