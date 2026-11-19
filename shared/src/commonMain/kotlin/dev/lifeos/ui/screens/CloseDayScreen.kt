package dev.lifeos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.dueDate
import dev.lifeos.data.isDone
import dev.lifeos.data.periodEnum
import dev.lifeos.db.Task
import dev.lifeos.ui.LocalPrefs
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.TaskDialog
import dev.lifeos.ui.rememberActions
import kotlinx.coroutines.launch
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil

/** "2 h 30", "45 min" */
fun durationText(ms: Long): String {
    val min = ms / 60_000
    return if (min >= 60) "${min / 60} h ${(min % 60).toString().padStart(2, '0')}" else "$min min"
}

private data class Candidate(val task: Task, val why: String)

/** Cierre del día (ritual de la noche): cómo fue, qué toca mañana y qué será lo primero. */
@Composable
fun CloseDayScreen(onClose: () -> Unit) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val tomorrow = today.plusDays(1)
    val prefs = LocalPrefs.current
    val act = rememberActions()
    val cs = MaterialTheme.colorScheme

    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val habits by repo.habits.collectAsState(emptyList())
    val counts by remember(today) { repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today) }.collectAsState(emptyMap())
    val dayStart = today.atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val focus by remember(today) { repo.focusBetween(dayStart, dayStart + 86_400_000) }.collectAsState(emptyList())
    val usage by remember(today) { repo.usageBetween(today, today) }.collectAsState(emptyList())
    val projectById = projects.associateBy { it.id }

    val planned = tasks.filter { it.planned_date == today.toString() }
    val pending = planned.filter { !it.isDone }
    val doneToday = tasks.filter { t -> t.done_at?.let { it >= dayStart } == true }
    val focusMs = focus.filter { it.ended_at != null }.sumOf { it.planned_ms }
    val blocks = focus.count { it.ended_at != null }
    val firstBlock = focus.minByOrNull { it.started_at }

    // Candidatas para mañana: lo que quedó de hoy, lo que vence pronto y el resto.
    val candidates = remember(tasks, today) {
        val open = tasks.filter { !it.isDone }
        val left = open.filter { it.planned_date == today.toString() }.map { Candidate(it, "quedó de hoy") }
        val already = open.filter { it.planned_date == tomorrow.toString() }.map { Candidate(it, "ya estaba para mañana") }
        val soon = open.filter { it !in left.map { c -> c.task } && it !in already.map { c -> c.task } && it.dueDate != null }
            .sortedBy { it.due_date }
            .take(5)
            .map { t ->
                val d = today.daysUntil(t.dueDate!!)
                Candidate(t, when { d < 0 -> "atrasada"; d == 0 -> "vence hoy"; d == 1 -> "vence mañana"; else -> "vence en $d días" })
            }
        val rest = open.filter { t -> listOf(left, already, soon).none { l -> l.any { it.task == t } } }
            .take(6).map { Candidate(it, taskContext(it, projectById, milestones)) }
        left + already + soon + rest
    }
    val chosen = remember { mutableStateListOf<String>() }
    var first by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    if (!initialized && candidates.isNotEmpty()) {
        // Se preseleccionan las que ya estaban para mañana (hasta 3).
        candidates.filter { it.why == "ya estaba para mañana" }.take(3).forEach { chosen += it.task.id }
        first = chosen.firstOrNull()
        initialized = true
    }
    var newTask by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    RitualFrame(
        top = "${Dates.long(today)} · ${Dates.time(Dates.nowMs())} · unos 2 minutos",
        onExit = onClose,
        exitLabel = "Ahora no",
    ) {
        Text("Cerremos el día", style = MaterialTheme.typography.headlineMedium)
        Text("Sin juicios: mira lo que pasó, elige lo de mañana y a dormir.", color = cs.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))

        // 1 · Cómo fue hoy
        RitualSection("1", "Cómo fue hoy") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${planned.count { it.isDone }} / ${planned.size}", "Plan cumplido")
                Stat(durationText(focusMs), "Foco · $blocks bloque${if (blocks == 1) "" else "s"}")
                usage.firstOrNull()?.let { Stat(durationText(it.scroll_ms), "Scroll (de ${prefs.scrollLimitMin} min)") }
            }
            Spacer(Modifier.height(12.dp))
            doneToday.forEach { t -> Line(true, t.title, t.done_at?.let { Dates.time(it) }) }
            habits.forEach { h ->
                val c = counts[h.id] ?: emptyMap()
                if (h.periodEnum == HabitPeriod.DAY && HabitMath.current(h, c, today) >= h.target) Line(true, h.name, "hábito")
                else if (h.periodEnum == HabitPeriod.WEEK && (c[today] ?: 0) > 0) Line(true, h.name, "hábito")
            }
            pending.forEach { t -> Line(false, t.title, if (t.id in chosen) "pasa a mañana" else "sale del plan") }
            if (doneToday.isEmpty() && pending.isEmpty()) Text("Hoy no había nada en el plan.", color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                firstBlock?.let { "Primer bloque a las ${Dates.time(it.started_at)}." } ?: "Hoy no hubo bloques de enfoque. Mañana, uno basta.",
                style = MaterialTheme.typography.bodyMedium, color = cs.primary,
            )
        }

        // 2 · Mañana
        RitualSection("2", "Mañana, ${Dates.dayName(tomorrow)} · elige hasta 3", right = "${chosen.size} / 3") {
            if (candidates.isEmpty()) Text("No tienes tareas pendientes.", color = cs.onSurfaceVariant)
            candidates.forEach { c ->
                val on = c.task.id in chosen
                val enabled = on || chosen.size < 3
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            if (on) { chosen -= c.task.id; if (first == c.task.id) first = chosen.firstOrNull() }
                            else { chosen += c.task.id; if (first == null) first = c.task.id }
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(on, null, enabled = enabled)
                    Column(Modifier.weight(1f)) {
                        Text(c.task.title, color = if (enabled) cs.onSurface else cs.onSurfaceVariant)
                        Text(c.why, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            TextButton(onClick = { newTask = true }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Nueva tarea")
            }
        }

        // 3 · Lo primero
        RitualSection("3", "¿Qué será lo primero?") {
            if (chosen.isEmpty()) Text("Elige antes algo para mañana.", color = cs.onSurfaceVariant)
            chosen.forEach { id ->
                val t = tasks.firstOrNull { it.id == id } ?: return@forEach
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { first = id },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(first == id, { first = id })
                    Text(t.title, fontWeight = if (first == id) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Text(
                "Mañana, al abrir la app, lo verás arriba con un botón para empezar.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val list = chosen.toList()
                val f = first
                scope.launch { repo.closeDay(today, tomorrow, list, f); onClose() }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text("Cerrar el día") }
        Spacer(Modifier.height(24.dp))
    }

    if (newTask) {
        TaskDialog(null, projects, milestones, onDismiss = { newTask = false })
    }
}

// ------------------------------------------------------------------ piezas de los rituales

/** Pantalla completa de un ritual: barra superior con salida y columna centrada con scroll. */
@Composable
fun RitualFrame(top: String, onExit: () -> Unit, exitLabel: String = "Salir", content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(top, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = onExit) { Text(exitLabel) }
                }
                content()
            }
        }
    }
}

@Composable
fun RitualSection(number: String, title: String, right: String? = null, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Surface(color = cs.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(26.dp)) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(number, style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (right != null) Text(right, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
fun Stat(value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).widthIn(min = 96.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun Line(done: Boolean, text: String, meta: String?) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Outlined.Check else Icons.Outlined.Remove, null, Modifier.size(18.dp), tint = if (done) cs.tertiary else cs.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text, modifier = Modifier.weight(1f), color = if (done) cs.onSurface else cs.onSurfaceVariant)
        if (meta != null) Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

