package com.example.cafeteriaapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cafeteriaapp.ui.Destinos
import com.example.cafeteriaapp.ui.viewmodel.AuthViewModel
import com.example.cafeteriaapp.ui.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginExitoso: (String, String, String) -> Unit,
    onIrARegistro: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by authViewModel.loginState.collectAsState()

    // 🌟 Envolvemos todo en un Surface para heredar el color de fondo del tema actual
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Usamos MaterialTheme.colorScheme.primary para los títulos principales (Moka o Caramelo)
            Text(
                text = "¡Hola de nuevo! ☕",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ingresa para pedir tu café y subir de nivel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo Institucional") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                // 🎨 Forzamos a los colores del TextField a sincronizarse con el fondo actual
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                // 🎨 Forzamos a los colores del TextField a sincronizarse con el fondo actual
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (loginState is LoginUiState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = { authViewModel.loginEstudiante(correo, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Iniciar Sesión")
                }
            }

            TextButton(onClick = onIrARegistro) {
                Text(
                    text = "¿No tienes cuenta? Regístrate aquí",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (val state = loginState) {
                is LoginUiState.Success -> {
                    LaunchedEffect(state) {
                        onLoginExitoso(state.usuarioId, state.role, state.nombre)
                    }
                }
                is LoginUiState.Error -> {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                else -> {}
            }
        }
    }
}