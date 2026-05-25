package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteria.data.model.PosicionLeaderboard
import com.example.cafeteria.data.model.ProcesarCompraRequest
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteria.data.model.ProductoDisponibleSchema
import com.example.cafeteria.data.model.RecomendacionRequest
import com.example.cafeteriaapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 🛒 Estado para cuando el usuario realiza un pago/compra en la cafetería
sealed interface CompraUiState {
    object Idle : CompraUiState
    object Loading : CompraUiState
    data class Success(val data: ProcesarCompraResponse) : CompraUiState
    data class Error(val message: String) : CompraUiState
}

sealed interface PerfilUiState {
    object Loading : PerfilUiState
    data class Success(
        val xpPropia: Int,
        val nivelPropio: Int,
        val leaderboard: List<PosicionLeaderboard>
    ) : PerfilUiState
    data class Error(val message: String) : PerfilUiState
}

// 🤖 Estado para las recomendaciones de la IA
sealed interface AiUiState {
    object Idle : AiUiState
    object Loading : AiUiState
    data class Success(val sugerencia: String): AiUiState
    data class Error(val message: String) : AiUiState
}

class CafeteriaViewModel : ViewModel() {

    // 2. FLUJO DE CONSULTA DEL PERFIL (Para PerfilScreen)
    private val _perfilState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val perfilState: StateFlow<PerfilUiState> = _perfilState.asStateFlow()

//FLUJO PARA LA IA
    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    fun cargarPerfilGamificacion(usuarioId: String) {
        viewModelScope.launch {
            _perfilState.value = PerfilUiState.Loading
            try {
                val resPerfil = RetrofitClient.apiService.obtenerPerfilGamificacion(usuarioId)
                val resLeaderboard = RetrofitClient.apiService.obtenerLeaderboard(usuarioId)

                if (resPerfil.isSuccessful && resPerfil.body() != null && resLeaderboard.isSuccessful) {
                    val perfilPropio = resPerfil.body()!!
                    val listaLeaderboard = resLeaderboard.body() ?: emptyList()

                    _perfilState.value = PerfilUiState.Success(
                        xpPropia = perfilPropio.xp_actual,
                        nivelPropio = perfilPropio.nivel_actual,
                        leaderboard = listaLeaderboard
                    )
                    cargarEstrategiaGamificacion(
                        usuarioId = usuarioId,
                        miXpActual = perfilPropio.xp_actual,
                        listaLeaderboard = listaLeaderboard
                    )

                } else {
                    _perfilState.value = PerfilUiState.Error("Error al sincronizar datos del servidor.")
                }
            } catch (e: Exception) {
                _perfilState.value = PerfilUiState.Error(e.localizedMessage ?: "Fallo de red")
            }
        }
    }
    private fun cargarEstrategiaGamificacion(
        usuarioId: String,
        miXpActual: Int,
        listaLeaderboard: List<PosicionLeaderboard>
    ) {
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                // 1. Buscamos el índice del alumno en el leaderboard real
                val miIndice = listaLeaderboard.indexOfFirst { it.usuario_id == usuarioId }

                var xpSiguienteRival = miXpActual
                var nombreSiguienteRival = "el líder del podio"

                // Si miIndice es mayor a 0, significa que hay alguien arriba de nosotros (el índice disminuye hacia el 1er lugar)
                if (miIndice > 0) {
                    val rivalDirecto = listaLeaderboard[miIndice - 1]
                    xpSiguienteRival = rivalDirecto.xp_total
                    nombreSiguienteRival = rivalDirecto.nombre ?: "Compañero"
                }

                // 2. Traemos el menú en tiempo real adaptado a ProductoResponse (sin stock)
                val resProductos = RetrofitClient.apiService.obtenerProductos()
                val menuDisponible = if (resProductos.isSuccessful && resProductos.body() != null) {
                    resProductos.body()!!.map { prod ->
                        ProductoDisponibleSchema(
                            id = prod.id.toString(),
                            nombre = prod.nombre,
                            precio = prod.precio,
                            categoria = prod.categoriaNombre
                        )
                    }
                } else emptyList()

                // 3. Si el menú cargó correctamente, disparamos el payload maestro al Back
                if (menuDisponible.isNotEmpty()) {
                    val payload = RecomendacionRequest(
                        usuarioId = usuarioId,
                        tipoContexto = "gamificacion",
                        historial = emptyList(),
                        productosDisponibles = menuDisponible,
                        puntosUsuarioActual = miXpActual,
                        puntosSiguienteUsuario = xpSiguienteRival,
                        nombreSiguienteUsuario = nombreSiguienteRival
                    )

                    val response = RetrofitClient.apiService.obtenerRecomendacionIa(payload)
                    if (response.isSuccessful && response.body() != null) {
                        _aiState.value = AiUiState.Success(response.body()!!.recomendacion)
                    } else {
                        _aiState.value = AiUiState.Error("No se pudo obtener recomendación")
                    }
                } else {
                    _aiState.value = AiUiState.Error("Menú vacío")
                }

            } catch (e: Exception) {
                _aiState.value = AiUiState.Error(e.localizedMessage ?: "Fallo de conexión con la IA")
            }
        }
    }
    fun limpiarRecomendacion() {
        _aiState.value = AiUiState.Idle
    }
}

