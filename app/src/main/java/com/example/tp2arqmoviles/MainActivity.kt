package com.example.tp2arqmoviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tp2arqmoviles.ui.theme.TP2ARQMOVILESTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow

data class Gasto(val descripcion: String, val monto: Double, val categoria: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TP2ARQMOVILESTheme{
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnotadorGastos(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AnotadorGastos(modifier: Modifier = Modifier) {
    val categorias = listOf("Comida", "Transporte", "Servicios", "Otros")
    val gastos = remember { mutableStateListOf<Gasto>() }

    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf(categorias[0]) }
    var menuExpandido by remember { mutableStateOf(false) }

    val total = gastos.sumOf { it.monto }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Anotador de gastos", fontSize = 22.sp)

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        OutlinedTextField(
            value = monto,
            onValueChange = { monto = it },
            label = { Text("Monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Box(modifier = Modifier.padding(top = 8.dp)) {
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
                categorias.forEach { cat ->
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
                val montoDouble = monto.toDoubleOrNull()
                if (descripcion.isNotBlank() && montoDouble != null && montoDouble > 0) {
                    gastos.add(Gasto(descripcion, montoDouble, categoriaSeleccionada))
                    descripcion = ""
                    monto = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Agregar gasto")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Gastos de hoy", fontSize = 18.sp)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            items(gastos) { gasto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${gasto.descripcion} (${gasto.categoria})",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "$${"%.2f".format(gasto.monto)}")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(text = "Total del día: $${"%.2f".format(total)}", fontSize = 20.sp)
    }
}