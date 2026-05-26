package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.domain.model.PedidoBaristaResponse
import com.example.cafeteriaapp.ui.viewmodel.BaristaUiState
import com.example.cafeteriaapp.ui.viewmodel.BaristaViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaHomeScreen(
    baristaViewModel: BaristaViewModel = viewModel(),
    onIrAEntregados: () -> Unit,
    onIrACancelados: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val uiState by baristaViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Control de Barra ☕", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 🔘 BOTONES SUPERIORES (Historiales que abarcan el ancho completo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onIrAEntregados,
                    modifier = Modifier.weight(1.0F),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Entregados")
                }
                Button(
                    onClick = onIrACancelados,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Cancelados")
                }
            }

            // 📋 CUERPO DEL CONTENEDOR (Las 3 Columnas de Flujo)
            when (val state = uiState) {
                is BaristaUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is BaristaUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { baristaViewModel.cargarPedidos() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                is BaristaUiState.Success -> {
                    // Separamos los pedidos por estado dinámicamente
                    // Por ahora PENDIENTE_PAGO estará vacío hasta mapear el endpoint completo
                    val entrantes = state.pedidos.filter { it.estado == "PENDIENTE_PAGO" }
                    val enProceso = state.pedidos.filter { it.estado == "PROCESANDO" }
                    val listos = state.pedidos.filter { it.estado == "LISTO" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Columna 1: Entrantes
                        CarrilPedidos(
                            titulo = "Entrantes (${entrantes.size})",
                            pedidos = entrantes,
                            modifier = Modifier.weight(1f),
                            baristaViewModel = baristaViewModel
                        )

                        // Columna 2: En Proceso
                        CarrilPedidos(
                            titulo = "En Proceso (${enProceso.size})",
                            pedidos = enProceso,
                            modifier = Modifier.weight(1f),
                            baristaViewModel = baristaViewModel
                        )

                        // Columna 3: Listos
                        CarrilPedidos(
                            titulo = "Listos (${listos.size})",
                            pedidos = listos,
                            modifier = Modifier.weight(1f),
                            baristaViewModel = baristaViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarrilPedidos(
    titulo: String,
    pedidos: List<PedidoBaristaResponse>,
    modifier: Modifier,
    baristaViewModel: BaristaViewModel
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // El LazyColumn se encarga de recorrer las cards hacia arriba automáticamente si una se elimina
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pedidos, key = { it.pedidoId }) { pedido ->
                PedidoCard(pedido = pedido, baristaViewModel = baristaViewModel)
            }
        }
    }
}

@Composable
fun PedidoCard(
    pedido: PedidoBaristaResponse,
    baristaViewModel: BaristaViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Encabezado: Usuario y Total
            Text(
                text = "ID Usuario: ${pedido.usuarioId.take(8)}...", // Reemplazable por nombre_usuario después
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total: $${String.format("%.2f", pedido.montoTotal)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(4.dp))

            // Lista de Productos e ítems
            pedido.productos.forEach { producto ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "• ${producto.cantidad}x ${producto.nombreProducto}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    // Extras del producto
                    producto.extras.forEach { extra ->
                        Text(
                            text = "  - ${extra.nombreExtra}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🕹️ BOTONES INTERACTIVOS DE LA CARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Izquierdo: Siempre Cancela el pedido ("X")
                Button(
                    onClick = {
                        baristaViewModel.cancelarPedido(pedido.pedidoId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancelar", modifier = Modifier.size(20.dp))
                }

                // Botón Derecho: Avanza el estado según la columna donde se encuentre
                val parBotones = when (pedido.estado) {
                    "PENDIENTE_PAGO" -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Check)
                    "PROCESANDO" -> Pair(MaterialTheme.colorScheme.tertiary, Icons.Default.ArrowForward)
                    else -> Pair(androidx.compose.ui.graphics.Color(0xFF2E7D32), Icons.Default.DoneAll)
                }
                val colorBoton = parBotones.first
                val icono = parBotones.second

                Button(
                    onClick = {
                        baristaViewModel.avanzarEstadoPedido(pedido.pedidoId, pedido.estado)

                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorBoton),
                    modifier = Modifier.weight(1.4f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = "Siguiente Paso",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
