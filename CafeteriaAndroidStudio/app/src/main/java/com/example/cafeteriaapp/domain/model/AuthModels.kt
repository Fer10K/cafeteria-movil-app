package com.example.cafeteriaapp.domain.model

import com.google.gson.annotations.SerializedName

data class RegistroRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("nombre_completo") val nombreCompleto: String
)

data class RegistroResponse(
    @SerializedName("usuario_id") val usuarioId: String,
    @SerializedName("mensaje") val mensaje: String
)

data class LoginRequest(
    val correo: String,
    val password: String)
data class LoginResponse(
    val usuario_id: String,
    val nombre_completo: String,
    val correo: String,
    val mensaje: String
)