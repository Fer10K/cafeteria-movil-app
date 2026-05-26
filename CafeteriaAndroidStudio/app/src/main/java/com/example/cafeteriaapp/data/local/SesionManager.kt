package com.example.cafeteriaapp.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PruebaSession", Context.MODE_PRIVATE)

    fun guardarSession(uid: String, rol: String) {
        prefs.edit().apply {
            putString("KEY_USER_ID", uid)
            putString("KEY_ROL", rol)
            apply()
        }
    }

    fun obtenerSession(): String? {
        return prefs.getString("KEY_USER_ID", null)
    }

    fun obtenerRol(): String {
        return prefs.getString("KEY_ROL", "cliente") ?: "cliente"
    }

    fun cerrarSession() {
        prefs.edit().clear().apply()
    }
}