package com.example.tp2arqmoviles.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Encapsula Firebase Authentication (email y contraseña). */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** Usuario con sesión activa al momento de abrir la app (Firebase la recuerda). */
    val usuarioActual: FirebaseUser? get() = auth.currentUser

    /** Emite el usuario cada vez que cambia la sesión (login, registro, logout). */
    fun observarUsuario(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun ingresar(email: String, password: String, onResultado: (Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResultado(it.exception) }
    }

    fun crearCuenta(email: String, password: String, onResultado: (Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResultado(it.exception) }
    }

    fun cerrarSesion() = auth.signOut()
}

/** Traduce las excepciones de Firebase Auth a mensajes en español para el usuario. */
fun mensajeErrorAuth(e: Exception): String = when (e) {
    is FirebaseNetworkException ->
        "Sin conexión a internet. Para ingresar o crear la cuenta necesitás red."
    is FirebaseAuthWeakPasswordException ->
        "La contraseña es muy corta: debe tener al menos 6 caracteres."
    is FirebaseAuthUserCollisionException ->
        "Ya existe una cuenta con ese email. Probá con \"Ingresar\"."
    is FirebaseAuthInvalidUserException ->
        if (e.errorCode == "ERROR_USER_DISABLED") "Esta cuenta está deshabilitada."
        else "Email o contraseña incorrectos."
    is FirebaseAuthInvalidCredentialsException ->
        if (e.errorCode == "ERROR_INVALID_EMAIL") "El email no tiene un formato válido."
        else "Email o contraseña incorrectos."
    is FirebaseTooManyRequestsException ->
        "Demasiados intentos fallidos. Esperá unos minutos e intentá de nuevo."
    else -> "No se pudo completar la operación. ${e.localizedMessage ?: ""}".trim()
}
