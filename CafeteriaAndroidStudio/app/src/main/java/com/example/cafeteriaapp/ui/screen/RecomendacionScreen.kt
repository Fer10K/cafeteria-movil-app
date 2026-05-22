package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.domain.model.Producto
import com.example.cafeteriaapp.domain.model.HistorialCompraItem
import com.example.cafeteriaapp.ui.viewmodel.CafeteriaViewModel
import com.example.cafeteriaapp.ui.viewmodel.AiUiState

@Composable
fun RecomendacionScreen(
    viewModel: CafeteriaViewModel = viewModel()
) {
    val aiState by viewModel.aiState.collectAsState()

    // Datos simulados locales para la prueba
    val historialEstudiante = listOf(
        HistorialCompraItem(productoNombre = "Café Americano", categoria = "Bebidas", fecha = "2026-05-22"),
        HistorialCompraItem(productoNombre = "Muffin de Chocolate", categoria = "Snacks", fecha = "2026-05-21")
    )

    val menuDelDia = listOf(
        Producto("1", "Café Americano", 35.0, stock = 5, categoria = "Bebidas"),
        Producto("3", "Capuccino Helado", 45.0, stock = 5, "Bebidas"),
        Producto("4", "Chilaquiles con Pollo", 65.0, stock = 5, "Comida"),
        Producto("5", "Galleta de Avena", 15.0, stock = 5, "Snacks")
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Recomendaciones de IA 🤖", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Pregúntale a Gemini qué te conviene desayunar hoy según tus gustos habituales.")
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.pedirRecomendacionAMenu(
                    usuarioId = "63f28ccd-9173-40b2-b919-01b784eb148fd", // Tu UUID de prueba
                    historial = historialEstudiante,
                    disponibles = menuDelDia
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Obtener Sugerencia Inteligente ✨")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Renderizado reactivo según la respuesta de Docker/Gemini
        when (val state = aiState) {
            is AiUiState.Idle -> {
                Text("Presiona el botón para despertar a la IA.")
            }
            is AiUiState.Loading -> {
                CircularProgressIndicator() // Spinner de carga mientras Gemini genera el texto
            }
            is AiUiState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sugerencia del Día:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.sugerencia, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is AiUiState.Error -> {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}