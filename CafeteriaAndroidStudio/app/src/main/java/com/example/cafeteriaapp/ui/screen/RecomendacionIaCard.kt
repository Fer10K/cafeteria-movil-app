package com.example.cafeteriaapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun RecomendacionIaCard(
    mensaje: String,
    visible: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Solo instanciamos el Popup si debe ser visible y el mensaje contiene texto
    if (visible && mensaje.isNotEmpty()) {
        Popup(
            alignment = Alignment.BottomCenter, // 🔥 Fuerza al Popup a posicionarse abajo al centro de la pantalla
            properties = PopupProperties(
                focusable = false, // Permite que el usuario siga interactuando con el menú de atrás
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            // La animación ahora se ejecuta perfectamente dentro del contenedor flotante
            AnimatedVisibility(
                visible = true, // Al instanciarse el popup, arranca la animación de entrada
                enter = slideInVertically(
                    initialOffsetY = { it }, // Inicia abajo y sube elegantemente
                    animationSpec = tween(durationMillis = 500)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it }, // Se oculta bajando
                    animationSpec = tween(durationMillis = 400)
                )
            ) {
                Card(
                    modifier = modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 😎 Clave: Evita que se encime con la barra de gestos/botones de Android
                        .padding(start = 16.dp, bottom = 24.dp), // Espaciado estético del borde inferior
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp // Un poco más de sombra para separarlo del fondo scrolleable
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Icono IA",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Hey!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar sugerencia",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}