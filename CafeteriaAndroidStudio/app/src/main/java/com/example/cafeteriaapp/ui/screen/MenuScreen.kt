package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.ui.viewmodel.MenuUiState
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel
import androidx.compose.material.icons.filled.ShoppingCart

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MenuScreen(
    menuViewModel: MenuViewModel = viewModel(),
    onIrAlCarritoClick: () -> Unit
) {
    val uiState by menuViewModel.uiState.collectAsState()

    val categoriasVisibles = listOf("Todo", "Café", "Fríos", "Repostería")
    var categoriaSeleccionadaIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ Cafetería Universitaria", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = onIrAlCarritoClick) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Ver Carrito",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            //Menú scrolleable horizontal de categorías
            ScrollableTabRow(
                selectedTabIndex = categoriaSeleccionadaIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categoriasVisibles.forEachIndexed { index, titulo ->
                    Tab(
                        selected = categoriaSeleccionadaIndex == index,
                        onClick = { categoriaSeleccionadaIndex = index },
                        text = { Text(titulo, style = MaterialTheme.typography.bodyLarge) }
                    )
                }
            }

            //Manejo de Estados de la API
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
                    val categoriaActual = categoriasVisibles[categoriaSeleccionadaIndex]

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
                                        // 💡 Aquí usamos el truco anterior: pasamos la instancia de tu ViewModel
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