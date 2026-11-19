package dev.lifeos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lifeos.data.CalendarMath.hm
import dev.lifeos.data.Dates
import dev.lifeos.data.EventKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.isWeekly
import dev.lifeos.data.kindEnum
import dev.lifeos.data.startDate
import dev.lifeos.data.untilDate
import dev.lifeos.db.Event
import dev.lifeos.db.Project
import kotlinx.datetime.LocalDate

private const val DAY_END = 23 * 60 + 59

/** Botón con la hora; al pulsarlo abre el selector de hora (24 h). */
@Composable
fun TimeField(label: String, minutes: Int, onChange: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { picking = true }) { Text(hm(minutes)) }
    }
    if (picking) {
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { onChange(state.hour * 60 + state.minute); picking = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * Crear o editar un evento. `occurrence` es el día concreto que se pulsó
 * (en los semanales sirve para borrar solo esa semana).
 */
@Composable
fun EventDialog(
    event: Event?,
    projects: List<Project>,
    initialDate: LocalDate,
    initialStart: Int = 9 * 60,
    occurrence: LocalDate? = null,
    initialKind: EventKind = EventKind.CLASS,
    initialProject: String? = null,
    onDismiss: () -> Unit,
) {
    val act = rememberActions()
    var title by remember { mutableStateOf(event?.title ?: "") }
    var kind by remember { mutableStateOf(event?.kindEnum ?: initialKind) }
    var projectId by remember { mutableStateOf(event?.project_id ?: initialProject) }
    var date by remember { mutableStateOf<LocalDate?>(event?.startDate ?: initialDate) }
    var start by remember { mutableStateOf(event?.start_min?.toInt() ?: initialStart) }
    var end by remember { mutableStateOf(event?.end_min?.toInt() ?: minOf(initialStart + 60, DAY_END)) }
    var weekly by remember { mutableStateOf(event?.isWeekly ?: false) }
    var until by remember { mutableStateOf(event?.untilDate) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var deleting by remember { mutableStateOf(false) }

    val project = projects.firstOrNull { it.id == projectId }
    val finalTitle = title.ifBlank { project?.name ?: "" }
    val valid = finalTitle.isNotBlank() && end > start && date != null

    fun save() {
        if (!valid) return
        val d = date ?: return
        val u = until?.takeIf { weekly && it >= d }
        val t = finalTitle
        if (event == null) act { addEvent(t, kind, d, start, end, weekly, u, projectId, location) }
        else act { updateEvent(event.id, t, kind, d, start, end, weekly, u, projectId, location) }
        onDismiss()
    }

    FormDialog(
        title = if (event == null) "Nuevo evento" else "Editar evento",
        canConfirm = valid,
        onConfirm = ::save,
        onDismiss = onDismiss,
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EventKind.entries.filter { it != EventKind.FOCUS }.forEach { k ->
                FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
            }
        }
        TitleField(
            title, { title = it },
            if (kind == EventKind.CLASS && project != null) "Nombre (si no, «${project.name}»)" else "Qué es",
            onDone = ::save,
        )
        val sorted = projects.sortedBy { if (it.kind == ProjectKind.SUBJECT.name) 0 else 1 }
        Picker(
            "Asignatura o proyecto",
            listOf<Pair<String?, String>>(null to "Ninguno") + sorted.map { it.id to projectLabel(it) },
            projectId,
        ) { projectId = it }
        DateField(if (weekly) "Primer día" else "Día", date, { date = it }, allowNone = false)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeField("Empieza", start) { new ->
                val duration = (end - start).coerceAtLeast(15)
                start = new
                end = minOf(new + duration, DAY_END)
            }
            TimeField("Termina", end) { end = it }
        }
        if (end <= start) {
            Text("Tiene que terminar después de empezar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(weekly, { weekly = it })
            Text(date?.let { "Se repite cada ${Dates.dayName(it)}" } ?: "Se repite cada semana")
        }
        if (weekly) DateField("Hasta", until, { until = it })
        OutlinedTextField(location, { location = it }, label = { Text("Lugar (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (event != null && event.kindEnum == EventKind.STUDY) {
            Button(onClick = {
                act { startFocus(event.title, null, event.project_id) }
                onDismiss()
            }) { Text("Empezar bloque ahora · ${LocalPrefs.current.blockMin} min") }
        }
        if (event != null) {
            if (event.isWeekly) {
                Text(
                    "Los cambios se aplican a todas las semanas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { deleting = true }) { Text("Borrar evento", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (deleting && event != null) {
        if (event.isWeekly && occurrence != null) {
            AlertDialog(
                onDismissRequest = { deleting = false },
                title = { Text("Borrar «${event.title}»") },
                text = {
                    Column {
                        TextButton(onClick = { act { skipOccurrence(event.id, occurrence) }; onDismiss() }) {
                            Text("Solo el ${Dates.medium(occurrence)}")
                        }
                        TextButton(onClick = { act { endSeriesFrom(event.id, occurrence) }; onDismiss() }) {
                            Text("Este y los siguientes")
                        }
                        TextButton(onClick = { act { deleteEvent(event.id) }; onDismiss() }) {
                            Text("Todas las semanas", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } },
            )
        } else {
            ConfirmDialog(
                "¿Borrar «${event.title}»?", "Desaparece del calendario.", "Borrar",
                onConfirm = { act { deleteEvent(event.id) }; onDismiss() },
                onDismiss = { deleting = false },
            )
        }
    }
}
