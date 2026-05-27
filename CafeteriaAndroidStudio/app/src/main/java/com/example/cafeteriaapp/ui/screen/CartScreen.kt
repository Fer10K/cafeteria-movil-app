package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cafeteriaapp.data.local.SessionManager
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.data.repository.CarritoRepository
import com.example.cafeteriaapp.ui.components.RecomendacionIaCard
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel
import kotlinx.coroutines.delay


// Agrégalo al final de tu archivo CartScreen.kt (Fuera del Composable)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    onBack: () -> Unit
) {
    val itemsCarrito = CarritoRepository.items
    val total = CarritoRepository.precioTotalCarrito
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val uidDinamico = sessionManager.obtenerSession() ?: ""

    val recomendacionIa by menuViewModel.recomendacionIaState.collectAsState()
    var mostrarBanner by remember { mutableStateOf(true) }

    // 🚀 CORRECCIÓN 1: Declarar el observador del flujo del ViewModel aquí arriba
    val estadoPago by menuViewModel.estadoPagoEfectivo.collectAsState()

    LaunchedEffect(key1 = CarritoRepository.items.size) {
        mostrarBanner = true
        menuViewModel.cargarSugerenciaMaridaje(uidDinamico)
    }

    DisposableEffect(Unit) {
        onDispose {
            menuViewModel.limpiarRecomendacion()
        }
    }

    var mostrarOpcionesPago by remember { mutableStateOf(false) }
    var procesandoNFC by remember { mutableStateOf(false) }
    var pagoExitoso by remember { mutableStateOf(false) }
    var procesandoEfectivo by remember { mutableStateOf(false) }
    var idPedidoCreado by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 🚀 CORRECCIÓN 2: Sacar el LaunchedEffect del 'if' y manejar la reactividad en la raíz
    LaunchedEffect(estadoPago) {
        // Solo reaccionamos si el usuario realmente está esperando un pago en efectivo
        if (procesandoEfectivo && idPedidoCreado != null) {
            when (estadoPago) {
                "PROCESANDO" -> {
                    procesandoEfectivo = false
                    pagoExitoso = true
                }
                "CANCELADO", "RECHAZADO" -> {
                    procesandoEfectivo = false
                    // Opcional: Mostrar un Toast informando la cancelación en caja
                }
            }
        }
    }

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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
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
            // El componente flotante de recomendación (puedes meterlo dentro de la columna o Scaffold)
            RecomendacionIaCard(
                mensaje = recomendacionIa,
                visible = mostrarBanner,
                onCloseClick = { mostrarBanner = false }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (itemsCarrito.isEmpty()) {
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
                            onClick = { mostrarOpcionesPago = true },
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
                        menuViewModel.enviarPedidoAlServidor(usuarioId = uidDinamico, metodoPago = "NFC") { idGenerado ->
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
                        menuViewModel.enviarPedidoAlServidor(usuarioId = uidDinamico, metodoPago = "EFECTIVO") { idGenerado ->
                            idPedidoCreado = idGenerado
                            procesandoEfectivo = true
                            menuViewModel.iniciarMonitoreoPedido(idGenerado)
                        }
                    }
                ) {
                    Text("Efectivo en Caja")
                }
            }
        )
    }

    // 2. Diálogo de Espera NFC
    if (procesandoNFC) {
        AlertDialog(
            onDismissRequest = { },
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
                }
            },
            confirmButton = {}
        )
    }

    // Diálogo de Pago Exitoso
    if (pagoExitoso) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¡Pago Exitoso!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("El pago se ha procesado correctamente.")
                    Text("Tu pedido ha sido enviado a la cocina de la cafetería.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {}
        )
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
                        text = "Por favor, acércate a la caja de la cafetería para liquidar tu cuenta.",
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