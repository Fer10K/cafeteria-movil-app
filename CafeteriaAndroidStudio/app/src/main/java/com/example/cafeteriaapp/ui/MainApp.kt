package com.example.cafeteriaapp.ui

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeteriaapp.data.local.SessionManager
import com.example.cafeteriaapp.ui.screen.CartScreen
import com.example.cafeteriaapp.ui.screen.LoginScreen
import com.example.cafeteriaapp.ui.screen.MenuScreen
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel
import androidx.lifecycle.ViewModelProvider

object Destinos {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val CARRITO = "carrito"
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val menuViewModel: MenuViewModel = viewModel()

    val rutaInicial = if (sessionManager.obtenerSession() != null) Destinos.HOME else Destinos.LOGIN

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
                onIrAlRegistro = {
                    navController.navigate(Destinos.REGISTRO)
                }
            )
        }

        // --- PANTALLA PRINCIPAL
        composable(Destinos.HOME) {
            MenuScreen(
                onIrAlCarritoClick = { navController.navigate("carrito") }
            )
        }
        composable(Destinos.CARRITO) {
            CartScreen(
                navController = navController,
                menuViewModel = menuViewModel
            )
        }
    }
}