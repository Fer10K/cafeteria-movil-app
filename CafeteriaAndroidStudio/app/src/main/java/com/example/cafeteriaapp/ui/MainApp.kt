package com.example.cafeteriaapp.ui

import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeteriaapp.data.local.SessionManager
import com.example.cafeteriaapp.ui.screen.BaristaHomeScreen
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
    const val BARISTA_HOME = "barista_home"
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
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel._registroState.collectAsState()

    val uuid = sessionManager.obtenerSession()
    val rol = sessionManager.obtenerRol()

    LaunchedEffect(uuid, rol) {
        Log.d("CAFETERIA_DEBUG", "UUID actual en Preferencias: $uuid")
        Log.d("CAFETERIA_DEBUG", "ROL actual en Preferencias: $rol")
    }

    val rutaInicial = when {
        uuid.isNullOrEmpty() -> Destinos.LOGIN
        rol == "barista" -> Destinos.BARISTA_HOME // Desvía al Barista si su rol coincide
        else -> Destinos.HOME
    }

    NavHost(
        navController = navController,
        startDestination = rutaInicial
    ) {
        // --- PANTALLA DE LOGIN ---
        composable(Destinos.LOGIN) {
            LoginScreen(
                onLoginExitoso = { uuidObtenido, rolObtenido ->
                    sessionManager.guardarSession(uuidObtenido, rolObtenido)

                    if (sessionManager.obtenerRol() == "barista") {
                        navController.navigate(Destinos.BARISTA_HOME) {
                            popUpTo(Destinos.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Destinos.HOME) {
                            popUpTo(Destinos.LOGIN) { inclusive = true }
                        }
                    }
                },
                onIrARegistro = {
                    navController.navigate(Destinos.REGISTRO)
                }
            )
        }

        // -- PANTALLA DE REGISTRO ---
        composable(Destinos.REGISTRO) {
            RegistroScreen(
                onRegistroExitoso = { usuarioId ->
                    navController.navigate(Destinos.LOGIN) {
                        popUpTo(Destinos.REGISTRO) { inclusive = true }
                    }
                }
            )
        }

        // --- PANTALLA PRINCIPAL CLIENTE ---
        composable(Destinos.HOME) {
            MenuScreen(
                onIrAlCarritoClick = { navController.navigate(Destinos.CARRITO) },
                onIrAGamificacionClick = { navController.navigate(Destinos.GAMIFICACION) },
                onIrAAjustesClick = { navController.navigate(Destinos.AJUSTES) }
            )
        }

        // --- ☕ PANTALLA PRINCIPAL BARISTA ---
        composable(Destinos.BARISTA_HOME) {
            BaristaHomeScreen(
                onIrAAjustesClick = { navController.navigate(Destinos.AJUSTES) }
            )
        }

        // --- AJUSTES ---
        composable(Destinos.AJUSTES) {
            SettingsScreen(
                onCerrarSesion = {
                    sessionManager.cerrarSession()
                    navController.navigate(Destinos.LOGIN) {
                        popUpTo(0) { inclusive = true }
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

        // --- PANTALLA DE GAMIFICACIÓN ---
        composable(Destinos.GAMIFICACION) {
            val usuarioId = sessionManager.obtenerSession() ?: ""
            com.example.cafeteriaapp.ui.screen.PerfilScreen(
                usuarioId = usuarioId
            )
        }
    }
}