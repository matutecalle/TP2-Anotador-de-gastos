package com.example.tp2arqmoviles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de ingreso / registro con email y contraseña.
 * Los textos que escribe el usuario son estado efímero de la UI (rememberSaveable,
 * sobrevive a la rotación); la validación y el resultado viven en el ViewModel.
 */
@Composable
fun LoginScreen(
    estado: LoginUiState,
    onIngresar: (String, String) -> Unit,
    onCrearCuenta: (String, String) -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Anotador de gastos", fontSize = 26.sp)
        Text(
            text = "Ingresá para guardar tus gastos en la nube y verlos en cualquier dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onEditar() },
            label = { Text("Email") },
            singleLine = true,
            isError = estado.errorEmail != null,
            supportingText = estado.errorEmail?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onEditar() },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = estado.errorPassword != null,
            supportingText = { Text(estado.errorPassword ?: "Mínimo 6 caracteres") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // Error que devuelve Firebase (credenciales inválidas, sin conexión, etc.)
        estado.errorGeneral?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = { onIngresar(email, password) },
            enabled = !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Ingresar")
        }

        OutlinedButton(
            onClick = { onCrearCuenta(email, password) },
            enabled = !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Crear cuenta")
        }

        if (estado.cargando) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(32.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
