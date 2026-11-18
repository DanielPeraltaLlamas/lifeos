package dev.lifeos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.DeadlineKind
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.kindEnum
import dev.lifeos.db.Milestone
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// ---------------------------------------------------------------- utilidades

@Composable
private fun FormDialog(
    title: String,
    confirmLabel: String = "Guardar",
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } },
        confirmButton = { TextButton(onClick = onConfirm, enabled = canConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun TitleField(value: String, onChange: (String) -> Unit, label: String, onDone: () -> Unit = {}) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

/** Selector de fecha: atajos (Hoy, Mañana, +1 semana) y calendario. */
@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, allowNone: Boolean = true) {
    val today = LocalToday.current
    var picking by remember { mutableStateOf(false) }
    Column {
        Text(
            label + (value?.let { ": " + Dates.medium(it) } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (allowNone) FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("Sin fecha") })
            FilterChip(selected = value == today, onClick = { onChange(today) }, label = { Text("Hoy") })
            FilterChip(selected = value == today.plusDays(1), onClick = { onChange(today.plusDays(1)) }, label = { Text("Mañana") })
            FilterChip(selected = value == today.plusDays(7), onClick = { onChange(today.plusDays(7)) }, label = { Text("En una semana") })
            FilterChip(selected = false, onClick = { picking = true }, label = { Text("Elegir…") })
        }
    }
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: today).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                    }
                    picking = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

/** Desplegable genérico con botón. */
@Composable
fun <T> Picker(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "—")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); open = false })
                }
            }
        }
    }
}

fun projectLabel(p: Project): String = p.name

// ---------------------------------------------------------------- tarea

@Composable
fun TaskDialog(
    task: Task?,
    projects: List<Project>,
    milestones: List<Milestone>,
    initialProjectId: String? = null,
    initialMilestoneId: String? = null,
    onDismiss: () -> Unit,
) {
    val act = rememberActions()
    val today = LocalToday.current
    var title by remember { mutableStateOf(task?.title ?: "") }
    var projectId by remember { mutableStateOf(task?.project_id ?: initialProjectId) }
    var milestoneId by remember { mutableStateOf(task?.milestone_id ?: initialMilestoneId) }
    var due by remember { mutableStateOf(task?.due_date?.let(LocalDate::parse)) }
    var blocks by remember { mutableStateOf(task?.estimate_blocks?.toInt() ?: 0) }
    var planToday by remember { mutableStateOf(false) }
    val projectMilestones = milestones.filter { it.project_id == projectId }

    fun save() {
        if (title.isBlank()) return
        val m = milestoneId?.takeIf { id -> projectMilestones.any { it.id == id } }
        val est = blocks.takeIf { it > 0 }
        if (task == null) {
            act { addTask(title, projectId, m, due, est, if (planToday) today else null) }
        } else {
            act { updateTask(task.id, title, projectId, m, due, est, task.notes) }
        }
        onDismiss()
    }

    FormDialog(
        title = if (task == null) "Nueva tarea" else "Editar tarea",
        canConfirm = title.isNotBlank(),
        onConfirm = ::save,
        onDismiss = onDismiss,
    ) {
        TitleField(title, { title = it }, "Qué hay que hacer", onDone = ::save)
        Picker(
            "Proyecto",
            listOf<Pair<String?, String>>(null to "Tarea suelta") + projects.map { it.id to projectLabel(it) },
            projectId,
        ) { projectId = it; milestoneId = null }
        if (projectMilestones.isNotEmpty()) {
            Picker(
                "Hito",
                listOf<Pair<String?, String>>(null to "Sin hito") +
                    projectMilestones.mapIndexed { i, m -> m.id to "H${i + 1} · ${m.title}" },
                milestoneId,
            ) { milestoneId = it }
        }
        DateField("Fecha límite", due, { due = it })
        Picker(
            "Estimación",
            (0..6).map { it to if (it == 0) "Sin estimar" else "$it bloque${if (it > 1) "s" else ""} de 50 min" },
            blocks,
        ) { blocks = it }
        if (task == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(planToday, { planToday = it })
                Text("Añadir al plan de hoy")
            }
        }
    }
}

// ---------------------------------------------------------------- proyecto

@Composable
fun ProjectDialog(project: Project?, initialKind: ProjectKind, onDismiss: () -> Unit, onCreated: (String) -> Unit = {}) {
    val act = rememberActions()
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var kind by remember { mutableStateOf(project?.kindEnum ?: initialKind) }
    var due by remember { mutableStateOf(project?.due_date?.let(LocalDate::parse)) }

    fun save() {
        if (name.isBlank()) return
        if (project == null) act { onCreated(addProject(name, kind, description, due)) }
        else act { updateProject(project.id, name, kind, description, due) }
        onDismiss()
    }

    FormDialog(
        title = if (project == null) (if (kind == ProjectKind.SUBJECT) "Nueva asignatura" else "Nuevo proyecto") else "Editar",
        canConfirm = name.isNotBlank(),
        onConfirm = ::save,
        onDismiss = onDismiss,
    ) {
        TitleField(name, { name = it }, "Nombre", onDone = ::save)
        OutlinedTextField(description, { description = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
        Picker(
            "Tipo",
            listOf(ProjectKind.PERSONAL to "Proyecto personal", ProjectKind.SUBJECT to "Asignatura", ProjectKind.TFG to "TFG"),
            kind,
        ) { kind = it }
        DateField("Fecha de entrega", due, { due = it })
    }
}

// ---------------------------------------------------------------- hito

@Composable
fun MilestoneDialog(projectId: String, milestone: Milestone?, onDismiss: () -> Unit) {
    val act = rememberActions()
    val today = LocalToday.current
    var title by remember { mutableStateOf(milestone?.title ?: "") }
    var deliverable by remember { mutableStateOf(milestone?.deliverable ?: "") }
    var due by remember { mutableStateOf<LocalDate?>(milestone?.due_date?.let(LocalDate::parse) ?: today.plusDays(14)) }

    fun save() {
        if (title.isBlank()) return
        val d = due ?: return
        if (milestone == null) act { addMilestone(projectId, title, d, deliverable) }
        else act { updateMilestone(milestone.id, title, d, deliverable) }
        onDismiss()
    }

    FormDialog(
        title = if (milestone == null) "Nuevo hito" else "Editar hito",
        canConfirm = title.isNotBlank() && due != null,
        onConfirm = ::save,
        onDismiss = onDismiss,
    ) {
        TitleField(title, { title = it }, "Nombre del hito", onDone = ::save)
        OutlinedTextField(deliverable, { deliverable = it }, label = { Text("Entregable") }, modifier = Modifier.fillMaxWidth())
        DateField("Fecha", due, { due = it }, allowNone = false)
    }
}

// ---------------------------------------------------------------- entrega/examen

@Composable
fun DeadlineDialog(projects: List<Project>, initialProjectId: String?, onDismiss: () -> Unit) {
    val act = rememberActions()
    val today = LocalToday.current
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(DeadlineKind.DELIVERY) }
    var due by remember { mutableStateOf<LocalDate?>(today.plusDays(7)) }
    var projectId by remember { mutableStateOf(initialProjectId) }

    fun save() {
        val d = due ?: return
        if (title.isBlank()) return
        act { addDeadline(title, kind, d, projectId) }
        onDismiss()
    }

    FormDialog("Nueva entrega o examen", canConfirm = title.isNotBlank() && due != null, onConfirm = ::save, onDismiss = onDismiss) {
        TitleField(title, { title = it }, "Qué es (p. ej. Práctica 3)", onDone = ::save)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DeadlineKind.entries.forEach { k ->
                FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
            }
        }
        Picker(
            "Asignatura o proyecto",
            listOf<Pair<String?, String>>(null to "Ninguno") + projects.map { it.id to projectLabel(it) },
            projectId,
        ) { projectId = it }
        DateField("Fecha", due, { due = it }, allowNone = false)
    }
}

// ---------------------------------------------------------------- hábito

@Composable
fun HabitDialog(habit: dev.lifeos.db.Habit?, onDismiss: () -> Unit) {
    val act = rememberActions()
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var period by remember { mutableStateOf(habit?.period?.let { HabitPeriod.valueOf(it) } ?: HabitPeriod.DAY) }
    var target by remember { mutableStateOf(habit?.target?.toInt() ?: 1) }
    var targetMax by remember { mutableStateOf(habit?.target_max?.toInt() ?: 0) }

    fun save() {
        if (name.isBlank()) return
        val max = targetMax.takeIf { it > target }
        if (habit == null) act { addHabit(name, period, target, max) }
        else act { updateHabit(habit.id, name, period, target, max) }
        onDismiss()
    }

    val unit = if (period == HabitPeriod.DAY) "al día" else "a la semana"
    FormDialog(if (habit == null) "Nuevo hábito" else "Editar hábito", canConfirm = name.isNotBlank(), onConfirm = ::save, onDismiss = onDismiss) {
        TitleField(name, { name = it }, "Nombre", onDone = ::save)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = period == HabitPeriod.DAY, onClick = { period = HabitPeriod.DAY }, label = { Text("Cada día") })
            FilterChip(selected = period == HabitPeriod.WEEK, onClick = { period = HabitPeriod.WEEK }, label = { Text("Cada semana") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Picker("Objetivo", (1..10).map { it to "$it $unit" }, target) { target = it; if (targetMax in 1..it) targetMax = 0 }
            Picker("Ideal (opcional)", listOf(0 to "Ninguno") + (target + 1..12).map { it to "$it $unit" }, targetMax) { targetMax = it }
        }
    }
}

// ---------------------------------------------------------------- captura rápida

@Composable
fun CaptureDialog(onDismiss: () -> Unit) {
    val act = rememberActions()
    val source = LocalSource.current
    var text by remember { mutableStateOf("") }
    fun save() {
        if (text.isBlank()) return
        val t = text
        act { capture(t, source) }
        onDismiss()
    }
    FormDialog("Apuntar en la bandeja", confirmLabel = "Apuntar", canConfirm = text.isNotBlank(), onConfirm = ::save, onDismiss = onDismiss) {
        TitleField(text, { text = it }, "Lo que se te pase por la cabeza", onDone = ::save)
    }
}

// ---------------------------------------------------------------- elegir tarea

@Composable
fun TaskPickerDialog(
    title: String,
    tasks: List<Task>,
    describe: (Task) -> String?,
    emptyText: String,
    onPick: (Task) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (tasks.isEmpty()) {
                EmptyText(emptyText)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(tasks, key = { it.id }) { t ->
                        ListItem(
                            headlineContent = { Text(t.title) },
                            supportingContent = describe(t)?.let { { Text(it) } },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.padding(bottom = 4.dp).clickableItem { onPick(t); onDismiss() },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onCreateNew != null) TextButton(onClick = { onDismiss(); onCreateNew() }) { Text("Nueva tarea") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

private fun Modifier.clickableItem(onClick: () -> Unit) = this.clickable(onClick = onClick)

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
