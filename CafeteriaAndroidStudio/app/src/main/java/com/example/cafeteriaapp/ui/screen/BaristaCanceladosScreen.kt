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
fun BaristaCanceladosScreen(
    baristaViewModel: BaristaViewModel,
    onRegresar: () -> Unit
) {
    val uiState by baristaViewModel.canceladosUiState.collectAsState()

    LaunchedEffect(Unit) {
        baristaViewModel.cargarPedidosCancelados()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedidos Cancelados ❌", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onRegresar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HistorialUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { baristaViewModel.cargarPedidosCancelados() }) { Text("Reintentar") }
                    }
                }
                is HistorialUiState.Success -> {
                    if (state.pedidos.isEmpty()) {
                        Text(
                            text = "No hay registros de pedidos cancelados.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.pedidos, key = { it.pedidoId }) { pedido ->
                                CanceladoCard(pedido = pedido)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CanceladoCard(pedido: PedidoBaristaResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Pedido: ${pedido.pedidoId.take(6).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Cancelado",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Monto Perdido: $${String.format("%.2f", pedido.montoTotal)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(4.dp))

            pedido.productos.forEach { producto ->
                Text(text = "• ${producto.cantidad}x ${producto.nombreProducto}", style = MaterialTheme.typography.bodyMedium)
                producto.extras.forEach { extra ->
                    Text(text = "  - ${extra.nombreExtra}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}