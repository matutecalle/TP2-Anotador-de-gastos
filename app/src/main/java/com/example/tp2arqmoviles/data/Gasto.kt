package com.example.tp2arqmoviles.data

/**
 * Modelo de un gasto.
 *
 * Se conservan los campos del TP2 (descripcion, monto, categoria) y se agregan:
 *  - id: identificador del documento en Firestore (users/{uid}/gastos/{id}).
 *  - fecha: momento de la carga en epoch millis; permite ordenar y agrupar por día.
 *  - pendiente: NO se guarda en Firestore. Indica que el gasto se escribió en la
 *    caché local pero todavía no fue confirmado por el servidor (sin conexión).
 */
data class Gasto(
    val id: String = "",
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val fecha: Long,
    val pendiente: Boolean = false
)

/** Categorías predefinidas (las mismas del TP2). */
val CATEGORIAS = listOf("Comida", "Transporte", "Servicios", "Otros")

/** Límite de caracteres de la descripción. Coincide con la regla de Firestore. */
const val MAX_DESCRIPCION = 200
