package com.example.cafeteriaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.domain.model.LoginRequest
import com.example.cafeteriaapp.domain.model.RegistroRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val usuarioId: String, val nombre: String, val role: String) : LoginUiState
    data class Error(val error: String) : LoginUiState
}
sealed interface RegistroUiState {
    object Idle : RegistroUiState
    object Loading : RegistroUiState
    data class Success(val usuarioId: String, val mensaje: String) : RegistroUiState
    data class Error(val error: String) : RegistroUiState
}

class AuthViewModel : ViewModel() {

    val _registroState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
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

    //LPOGIN

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun loginEstudiante(correo: String, password: String) {
        if (correo.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState.Error("Por favor, llena todos los campos.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val response = RetrofitClient.apiService.loginUsuario(
                    LoginRequest(
                        correo.trim(),
                        password
                    )
                )
                val body = response.body()!!
                println("DEBUG LOGIN: El cuerpo del backend es: $body")
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    println("DEBUG LOGIN: El UUID extraído es: ${body.usuarioId}")
                    _loginState.value = LoginUiState.Success(body.usuarioId, body.nombreCompleto, body.role)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Credenciales incorrectas."
                    _loginState.value = LoginUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("Error de red: No hay conexión con el servidor.")
            }
        }
    }
}