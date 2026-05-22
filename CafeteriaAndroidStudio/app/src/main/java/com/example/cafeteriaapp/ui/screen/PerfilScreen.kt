package com.example.cafeteria.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.ui.viewmodel.GamificationUiState
import com.example.cafeteriaapp.ui.viewmodel.CafeteriaViewModel

@Composable
fun DetalleCompraScreen(
    cafeteriaViewModel: CafeteriaViewModel = viewModel()
) {
    // Recolectamos el estado del flujo de red de forma reactiva
    val uiState by cafeteriaViewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Resumen de tu Compra", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        // El botón invoca el método del ViewModel pasándole los parámetros correspondientes
        Button(onClick = {
            cafeteriaViewModel.enviarCompraAlBackend(
                usuarioId = "63f28ccd-9173-40b2-b919-01b784eb148f",
                monto = 150.0,
                cantidad = 2
            )
        }) {
            Text("Simular Pago en Cafetería ☕")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reaccionamos de manera inteligente a lo que responda el Backend
        when (val state = uiState) {
            is GamificationUiState.Idle -> {
                Text("Listo para procesar transacciones.")
            }
            is GamificationUiState.Loading -> {
                CircularProgressIndicator() // Pantalla de carga mientras Docker responde
            }
            is GamificationUiState.Success -> {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("¡Pago Procesado Exitosamente!", color = MaterialTheme.colorScheme.primary)
                        Text("XP Actual: ${state.data.xp_actual}")
                        Text("Nivel del Alumno: ${state.data.nivel_actual}")

                        if (state.data.subio_de_nivel) {
                            Text("🎉 ¡FELICIDADES, SUBISTE DE NIVEL! 🎉", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            is GamificationUiState.Error -> {
                Text("Hubo un problema: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}