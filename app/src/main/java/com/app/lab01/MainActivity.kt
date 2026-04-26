package com.app.lab01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lab01.data.TareaEntity
import com.app.lab01.viewmodel.TareasViewModel
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

private val VioletaPrimario   = Color(0xFF7C3AED)
private val VioletaClaro      = Color(0xFFEDE9FE)
private val CianAccento       = Color(0xFF06B6D4)
private val RosaAccento       = Color(0xFFEC4899)
private val VerdeExito        = Color(0xFF10B981)
private val FondoClaro        = Color(0xFFF5F3FF)
private val FondoOscuro       = Color(0xFF1E1B2E)
private val SuperficieOscura  = Color(0xFF2D2A45)

private val LightColorScheme = lightColorScheme(
    primary          = VioletaPrimario,
    onPrimary        = Color.White,
    secondary        = CianAccento,
    onSecondary      = Color.White,
    tertiary         = RosaAccento,
    background       = FondoClaro,
    surface          = Color.White,
    onBackground     = Color(0xFF1A1035),
    onSurface        = Color(0xFF1A1035),
)

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFFA78BFA),
    onPrimary        = Color(0xFF2E1065),
    secondary        = CianAccento,
    onSecondary      = Color(0xFF003040),
    tertiary         = RosaAccento,
    background       = FondoOscuro,
    surface          = SuperficieOscura,
    onBackground     = Color(0xFFEDE9FE),
    onSurface        = Color(0xFFEDE9FE),
)

enum class FiltroTarea { TODAS, PENDIENTES, COMPLETADAS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var modoOscuro by remember { mutableStateOf(false) }
            MaterialTheme(
                colorScheme = if (modoOscuro) DarkColorScheme else LightColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppTareas(
                        modoOscuro = modoOscuro,
                        onToggleTema = { modoOscuro = !modoOscuro }
                    )
                }
            }
        }
    }
}

@Composable
fun AppTareas(
    vm: TareasViewModel = viewModel(),
    modoOscuro: Boolean,
    onToggleTema: () -> Unit
) {
    val tareas by vm.tareas.collectAsState()
    var texto by remember { mutableStateOf("") }
    var fechaNuevaTarea by remember { mutableStateOf<Long?>(null) }
    var filtro by remember { mutableStateOf(FiltroTarea.TODAS) }
    var tareaEditando by remember { mutableStateOf<TareaEntity?>(null) }
    var textoEdicion by remember { mutableStateOf("") }
    var fechaEdicion by remember { mutableStateOf<Long?>(null) }
    var mostrarDatePickerNueva by remember { mutableStateOf(false) }
    var mostrarDatePickerEdicion by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val tareasFiltradas = when (filtro) {
        FiltroTarea.TODAS       -> tareas
        FiltroTarea.PENDIENTES  -> tareas.filter { !it.completada }
        FiltroTarea.COMPLETADAS -> tareas.filter { it.completada }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mis Tareas", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onToggleTema) {
                Text(if (modoOscuro) "☀️" else "🌙",
                    style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            "${tareas.count { !it.completada }} pendientes · ${tareas.count { it.completada }} completadas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CampoTexto(valor = texto, onValorChange = { texto = it }, label = "Nueva tarea")
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { mostrarDatePickerNueva = true }) {
                Text(fechaNuevaTarea?.let { "📅 ${formatearFecha(it)}" } ?: "📅 Fecha límite (opcional)")
            }
            if (fechaNuevaTarea != null) {
                IconButton(onClick = { fechaNuevaTarea = null }) {
                    Icon(Icons.Default.Delete, contentDescription = "Quitar fecha",
                        tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Button(
            onClick = {
                if (texto.isNotBlank()) {
                    vm.agregarTarea(texto, fechaNuevaTarea)
                    texto = ""
                    fechaNuevaTarea = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("＋  Agregar tarea", color = MaterialTheme.colorScheme.onPrimary) }

        Spacer(Modifier.height(12.dp))
        FiltroChips(filtroActual = filtro, onFiltroChange = { filtro = it })
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tareasFiltradas, key = { it.id }) { tarea ->
                ItemTarea(
                    tarea    = tarea,
                    onToggle = { vm.toggleTarea(tarea) },
                    onDelete = { vm.eliminarTarea(tarea) },
                    onEdit   = {
                        tareaEditando = tarea
                        textoEdicion  = tarea.titulo
                        fechaEdicion  = tarea.fechaLimite
                    }
                )
            }
        }
    }

    if (mostrarDatePickerNueva) {
        DatePickerModal(
            onFechaSeleccionada = { fechaNuevaTarea = it; mostrarDatePickerNueva = false },
            onDismiss = { mostrarDatePickerNueva = false }
        )
    }

    if (mostrarDatePickerEdicion) {
        DatePickerModal(
            onFechaSeleccionada = { fechaEdicion = it; mostrarDatePickerEdicion = false },
            onDismiss = { mostrarDatePickerEdicion = false }
        )
    }

    tareaEditando?.let { tarea ->
        AlertDialog(
            onDismissRequest = { tareaEditando = null },
            title = { Text("Editar tarea") },
            text = {
                Column {
                    OutlinedTextField(
                        value = textoEdicion,
                        onValueChange = { textoEdicion = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { mostrarDatePickerEdicion = true }) {
                            Text(fechaEdicion?.let { "📅 ${formatearFecha(it)}" }
                                ?: "📅 Fecha límite (opcional)")
                        }
                        if (fechaEdicion != null) {
                            IconButton(onClick = { fechaEdicion = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar fecha",
                                    tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textoEdicion.isNotBlank())
                        vm.editarTarea(tarea, textoEdicion, fechaEdicion)
                    tareaEditando = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { tareaEditando = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FiltroChips(filtroActual: FiltroTarea, onFiltroChange: (FiltroTarea) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FiltroTarea.entries.forEach { f ->
            val etiqueta = when (f) {
                FiltroTarea.TODAS       -> "Todas"
                FiltroTarea.PENDIENTES  -> "Pendientes"
                FiltroTarea.COMPLETADAS -> "Completadas"
            }
            FilterChip(
                selected = filtroActual == f,
                onClick  = { onFiltroChange(f) },
                label    = { Text(etiqueta) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = MaterialTheme.colorScheme.primary,
                    selectedLabelColor        = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun ItemTarea(
    tarea: TareaEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val colorFondo by animateColorAsState(
        targetValue = if (tarea.completada)
            VerdeExito.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surface,
        label = "cardColor"
    )
    val colorBorde = if (tarea.completada) VerdeExito else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colorFondo),
        border    = androidx.compose.foundation.BorderStroke(2.dp, colorBorde.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tarea.completada,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor   = VerdeExito,
                    uncheckedColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (tarea.completada)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough else null
                )
                tarea.fechaLimite?.let { fecha ->
                    val vencida = !tarea.completada && fecha < System.currentTimeMillis()
                    Text(
                        text = "📅 ${formatearFecha(fecha)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (vencida) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun CampoTexto(
    valor: String, onValorChange: (String) -> Unit,
    label: String, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor, onValueChange = onValorChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onFechaSeleccionada: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateState = rememberDatePickerState()
    var mostrarTimePicker by remember { mutableStateOf(false) }
    var fechaSeleccionadaMs by remember { mutableStateOf<Long?>(null) }

    if (!mostrarTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val utcMs = dateState.selectedDateMillis
                    if (utcMs != null) {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMs
                        }
                        val calLocal = Calendar.getInstance().apply {
                            set(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                                0, 0, 0
                            )
                            set(Calendar.MILLISECOND, 0)
                        }
                        fechaSeleccionadaMs = calLocal.timeInMillis
                        mostrarTimePicker = true
                    }
                }) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = 9,
            initialMinute = 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("¿A qué hora?") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = fechaSeleccionadaMs ?: return@TextButton
                    val horaMs = (timeState.hour * 60L + timeState.minute) * 60_000L
                    onFechaSeleccionada(base + horaMs)
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        )
    }
}

fun formatearFecha(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(Date(millis))
}