package com.example.tp2arqmoviles.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tp2arqmoviles.data.AuthRepository
import com.example.tp2arqmoviles.data.EstadoSync
import com.example.tp2arqmoviles.data.Gasto
import com.example.tp2arqmoviles.data.GastosRepository
import com.example.tp2arqmoviles.data.MAX_DESCRIPCION
import com.example.tp2arqmoviles.data.ResultadoGastos
import com.example.tp2arqmoviles.data.mensajeErrorAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Errores de validación del formulario de carga, uno por campo (null = sin error). */
data class ErroresFormulario(
    val descripcion: String? = null,
    val monto: String? = null
)

/** Estado de la pantalla de login/registro. */
data class LoginUiState(
    val cargando: Boolean = false,
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null
)

/**
 * ViewModel único de la app. Sobrevive a rotaciones de pantalla y expone todo el
 * estado como StateFlow (flujo unidireccional: la UI observa y envía eventos).
 *
 * Reemplaza al remember / mutableStateListOf del TP2 (migración prevista en la sección 4.2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val gastosRepo: GastosRepository = GastosRepository()
) : ViewModel() {

    // ---------- Sesión ----------

    /** Usuario logueado o null. Arranca con la sesión guardada (login automático). */
    val usuario: StateFlow<FirebaseUser?> = authRepo.observarUsuario()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.usuarioActual)

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    // ---------- Gastos ----------

    private val _errorDatos = MutableStateFlow<String?>(null)
    /** Error al leer/escribir en Firestore (por ejemplo, reglas mal configuradas). */
    val errorDatos: StateFlow<String?> = _errorDatos.asStateFlow()

    /**
     * Lista de gastos en tiempo real. flatMapLatest: cuando cambia el usuario se cancela
     * el listener anterior y se abre uno nuevo sobre users/{uid}/gastos.
     */
    val gastos: StateFlow<ResultadoGastos> = usuario
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(ResultadoGastos(emptyList(), EstadoSync.SINCRONIZADO))
            } else {
                gastosRepo.observarGastos(user.uid)
                    .onEach { _errorDatos.value = null }
                    .catch { e ->
                        _errorDatos.value = "No se pudieron leer los gastos: ${e.localizedMessage}"
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ResultadoGastos(emptyList(), EstadoSync.SINCRONIZADO)
        )

    private val _erroresFormulario = MutableStateFlow(ErroresFormulario())
    val erroresFormulario: StateFlow<ErroresFormulario> = _erroresFormulario.asStateFlow()

    // ---------- Eventos de login ----------

    fun ingresar(email: String, password: String) = autenticar(email, password, crear = false)

    fun crearCuenta(email: String, password: String) = autenticar(email, password, crear = true)

    private fun autenticar(email: String, password: String, crear: Boolean) {
        val emailLimpio = email.trim()

        // Validación local antes de llamar a Firebase: respuesta inmediata y menos tráfico.
        val errorEmail = when {
            emailLimpio.isEmpty() -> "Ingresá tu email."
            !Patterns.EMAIL_ADDRESS.matcher(emailLimpio).matches() ->
                "El email no tiene un formato válido."
            else -> null
        }
        val errorPassword = when {
            password.isEmpty() -> "Ingresá tu contraseña."
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            else -> null
        }
        if (errorEmail != null || errorPassword != null) {
            _login.value = LoginUiState(errorEmail = errorEmail, errorPassword = errorPassword)
            return
        }

        _login.value = LoginUiState(cargando = true)
        val alTerminar: (Exception?) -> Unit = { error ->
            _login.value = if (error == null) LoginUiState()
            else LoginUiState(errorGeneral = mensajeErrorAuth(error))
        }
        if (crear) authRepo.crearCuenta(emailLimpio, password, alTerminar)
        else authRepo.ingresar(emailLimpio, password, alTerminar)
    }

    fun limpiarErroresLogin() {
        _login.value = _login.value.copy(errorEmail = null, errorPassword = null, errorGeneral = null)
    }

    fun cerrarSesion() {
        authRepo.cerrarSesion()
        _erroresFormulario.value = ErroresFormulario()
        _errorDatos.value = null
    }

    // ---------- Eventos de gastos ----------

    /**
     * Valida y agrega un gasto. Devuelve true si se aceptó (la UI limpia el formulario).
     * Si se rechaza, deja el mensaje en [erroresFormulario] junto al campo que falló
     * (mejora pendiente identificada en el TP2).
     */
    fun agregarGasto(descripcion: String, montoTexto: String, categoria: String): Boolean {
        val uid = usuario.value?.uid ?: return false
        val desc = descripcion.trim()
        // Se acepta coma o punto como separador decimal (teclado en español).
        val monto = montoTexto.trim().replace(',', '.').toDoubleOrNull()

        val errores = ErroresFormulario(
            descripcion = when {
                desc.isEmpty() -> "La descripción no puede estar vacía."
                desc.length > MAX_DESCRIPCION -> "Máximo $MAX_DESCRIPCION caracteres."
                else -> null
            },
            monto = when {
                montoTexto.isBlank() -> "Ingresá un monto."
                monto == null -> "El monto debe ser un número (ej: 1500.50)."
                monto <= 0 -> "El monto debe ser mayor a cero."
                else -> null
            }
        )
        _erroresFormulario.value = errores
        if (errores.descripcion != null || errores.monto != null || monto == null) return false

        val gasto = Gasto(
            descripcion = desc,
            monto = monto,
            categoria = categoria,
            fecha = System.currentTimeMillis()
        )
        gastosRepo.agregarGasto(uid, gasto) { e ->
            _errorDatos.value = "El servidor rechazó el gasto: ${e.localizedMessage}"
        }
        return true
    }

    /** Al escribir en un campo se borra su error, para no dejar mensajes viejos. */
    fun limpiarErrorDescripcion() = _erroresFormulario.update { it.copy(descripcion = null) }

    fun limpiarErrorMonto() = _erroresFormulario.update { it.copy(monto = null) }
}
