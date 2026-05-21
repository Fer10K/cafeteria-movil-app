package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteriaapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Representa los diferentes estados de la pantalla al interactuar con el backend
sealed interface GamificationUiState {
    object Idle : GamificationUiState
    object Loading : GamificationUiState
    data class Success(val data: ProcesarCompraResponse) : GamificationUiState
    data class Error(val message: String) : GamificationUiState
}

class CafeteriaViewModel : ViewModel() {

    // El estado privado mutable y el público inmutable que leerá la vista
    private val _uiState = MutableStateFlow<GamificationUiState>(GamificationUiState.Idle)
    val uiState: StateFlow<GamificationUiState> = _uiState

    fun enviarCompraAlBackend(usuarioId: String, monto: Double, cantidad: Int) {
        // viewModelScope amarra la corrutina al ciclo de vida del ViewModel de forma segura
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
                    // Informamos a la UI que todo salió bien mandando los datos de XP y Nivel
                    _uiState.value = GamificationUiState.Success(response.body()!!)
                } else {
                    _uiState.value = GamificationUiState.Error("Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                // Captura fallos de red (ej: el Docker está apagado o no hay internet)
                _uiState.value = GamificationUiState.Error("Fallo de red: ${e.localizedMessage}")
            }
        }
    }
}