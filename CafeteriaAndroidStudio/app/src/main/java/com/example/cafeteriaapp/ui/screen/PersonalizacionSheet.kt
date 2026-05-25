package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeteriaapp.domain.model.ProductoResponse
import com.example.cafeteriaapp.domain.model.OpcionExtraResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizacionSheet(
    producto: ProductoResponse,
    extrasSeleccionados: List<OpcionExtraResponse>,
    cantidad: Int,
    onOpcionAlternada: (OpcionExtraResponse, Int, Int, List<OpcionExtraResponse>) -> Unit,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    // Calcular precio en tiempo real en la UI
    val precioBase = producto.precio ?: 0.0
    val precioExtras = extrasSeleccionados.sumOf { it.precioAdicional ?: 0.0 }
    val precioTotalCalculado = (precioBase + precioExtras) * cantidad

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // CABECERA DEL PRODUCTO
            Text(
                text = "Personaliza tu ${producto.nombre}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            producto.descripcion?.let {
                Text(text = it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            // LISTA DINÁMICA DE GRUPOS Y EXTRAS
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                producto.gruposOpciones?.let { grupos ->
                    items(grupos) { grupo ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Título del grupo (Ej: Tipo de Leche)
                            Text(
                                text = grupo.nombre ?: "Extras",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (grupo.maxSeleccion == 1) "Selecciona una opción" else "Puedes seleccionar hasta ${grupo.maxSeleccion}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Renderizar las opciones del grupo
                            grupo.opciones?.forEach { opcion ->
                                val estaSeleccionado = extrasSeleccionados.any { it.id == opcion.id }
                                val maxSel = grupo.maxSeleccion ?: 1
                                val gId = grupo.id ?: 0
                                val todasLasOpciones = grupo.opciones ?: emptyList()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 💡 DECISIÓN COMPONENTES UI INTELIGENTE: Radio o Checkbox
                                    if (maxSel == 1) {
                                        RadioButton(
                                            selected = estaSeleccionado,
                                            onClick = { onOpcionAlternada(opcion, maxSel, gId, todasLasOpciones) }
                                        )
                                    } else {
                                        Checkbox(
                                            checked = estaSeleccionado,
                                            onCheckedChange = { onOpcionAlternada(opcion, maxSel, gId, todasLasOpciones) }
                                        )
                                    }

                                    Text(
                                        text = opcion.nombre ?: "",
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if ((opcion.precioAdicional ?: 0.0) > 0.0) {
                                        Text(
                                            text = "+$${opcion.precioAdicional}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 16.dp))

            // FOOTER: CONTROL DE CANTIDAD Y CONFIRMACIÓN
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selector numérico (Steppers)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onDecrementar, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$cantidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    FilledTonalButton(onClick = onIncrementar, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Botón Añadir con el precio mutado
                Button(
                    onClick = onConfirmar,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp)
                        .height(48.dp)
                ) {
                    Text(text = "Añadir $${String.format("%.2f", precioTotalCalculado)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}