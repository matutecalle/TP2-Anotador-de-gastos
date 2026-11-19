package com.example.tp2arqmoviles.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Estado de sincronización de la lista según los metadatos del snapshot. */
enum class EstadoSync {
    /** Todo lo que se ve está confirmado por el servidor. */
    SINCRONIZADO,
    /** Hay escrituras locales que todavía no llegaron al servidor (hasPendingWrites). */
    PENDIENTE,
    /** No hay escrituras pendientes, pero los datos vienen de la caché local (sin conexión). */
    SIN_CONEXION
}

/** Resultado de cada snapshot: la lista y su estado de sincronización. */
data class ResultadoGastos(
    val gastos: List<Gasto>,
    val estadoSync: EstadoSync
)

/**
 * Único punto de acceso a los gastos en Firestore.
 * Ruta: users/{uid}/gastos/{gastoId}
 *
 * La UI y el ViewModel no conocen Firestore: si mañana los datos pasan por la API de
 * Cloud Run (docs/backend-cloud-run), solo cambia esta clase.
 */
class GastosRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun coleccion(uid: String): CollectionReference =
        db.collection("users").document(uid).collection("gastos")

    /**
     * Agrega un gasto. No se espera la respuesta del servidor: Firestore escribe primero
     * en la caché local y el snapshot listener lo muestra al instante (aunque no haya red).
     * Cuando vuelve la conexión, el SDK lo sube solo.
     *
     * [onError] solo se llama si el servidor RECHAZA la escritura (por ejemplo, por las
     * reglas de seguridad). Estando offline no se llama: la escritura queda pendiente.
     */
    fun agregarGasto(uid: String, gasto: Gasto, onError: (Exception) -> Unit) {
        val documento = coleccion(uid).document() // id generado localmente, sin red
        val datos = mapOf(
            "descripcion" to gasto.descripcion,
            "monto" to gasto.monto,
            "categoria" to gasto.categoria,
            "fecha" to gasto.fecha
        )
        documento.set(datos).addOnFailureListener { onError(it) }
    }

    /**
     * Observa los gastos del usuario en tiempo real, ordenados por fecha descendente.
     * Se usa MetadataChanges.INCLUDE para recibir un nuevo snapshot también cuando un
     * gasto pasa de "pendiente" a "sincronizado" (cambia solo la metadata, no los datos).
     */
    fun observarGastos(uid: String): Flow<ResultadoGastos> = callbackFlow {
        val registro = coleccion(uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error) // el ViewModel captura el error y lo muestra
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val gastos = snapshot.documents.mapNotNull { it.aGasto() }
                val estado = when {
                    snapshot.metadata.hasPendingWrites() -> EstadoSync.PENDIENTE
                    snapshot.metadata.isFromCache -> EstadoSync.SIN_CONEXION
                    else -> EstadoSync.SINCRONIZADO
                }
                trySend(ResultadoGastos(gastos, estado))
            }
        awaitClose { registro.remove() }
    }

    /** Conversión manual (sin reflexión): si un documento está mal formado, se ignora. */
    private fun DocumentSnapshot.aGasto(): Gasto? {
        val descripcion = getString("descripcion") ?: return null
        val monto = getDouble("monto") ?: return null
        val categoria = getString("categoria") ?: return null
        val fecha = getLong("fecha") ?: return null
        return Gasto(
            id = id,
            descripcion = descripcion,
            monto = monto,
            categoria = categoria,
            fecha = fecha,
            pendiente = metadata.hasPendingWrites()
        )
    }
}
