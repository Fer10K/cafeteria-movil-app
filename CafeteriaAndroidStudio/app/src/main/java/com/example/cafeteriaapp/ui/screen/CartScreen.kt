package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.data.repository.CarritoRepository
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel
import kotlinx.coroutines.delay


// Agrégalo al final de tu archivo CartScreen.kt (Fuera del Composable)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val itemsCarrito = CarritoRepository.items
    val total = CarritoRepository.precioTotalCarrito

    var mostrarOpcionesPago by remember { mutableStateOf(false) }
    var procesandoNFC by remember { mutableStateOf(false) }
    var pagoExitoso by remember { mutableStateOf(false) }
    var procesandoEfectivo by remember { mutableStateOf(false) }
    var idPedidoCreado by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    if (procesandoNFC) {
        LaunchedEffect(Unit) {
            delay(3000)
            procesandoNFC = false
            pagoExitoso = true
        }
    }

    if (pagoExitoso) {
        LaunchedEffect(Unit) {
            delay(2000)
            CarritoRepository.limpiar()
            pagoExitoso = false
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Carrito", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (itemsCarrito.isEmpty()) {
                // --- ESTADO VACÍO ---
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tu carrito está vacío", fontSize = 20.sp, color = Color.Gray)
                }
            } else {
                // --- LISTA DE PRODUCTOS ---
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(itemsCarrito) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.productoBase.nombre} x${item.cantidad}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    if (item.extrasSeleccionados.isNotEmpty()) {
                                        Text(
                                            text = item.extrasSeleccionados.joinToString(", ") { it.nombre ?: "" },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "$${String.format("%.2f", item.precioTotal)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                IconButton(onClick = { CarritoRepository.eliminar(item) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                // --- FOOTER CON BOTÓN CONFIRMAR ---
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total a pagar", fontSize = 14.sp)
                            Text(
                                text = "$${String.format("%.2f", total)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Button(
                            onClick = { mostrarOpcionesPago = true }, // 💡 Abre la selección de método de pago
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar Pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 1. Diálogo de selección del Método de Pago
    if (mostrarOpcionesPago) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesPago = false },
            title = { Text("Selecciona Método de Pago", fontWeight = FontWeight.Bold) },
            text = { Text("¿Cómo deseas realizar el pago de tu pedido en la cafetería?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarOpcionesPago = false
                        // Lanzamos la petición a la API
                        menuViewModel.enviarPedidoAlServidor(metodoPago = "NFC") { idGenerado ->
                            // Cuando la API responda con éxito, activamos la pantalla de espera NFC local
                            procesandoNFC = true
                        }
                    }
                ) {
                    Text("Pago con NFC")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        mostrarOpcionesPago = false
                        // Lanzamos la petición a la API
                        menuViewModel.enviarPedidoAlServidor(metodoPago = "EFECTIVO") { idGenerado ->
                            // Al responder con éxito, guardamos el ID real de la base de datos y empezamos el bucle de escucha
                            idPedidoCreado = idGenerado
                            procesandoEfectivo = true
                        }
                    }
                ) {
                    Text("Efectivo en Caja")
                }
            }
        )
    }

    // 2. Diálogo de Espera NFC (Simulación de lectura de hardware)
    if (procesandoNFC) {
        AlertDialog(
            onDismissRequest = { }, // Evita que el usuario lo cierre tocando afuera
            title = { Text("Procesando Pago", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Esperando lectura de sensor NFC...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Acerque su teléfono a la terminal de la cafetería.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {}
        )
        LaunchedEffect(Unit) {
            delay(3000)
            procesandoNFC = false
            pagoExitoso = true
        }
    }

    if (pagoExitoso) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¡Pago Exitoso!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("El pago simulado se ha procesado correctamente.")
                    Text("Tu pedido ha sido enviado a la cocina de la cafetería.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Redirigiendo al menú principal...", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {}
        )
    }
    // MONITOREO DEL PEDIDO EN EFECTIVO
    if (procesandoEfectivo && idPedidoCreado != null) {
        LaunchedEffect(idPedidoCreado) {
            var pagoConfirmadoPorBarista = false

            while (!pagoConfirmadoPorBarista) {
                delay(4000) // Consulta al servidor cada 4 segundos para no saturar tu API

                try {
                    val respuesta = RetrofitClient.apiService.verificarEstadoPedido(idPedidoCreado!!)

                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        val statusResponse = respuesta.body()!!

                        println("🔍 [POLLING] Consultando pedido ${idPedidoCreado}. Estado en backend: ${statusResponse.estado}")

                        // Evaluamos el estado exacto que configuramos en el Backend
                        when (statusResponse.estado) {
                            "PROCESANDO" -> {
                                // ¡El barista ya lo aceptó!
                                pagoConfirmadoPorBarista = true
                                procesandoEfectivo = false

                                try {
                                    val requestGami = com.example.cafeteria.data.model.ProcesarCompraRequest(
                                        usuario_id = "63f28ccd-9173-40b2-b919-01b784eb148f",
                                        monto_total = total,
                                        cantidad_productos = itemsCarrito.sumOf { it.cantidad },
                                        es_primer_compra_dia = false
                                    )
                                    RetrofitClient.apiService.procesarCompra(requestGami)
                                } catch (e: Exception) {
                                    println("❌ Error al otorgar XP en pago por caja: ${e.message}")
                                }

                                pagoExitoso = true // Detona tu animación, popBackStack, etc.
                            }
                            "RECHAZADO", "CANCELADO" -> {
                                procesandoEfectivo = false
                                // Opcional: mostrar un Toast o diálogo de error al alumno
                                break
                            }
                            "PENDIENTE_PAGO" -> {
                                // No hace nada, sigue el ciclo 'while' y vuelve a preguntar en 4 segundos
                            }
                        }
                    } else {
                        println("⚠️ [POLLING] El servidor respondió con un error: ${respuesta.code()}")
                    }
                } catch (e: Exception) {
                    println("❌ [POLLING ERROR] Error al consultar estado: ${e.message}")
                }
            }
        }
    }
    // 3. Diálogo de Espera para Pago en Efectivo
    if (procesandoEfectivo) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Pedido Enviado", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Esperando confirmación de pago...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Por favor, acércate a la caja de la cafetería y proporciona tu nombre para liquidar tu cuenta.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }
}

data class PedidoStatusResponse(
    val pedido_id: String,
    val estado: String,
    val mensaje: String
)