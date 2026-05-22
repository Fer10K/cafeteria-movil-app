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