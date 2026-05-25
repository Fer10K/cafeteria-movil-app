package com.example.cafeteriaapp.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteriaapp.ui.viewmodel.GamificationUiState
import com.example.cafeteriaapp.ui.viewmodel.CafeteriaViewModel
import com.example.cafeteriaapp.ui.viewmodel.MenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCompraScreen(
    menuViewModel: MenuViewModel = viewModel()
) {
    val uiState by menuViewModel.gamificationUiState.collectAsState()
    val usuarioIdConstante = "63f28ccd-9173-40b2-b919-01b784eb148f"

    LaunchedEffect(Unit) {
        // Ejecuta la carga a través del flujo limpio del ViewModel
        menuViewModel.cargarPerfilGamificacion(usuarioIdConstante)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Perfil Coffee Pro", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (val state = uiState) {
                is GamificationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is GamificationUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message, // 💡 CORREGIDO: Cambiado de .mensaje a .message
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { menuViewModel.cargarPerfilGamificacion(usuarioIdConstante) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                is GamificationUiState.Idle, is GamificationUiState.Success -> {
                    val xpActual = if (state is GamificationUiState.Success) state.data.xp_actual else 0
                    val nivelActual = if (state is GamificationUiState.Success) state.data.nivel_actual else 1

                    val xpNecesariaSiguienteNivel = nivelActual * 500
                    val xpNivelAnterior = (nivelActual - 1) * 500
                    val xpProgresoEnNivelActual = xpActual - xpNivelAnterior
                    val xpRequeridaSoloParaEsteNivel = xpNecesariaSiguienteNivel - xpNivelAnterior

                    val porcentajeProgreso = if (xpRequeridaSoloParaEsteNivel > 0) {
                        (xpProgresoEnNivelActual.toFloat() / xpRequeridaSoloParaEsteNivel.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val progresoAnimado by animateFloatAsState(targetValue = porcentajeProgreso)

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- TARJETA PRINCIPAL DEL JUGADOR ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "LVL\n$nivelActual",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "Fernando", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Estudiante / Cliente Frecuente", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Progreso de Nivel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$xpActual / $xpNecesariaSiguienteNivel XP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progresoAnimado },
                                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN DE ENCABEZADO DE LOGROS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Mis Medallas Desbloqueadas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val listaLogrosReales = if (state is GamificationUiState.Success) state.data.logros_nuevos else emptyList()

                    if (listaLogrosReales.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no has desbloqueado medallas.\n¡Tus compras e hitos aparecerán aquí! ☕",
                                color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
                        ) {
                            items(listaLogrosReales) { logro ->
                                MedallaCard(
                                    nombre = logro.nombre,
                                    descripcion = logro.descripcion,
                                    desbloqueado = true
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MedallaCard(nombre: String, descripcion: String, desbloqueado: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = Color(0xFFFFB300)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descripcion,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}