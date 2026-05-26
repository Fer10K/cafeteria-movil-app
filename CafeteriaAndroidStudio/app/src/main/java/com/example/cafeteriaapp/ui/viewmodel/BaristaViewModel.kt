package com.example.cafeteriaapp.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteriaapp.domain.model.PedidoBaristaResponse
import com.example.cafeteriaapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


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
    private val _canceladosUiState = MutableStateFlow<HistorialUiState>(HistorialUiState.Loading)
    val canceladosUiState: StateFlow<HistorialUiState> = _canceladosUiState.asStateFlow()

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
                _canceladosUiState.value = HistorialUiState.Error("Error de conexión con el servidor.")
            }
        }
    }


    private val _entregadosUiState = MutableStateFlow<HistorialUiState>(HistorialUiState.Loading)
    val entregadosUiState: StateFlow<HistorialUiState> = _entregadosUiState.asStateFlow()

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
                _entregadosUiState.value = HistorialUiState.Error("Error de conexión con Parrot OS.")
            }
        }
    }



    private val _uiState = MutableStateFlow<BaristaUiState>(BaristaUiState.Loading)
    val uiState: StateFlow<BaristaUiState> = _uiState.asStateFlow()
    init {
        cargarPedidos()
    }

    fun cargarPedidos() {
        viewModelScope.launch {
            // Ponemos la pantalla en modo carga
            _uiState.value = BaristaUiState.Loading

            try {
                // Realizamos la petición HTTP
                val response = RetrofitClient.apiService.obtenerPedidosBarista()

                if (response.isSuccessful && response.body() != null) {
                    val listaPedidos = response.body()!!
                    android.util.Log.d("BARISTA_DEBUG", "Pedidos crudos llegados al ViewModel: ${listaPedidos.size}")
                    if (listaPedidos.isNotEmpty()) {
                        android.util.Log.d("BARISTA_DEBUG", "Primer pedido ID: ${listaPedidos[0].pedidoId} | Estado: ${listaPedidos[0].estado}")
                    }
                    _uiState.value = BaristaUiState.Success(pedidos = listaPedidos)
                } else {
                    _uiState.value = BaristaUiState.Error(
                        mensaje = "Error del servidor: ${response.code()} - No se pudieron obtener las comandas."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BaristaUiState.Error(
                    mensaje = "Error de conexión."
                )
            }
        }
    }
    fun avanzarEstadoPedido(pedidoId: String, estadoActual: String) {
        viewModelScope.launch {
            // Determinamos el siguiente estado lógico
            val siguienteEstado = when (estadoActual) {
                "PENDIENTE_PAGO" -> "PROCESANDO"
                "PROCESANDO" -> "LISTO"
                "LISTO" -> "ENTREGADO"
                else -> "CANCELADO"
            }

            try {
                val response = RetrofitClient.apiService.actualizarEstadoPedido(pedidoId, siguienteEstado)

                if (response.isSuccessful) {
                    // 🔄 Si la BD cambió con éxito, recargamos la lista
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
                    cargarPedidos() // Recarga y remueve la card del flujo activo
                }
            } catch (e: Exception) {
                // Manejo silencioso o log de error
            }
        }
    }
}