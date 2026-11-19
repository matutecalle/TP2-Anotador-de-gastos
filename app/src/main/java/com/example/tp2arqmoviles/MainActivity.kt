package com.example.tp2arqmoviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.tp2arqmoviles.ui.GastosScreen
import com.example.tp2arqmoviles.ui.GastosViewModel
import com.example.tp2arqmoviles.ui.LoginScreen
import com.example.tp2arqmoviles.ui.theme.TP2ARQMOVILESTheme

class MainActivity : ComponentActivity() {

    // El ViewModel queda asociado a la Activity y sobrevive a la rotación de pantalla.
    private val viewModel: GastosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TP2ARQMOVILESTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(viewModel, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * "Navegación" mínima: si hay sesión se muestra la pantalla de gastos, si no el login.
 * Con solo dos pantallas que dependen del estado de sesión no hace falta la librería
 * Navigation Compose. Al abrir la app con sesión activa se entra directo a los gastos.
 */
@Composable
fun App(viewModel: GastosViewModel, modifier: Modifier = Modifier) {
    val usuario by viewModel.usuario.collectAsState()

    val usuarioActual = usuario
    if (usuarioActual == null) {
        val login by viewModel.login.collectAsState()
        LoginScreen(
            estado = login,
            onIngresar = viewModel::ingresar,
            onCrearCuenta = viewModel::crearCuenta,
            onEditar = viewModel::limpiarErroresLogin,
            modifier = modifier
        )
    } else {
        val resultado by viewModel.gastos.collectAsState()
        val errores by viewModel.erroresFormulario.collectAsState()
        val errorDatos by viewModel.errorDatos.collectAsState()
        GastosScreen(
            email = usuarioActual.email.orEmpty(),
            gastos = resultado.gastos,
            estadoSync = resultado.estadoSync,
            errores = errores,
            errorDatos = errorDatos,
            onAgregar = viewModel::agregarGasto,
            onEditarDescripcion = viewModel::limpiarErrorDescripcion,
            onEditarMonto = viewModel::limpiarErrorMonto,
            onCerrarSesion = viewModel::cerrarSesion,
            modifier = modifier
        )
    }
}
