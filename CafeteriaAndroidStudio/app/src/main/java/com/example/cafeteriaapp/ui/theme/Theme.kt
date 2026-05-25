package com.example.cafeteriaapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Configuración del Modo Oscuro
private val DarkColorScheme = darkColorScheme(
    primary = CafeOscuroPrimario,           // Títulos en tono caramelo elegante
    background = CafeOscuroFondo,
    surface = CafeOscuroFondo,
    secondaryContainer = CafeOscuroContenedor, // Tus Cards de IA y Producto usarán este
    onBackground = TextoBlancoPrincipal,    // Descripciones en blanco
    onSurface = TextoBlancoPrincipal,
    onSecondaryContainer = TextoBlancoPrincipal
)

// Configuración del Modo Claro (Por Defecto)
private val LightColorScheme = lightColorScheme(
    primary = CafeClaroPrimario,            // Títulos en café moka
    background = CafeClaroFondo,
    surface = CafeClaroFondo,
    secondaryContainer = CafeClaroContenedor, // Cards en tono capuchino suave
    onBackground = TextoNegroPrincipal,     // Descripciones en estricto color negro
    onSurface = TextoNegroPrincipal,
    onSecondaryContainer = TextoNegroPrincipal
)

@Composable
fun CafeteriaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Detecta automáticamente el sistema
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        //typography = Typography, // Tu tipografía actual
        content = content
    )
}