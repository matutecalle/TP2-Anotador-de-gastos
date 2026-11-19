package com.example.tp2arqmoviles

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Clase Application: se ejecuta una sola vez, antes que cualquier pantalla.
 *
 * Acá se configura Firestore porque los settings deben aplicarse ANTES del primer uso
 * de la base (si no, el SDK lanza una excepción).
 */
class GastosApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Persistencia offline explícita: los datos y las escrituras pendientes se guardan
        // en disco. La app sigue funcionando sin internet (RNF-02 del TP2) y sincroniza
        // sola al recuperar la conexión. 100 MB es el tamaño por defecto del SDK; se deja
        // escrito para que la decisión quede visible.
        val cache = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100L * 1024 * 1024)
            .build()

        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cache)
                .build()
    }
}
