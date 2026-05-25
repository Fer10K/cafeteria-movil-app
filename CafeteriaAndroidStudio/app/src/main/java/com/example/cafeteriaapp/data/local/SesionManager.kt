package com.example.cafeteriaapp.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "usuario_uuid"
    }

    fun guardarSesion(uuid: String) {
        prefs.edit().putString(KEY_USER_ID, uuid).apply()
    }

    fun obtenerSession(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun cerrarSesion() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }
}