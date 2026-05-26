package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeteria.data.model.PosicionLeaderboard
import com.example.cafeteriaapp.ui.components.RecomendacionIaCard
import com.example.cafeteriaapp.ui.viewmodel.AiUiState
import com.example.cafeteriaapp.ui.viewmodel.CafeteriaViewModel
import com.example.cafeteriaapp.ui.viewmodel.PerfilUiState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.ProgressIndicatorDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuarioId: String,
    viewModel: CafeteriaViewModel = viewModel(),
    onBack: () -> Unit
) {
    val pState by viewModel.perfilState.collectAsState()
    val aiUiState by viewModel.aiState.collectAsState()
    var mostrarBanner by remember { mutableStateOf(true) }

    LaunchedEffect(usuarioId) {
        if (usuarioId.isNotEmpty()) {
            mostrarBanner = true
            viewModel.cargarPerfilGamificacion(usuarioId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.limpiarRecomendacion()
        }
    }

    when (val aiState = aiUiState) {
        is AiUiState.Success -> {
            RecomendacionIaCard(
                mensaje = aiState.sugerencia,
                visible = mostrarBanner,
                onCloseClick = { mostrarBanner = false }
            )
        }
        else -> { /* No hace nada si está en Loading, Error o Idle */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabla de Posiciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = pState) {
                is PerfilUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PerfilUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            val miXp = state.xpPropia
                            val miNivel = state.nivelPropio

                            // 📊 CÁLCULO DE GAMIFICACIÓN DINÁMICO (Asumiendo que cada nivel requiere 1000 XP)
                            val xpDelNivelActual = miXp % 1000
                            val progresoFlotante = xpDelNivelActual.toFloat() / 1000f

                            // 🎨 DEGRADADO PREMIUM CON TUS COLORES DE SISTEMA
                            val degradadoPremium = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                )
                            )

                            // 🌟 NUEVA TARJETA DE PERFIL IMPACTANTE
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(brush = degradadoPremium)
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Stars,
                                                contentDescription = "Icono de Nivel",
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Perfil de Cliente",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Miembro Cafetería",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Nivel $miNivel",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "Progreso de Nivel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "$xpDelNivelActual / 1000 XP",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { progresoFlotante },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "✨ $miXp XP totales acumulados",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Subtítulo del Leaderboard
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EmojiEvents, "Podio", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Otros Competidores de la Escuela", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        itemsIndexed(state.leaderboard) { index, compañero ->
                            UsuarioLeaderboardCard(posicion = index + 2, datos = compañero)
                        }
                    }
                }
                is PerfilUiState.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun UsuarioLeaderboardCard(posicion: Int, datos: PosicionLeaderboard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$posicion",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = datos.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nivel ${datos.nivel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = "${datos.xp_total}xp.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}