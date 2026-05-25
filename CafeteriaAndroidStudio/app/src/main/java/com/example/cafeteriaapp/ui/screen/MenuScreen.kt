package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.ui.viewmodel.MenuUiState
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    menuViewModel: MenuViewModel = viewModel(),
    onIrAlCarritoClick: () -> Unit,
    onIrAGamificacionClick: () -> Unit,
    onIrAAjustesClick: () -> Unit
) {
    val uiState by menuViewModel.uiState.collectAsState()

    val categoriesVisibles = listOf("Todo", "Café", "Fríos", "Repostería")
    var categoriaSeleccionadaIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            // 💡 CORRECCIÓN 1: Cambiamos .fillMaxSize() por .fillMaxWidth() para que solo ocupe su alto natural
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // --- SECCIÓN: Barra de Bienvenida ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cafetherian",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- BARRA DE HERRAMIENTAS SUPERIOR ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp), // Ajustamos espaciado inferior
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BOTÓN ACCESO A GAMIFICACIÓN
                        IconButton(onClick = onIrAGamificacionClick) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Panel de Gamificación",
                                tint = Color(0xFFFFB300)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // BOTÓN REUBICADO DEL CARRITO DE COMPRAS
                        IconButton(onClick = onIrAlCarritoClick) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Ver Carrito",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        //BOTON DE AJUSTES
                        IconButton(onClick = onIrAAjustesClick) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues -> // 💡 CORRECCIÓN 2: Usamos estos márgenes obligatorios
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Evita que el contenido colisione o se meta debajo de la topBar
        ) {
            // Menú scrolleable horizontal de categorías
            ScrollableTabRow(
                selectedTabIndex = categoriaSeleccionadaIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categoriesVisibles.forEachIndexed { index, titulo ->
                    Tab(
                        selected = categoriaSeleccionadaIndex == index,
                        onClick = { categoriaSeleccionadaIndex = index },
                        text = { Text(titulo, style = MaterialTheme.typography.bodyLarge) }
                    )
                }
            }

            // Manejo de Estados de la API
            when (val state = uiState) {
                is MenuUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is MenuUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                    }
                }

                is MenuUiState.Success -> {
                    val categoriaActual = categoriesVisibles[categoriaSeleccionadaIndex]

                    if (categoriaActual == "Todo") {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                            state.productosPorCategoria.forEach { (nombreCategoria, productos) ->
                                item {
                                    println("🎨 [COMPOSE DEBUG] Todo -> Dibujando fila horizontal para la categoría: '$nombreCategoria' con ${productos.size} productos.")

                                    Text(
                                        text = nombreCategoria,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = 16.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(productos) { producto ->
                                            ProductoCard(
                                                producto = producto,
                                                onPersonalizarClick = { prod ->
                                                    menuViewModel.abrirPersonalizacion(prod)
                                                },
                                                modifier = Modifier.width(220.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val productosFiltrados = state.todosLosProductos.filter {
                            it.categoriaNombre?.equals(categoriaActual, ignoreCase = true) == true
                        }

                        println("🎨 [COMPOSE DEBUG] Pestaña específica -> Encontrados ${productosFiltrados.size} productos que coinciden con '$categoriaActual'")

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Especialidades en $categoriaActual",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                                )
                            }
                            items(productosFiltrados) { producto ->
                                ProductoCard(
                                    producto = producto,
                                    onPersonalizarClick = { prod ->
                                        menuViewModel.abrirPersonalizacion(prod)
                                    }
                                )
                            }
                        }
                    }

                    val productoAPersonalizar = menuViewModel.productoAEditar

                    productoAPersonalizar?.let { producto ->
                        PersonalizacionSheet(
                            producto = producto,
                            extrasSeleccionados = menuViewModel.extrasSeleccionadosTemporalmente,
                            cantidad = menuViewModel.cantidadTemporal,
                            onOpcionAlternada = { opcion, maxSeleccion, grupoId, opcionesGrupo ->
                                menuViewModel.alternarOpcion(opcion, maxSeleccion, grupoId, opcionesGrupo)
                            },
                            onIncrementar = { menuViewModel.incrementarCantidad() },
                            onDecrementar = { menuViewModel.decrementarCantidad() },
                            onConfirmar = { menuViewModel.agregarAlCarritoConfirmado() },
                            onDismiss = { menuViewModel.cerrarPersonalizacion() }
                        )
                    }
                }
            }
        }
    }
}