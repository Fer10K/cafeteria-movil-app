package com.example.cafeteriaapp.ui

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeteriaapp.data.local.SessionManager
import com.example.cafeteriaapp.ui.screen.CartScreen
import com.example.cafeteriaapp.ui.screen.LoginScreen
import com.example.cafeteriaapp.ui.screen.MenuScreen
import com.example.cafeteriaapp.ui.screen.RegistroScreen
import com.example.cafeteriaapp.ui.screen.SettingsScreen
import com.example.cafeteriaapp.ui.viewmodel.AuthViewModel
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel

object Destinos {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val CARRITO = "carrito"
    const val GAMIFICACION = "gamificacion"
    const val AJUSTES = "ajustes"
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val menuViewModel: MenuViewModel = viewModel()

    val rutaInicial = if (sessionManager.obtenerSession() != null) Destinos.HOME else Destinos.LOGIN
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel._registroState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = rutaInicial
    ) {
        // --- PANTALLA DE LOGIN ---
        composable(Destinos.LOGIN) {
            LoginScreen(
                onLoginExitoso = { uuid ->
                    sessionManager.guardarSesion(uuid)
                    navController.navigate(Destinos.HOME) {
                        popUpTo(Destinos.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = {
                    // 🚀 El ruteo se resuelve aquí afuera, en el contenedor principal
                    navController.navigate(Destinos.REGISTRO)
                }
            )
        }

        // -- PANTALLA DE REGISTRO ---
        composable(Destinos.REGISTRO) {
            RegistroScreen(
                onRegistroExitoso = { usuarioId ->
                    navController.navigate(Destinos.LOGIN) {
                        popUpTo(Destinos.REGISTRO) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // --- PANTALLA PRINCIPAL
        composable(Destinos.HOME) {
            MenuScreen(
                onIrAlCarritoClick = { navController.navigate("carrito") },
                onIrAGamificacionClick = { navController.navigate(Destinos.GAMIFICACION) },
                onIrAAjustesClick = { navController.navigate(Destinos.AJUSTES) }
            )
        }

        ///AJUSTES
        composable(Destinos.AJUSTES) {
            SettingsScreen(
                onCerrarSesion = {
                    sessionManager.cerrarSesion()

                    navController.navigate(Destinos.LOGIN) {
                        popUpTo(Destinos.HOME) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // DE CARRITO -----
        composable(Destinos.CARRITO) {
            CartScreen(
                navController = navController,
                menuViewModel = menuViewModel
            )
        }

        // --- PANTALLA DE GAMIFICACIÓN---
        composable(Destinos.GAMIFICACION) {
            // 🔐 Extraemos de forma segura el UUID guardado en las preferencias locales
            val usuarioId = sessionManager.obtenerSession() ?: ""
            com.example.cafeteriaapp.ui.screen.PerfilScreen(
                usuarioId = usuarioId
            )
        }
    }
}