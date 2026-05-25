package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cafeteria.data.model.PosicionLeaderboard
import com.example.cafeteriaapp.ui.components.RecomendacionIaCard // IMPORTANTE: Tu Card basada en Popup
import com.example.cafeteriaapp.ui.viewmodel.AiUiState
import com.example.cafeteriaapp.ui.viewmodel.CafeteriaViewModel
import com.example.cafeteriaapp.ui.viewmodel.PerfilUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuarioId: String,
    viewModel: CafeteriaViewModel = viewModel()
) {
    // Escuchamos el estado del Perfil (Leaderboard)
    val pState by viewModel.perfilState.collectAsState()

    // 🤖 1. ESCUCHAMOS EL ESTADO DE LA IA QUE ANTES ESTABA IGNORADO
    val aiUiState by viewModel.aiState.collectAsState()

    // Estado para controlar si el usuario cierra el banner manualmente
    var mostrarBanner by remember { mutableStateOf(true) }

    LaunchedEffect(usuarioId) {
        if (usuarioId.isNotEmpty()) {
            mostrarBanner = true // Cada que se recargue, reiniciamos el visor del banner
            viewModel.cargarPerfilGamificacion(usuarioId)
        }
    }

    // 2. LIMPIEZA DE MEMORIA: Al salir de la pantalla de gamificación, reseteamos la recomendación
    DisposableEffect(Unit) {
        onDispose {
            viewModel.limpiarRecomendacion()
        }
    }

    // 3. INYECTAMOS EL POPUP DE IA EVALUANDO EL SEALED INTERFACE
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

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, "Nivel", tint = Color(0xFFFFB300), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Mi Perfil (Nivel $miNivel)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("$miXp xp totales acumulados", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, "Podio", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Otros Competidores de la Escuela", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        itemsIndexed(state.leaderboard) { index, compañero ->
                            // Pasamos el índice real para pintar el podio dinámico
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
            // LADO IZQUIERDO: Número de posición + Avatar de Supabase
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$posicion",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.secondary
                )

                // Imagen de perfil cargada asíncronamente desde la URL
                AsyncImage(
                    model = datos.avatar_url,
                    contentDescription = "Avatar de ${datos.nombre}",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_gallery), // Temporal mientras carga
                    error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_report_image) // Fallback si se cae la red
                )

                Spacer(modifier = Modifier.width(16.dp))

                // CENTRO: Nombre del alumno y su nivel debajo
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

            // LADO DERECHO: Puntos formateados en "XXXxp."
            Text(
                text = "${datos.xp_total}xp.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}