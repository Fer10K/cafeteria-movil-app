package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.domain.model.RegistroRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RegistroUiState {
    object Idle : RegistroUiState
    object Loading : RegistroUiState
    data class Success(val usuarioId: String, val mensaje: String) : RegistroUiState
    data class Error(val error: String) : RegistroUiState
}

class AuthViewModel : ViewModel() {

    private val _registroState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val registroState: StateFlow<RegistroUiState> = _registroState

    fun registrarEstudiante(nombre: String, correo: String, password: String) {
        if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
            _registroState.value = RegistroUiState.Error("Por favor, llena todos los campos.")
            return
        }

        if (password.length < 6) {
            _registroState.value = RegistroUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        viewModelScope.launch {
            _registroState.value = RegistroUiState.Loading
            try {
                val request = RegistroRequest(
                    correo = correo.trim(),
                    password = password,
                    nombreCompleto = nombre.trim()
                )

                val response = RetrofitClient.apiService.registrarUsuario(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _registroState.value = RegistroUiState.Success(body.usuarioId, body.mensaje)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido en el servidor"
                    _registroState.value = RegistroUiState.Error(errorBody)
                }
            } catch (e: Exception) {
                _registroState.value = RegistroUiState.Error("Fallo de red: No se pudo conectar al servidor.")
            }
        }
    }

    fun resetState() {
        _registroState.value = RegistroUiState.Idle
    }
}