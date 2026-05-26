package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cafeteriaapp.domain.model.PedidoBaristaResponse
import com.example.cafeteriaapp.ui.viewmodel.BaristaViewModel
import com.example.cafeteriaapp.ui.viewmodel.HistorialUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaEntregadosScreen(
    baristaViewModel: BaristaViewModel,
    onRegresar: () -> Unit
) {
    val uiState by baristaViewModel.entregadosUiState.collectAsState()

    // Cada vez que se abre la pantalla, forzamos la recarga de los datos frescos
    LaunchedEffect(Unit) {
        baristaViewModel.cargarPedidosEntregados()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Entregados ✅", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onRegresar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is HistorialUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is HistorialUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { baristaViewModel.cargarPedidosEntregados() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is HistorialUiState.Success -> {
                    if (state.pedidos.isEmpty()) {
                        Text(
                            text = "No se han entregado productos en este turno todavía.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.pedidos, key = { it.pedidoId }) { pedido ->
                                HistorialCard(pedido = pedido)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorialCard(pedido: PedidoBaristaResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pedido: ${pedido.pedidoId.take(6).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Entregado",
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Total Cobrado: $${String.format("%.2f", pedido.montoTotal)}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(4.dp))

            pedido.productos.forEach { producto ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "• ${producto.cantidad}x ${producto.nombreProducto}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    producto.extras.forEach { extra ->
                        Text(
                            text = "  - ${extra.nombreExtra}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}