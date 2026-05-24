package com.example.cafeteriaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cafeteria.ui.screen.DetalleCompraScreen
import com.example.cafeteriaapp.ui.screen.LoginScreen
import com.example.cafeteriaapp.ui.screen.RecomendacionScreen
import com.example.cafeteriaapp.ui.screen.RegistroScreen
import com.example.cafeteriaapp.ui.theme.CafeteriaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafeteriaAppTheme {

                LoginScreen(
                    onLoginExitoso = { usuarioId ->
                        println("Login exitoso: $usuarioId")
                    },
                    onIrAlRegistro = {
                        println("Ir al registro")
                    }
                )

            }

        }
    }
}