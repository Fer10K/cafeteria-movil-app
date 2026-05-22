package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.domain.model.HistorialCompraItem
import com.example.cafeteriaapp.domain.model.Producto
import com.example.cafeteriaapp.domain.model.RecomendacionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface GamificationUiState {
    object Idle : GamificationUiState
    object Loading : GamificationUiState
    data class Success(val data: ProcesarCompraResponse) : GamificationUiState
    data class Error(val message: String) : GamificationUiState
}


sealed interface AiUiState {
    object Idle : AiUiState
    object Loading : AiUiState
    data class Success(val sugerencia: String): AiUiState
    data class Error(val message: String) : AiUiState
}
class CafeteriaViewModel : ViewModel() {


    //ENVIAR AL BACK
    private val _uiState = MutableStateFlow<GamificationUiState>(GamificationUiState.Idle)
    val uiState: StateFlow<GamificationUiState> = _uiState

    fun enviarCompraAlBackend(usuarioId: String, monto: Double, cantidad: Int) {
        viewModelScope.launch {
            _uiState.value = GamificationUiState.Loading
            try {
                val requestData = ProcesarCompraRequest(
                    usuario_id = usuarioId,
                    monto_total = monto,
                    cantidad_productos = cantidad,
                    es_primer_compra_dia = false
                )

                val response = RetrofitClient.apiService.procesarCompra(requestData)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = GamificationUiState.Success(response.body()!!)
                } else {
                    _uiState.value = GamificationUiState.Error("Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = GamificationUiState.Error("Fallo de red: ${e.localizedMessage}")
            }
        }
    }

    //CONSULTA A LA IA
    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState

    fun pedirRecomendacionAMenu(usuarioId: String, historial: List<HistorialCompraItem>, disponibles: List<Producto>) {
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                val requestData = RecomendacionRequest(
                    usuario_id = usuarioId,
                    historial = historial,
                    productos_disponibles = disponibles
                )

                val response = RetrofitClient.apiService.obtenerRecomendacionIA(requestData)

                if (response.isSuccessful && response.body() != null) {
                    _aiState.value = AiUiState.Success(response.body()!!.recomendacion)
                } else {
                    _aiState.value = AiUiState.Error("Gemini no pudo procesar la recomendación.")
                }
            } catch (e: Exception) {
                _aiState.value = AiUiState.Error("Fallo de red al conectar con la IA: ${e.localizedMessage}")
            }
        }
    }
}