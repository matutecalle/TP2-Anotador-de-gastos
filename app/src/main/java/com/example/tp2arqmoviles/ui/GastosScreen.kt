package com.example.tp2arqmoviles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tp2arqmoviles.data.CATEGORIAS
import com.example.tp2arqmoviles.data.EstadoSync
import com.example.tp2arqmoviles.data.Gasto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GastosScreen(
    email: String,
    gastos: List<Gasto>,
    estadoSync: EstadoSync,
    errores: ErroresFormulario,
    errorDatos: String?,
    onAgregar: (descripcion: String, monto: String, categoria: String) -> Boolean,
    onEditarDescripcion: () -> Unit,
    onEditarMonto: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado efímero del formulario (lo que se está tipeando). La lista de gastos,
    // los errores y la sesión vienen del ViewModel.
    var descripcion by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var categoriaSeleccionada by rememberSaveable { mutableStateOf(CATEGORIAS[0]) }
    var menuExpandido by remember { mutableStateOf(false) }

    // Valores DERIVADOS de la lista (no se guardan): igual que en el TP2, así el total
    // nunca puede quedar desfasado del listado.
    val inicioHoy = remember(gastos) { inicioDelDia(System.currentTimeMillis()) }
    val totalDelDia = gastos.filter { it.fecha >= inicioHoy }.sumOf { it.monto }
    val gastosPorDia = remember(gastos) { gastos.groupBy { inicioDelDia(it.fecha) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ---------- Encabezado ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Anotador de gastos", fontSize = 22.sp)
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }
        }

        IndicadorSync(estadoSync, modifier = Modifier.padding(top = 4.dp))

        errorDatos?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ---------- Formulario ----------
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it; onEditarDescripcion() },
            label = { Text("Descripción") },
            singleLine = true,
            isError = errores.descripcion != null,
            supportingText = errores.descripcion?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = monto,
            onValueChange = { monto = it; onEditarMonto() },
            label = { Text("Monto") },
            singleLine = true,
            isError = errores.monto != null,
            supportingText = errores.monto?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = { menuExpandido = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Categoría: $categoriaSeleccionada")
            }
            DropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false }
            ) {
                CATEGORIAS.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            categoriaSeleccionada = cat
                            menuExpandido = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (onAgregar(descripcion, monto, categoriaSeleccionada)) {
                    descripcion = ""
                    monto = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Agregar gasto")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // ---------- Historial agrupado por día ----------
        Text(text = "Historial", fontSize = 18.sp)

        if (gastos.isEmpty()) {
            Text(
                text = "Todavía no cargaste gastos.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            gastosPorDia.forEach { (dia, gastosDelDia) ->
                item(key = "dia-$dia") {
                    EncabezadoDia(dia, gastosDelDia.sumOf { it.monto }, inicioHoy)
                }
                items(gastosDelDia, key = { it.id }) { gasto ->
                    FilaGasto(gasto)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(text = "Total del día: ${formatoMonto(totalDelDia)}", fontSize = 20.sp)
    }
}

/** Indicador basado en los metadatos del snapshot de Firestore. */
@Composable
private fun IndicadorSync(estado: EstadoSync, modifier: Modifier = Modifier) {
    val (texto, color) = when (estado) {
        EstadoSync.SINCRONIZADO -> "● Sincronizado" to Color(0xFF2E7D32)
        EstadoSync.PENDIENTE -> "● Pendiente de sincronizar" to Color(0xFFEF6C00)
        EstadoSync.SIN_CONEXION -> "● Sin conexión (datos guardados en el teléfono)" to Color(0xFF757575)
    }
    Text(text = texto, color = color, style = MaterialTheme.typography.labelMedium, modifier = modifier)
}

@Composable
private fun EncabezadoDia(dia: Long, subtotal: Double, inicioHoy: Long) {
    val titulo = when (dia) {
        inicioHoy -> "Hoy"
        inicioDelDia(inicioHoy - 1) -> "Ayer" // 1 ms antes de hoy = algún momento de ayer
        else -> SimpleDateFormat("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-AR")).format(Date(dia))
            .replaceFirstChar { it.uppercase() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = formatoMonto(subtotal), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Fila de un gasto. Se mantiene la corrección del TP2: la descripción ocupa el espacio
 * restante (weight 1f) en una sola línea con "…", y el monto siempre queda legible.
 */
@Composable
private fun FilaGasto(gasto: Gasto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = "${gasto.descripcion} (${gasto.categoria})",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (gasto.pendiente) {
                Text(
                    text = "Sin sincronizar",
                    color = Color(0xFFEF6C00),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Text(text = formatoMonto(gasto.monto))
    }
}

private fun formatoMonto(monto: Double): String = "$" + "%.2f".format(monto)

/** Epoch millis de las 00:00 del día de [millis], en la zona horaria del teléfono. */
private fun inicioDelDia(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
