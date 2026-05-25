package com.example.cafeteriaapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cafeteriaapp.domain.model.ProductoResponse

@Composable
fun ProductoCard(
    producto: ProductoResponse,
    modifier: Modifier = Modifier,
    onPersonalizarClick: (ProductoResponse) -> Unit
) {
    var expandida by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expandida = !expandida },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
                AsyncImage(
                    model = producto.imagenUrl,
                    contentDescription = producto.nombre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$${String.format("%.2f", producto.precio)} MXN",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                    AnimatedVisibility(visible = expandida) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = producto.descripcion ?: "Sin descripción disponible.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = {
                                    onPersonalizarClick(producto)
                                }) {
                                    Text("Añadir")
                                }
                            }
                        }
                    }
            }
        }
    }
}