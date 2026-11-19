package dev.lifeos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lifeos.data.CalendarMath
import dev.lifeos.data.DayItem
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.Prefs
import dev.lifeos.data.isDone
import dev.lifeos.data.periodEnum
import dev.lifeos.ui.DeadlineDialog
import dev.lifeos.ui.LocalPrefs
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.rememberActions
import kotlinx.coroutines.launch
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val STEPS = listOf("Cómo fue la semana", "Qué se acerca", "Vaciar la bandeja", "Tres metas", "Ajustes")

/** Revisión semanal (ritual del domingo), en cinco pasos. */
@Composable
fun ReviewScreen(onClose: () -> Unit) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val prefs = LocalPrefs.current
    val act = rememberActions()
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val week = Dates.weekStart(today)
    val prevWeek = week.plusDays(-7)
    val weekEnd = week.plusDays(6)

    val review by remember(week) { repo.weekReview(week) }.collectAsState(null)
    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val deadlines by repo.deadlines.collectAsState(emptyList())
    val habits by repo.habits.collectAsState(emptyList())
    val counts by remember(today) { repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today) }.collectAsState(emptyMap())
    val fromMs = prevWeek.atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val toMs = weekEnd.plusDays(1).atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val focus by remember(week) { repo.focusBetween(fromMs, toMs) }.collectAsState(emptyList())
    val usage by remember(week) { repo.usageBetween(week, weekEnd) }.collectAsState(emptyList())

    var step by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    val goals = remember { mutableStateListOf("", "", "") }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(review) {
        if (!loaded && review != null) {
            note = review?.note ?: ""
            review?.goals?.split('\n')?.take(3)?.forEachIndexed { i, g -> goals[i] = g }
            loaded = true
        }
    }
    var newDeadline by remember { mutableStateOf(false) }

    fun save() = act { saveWeekReview(week, note, goals.toList()) }

    RitualFrame(
        top = "Revisión semanal · ${Dates.dayName(today).replaceFirstChar { it.uppercase() }} ${Dates.short(today)} · unos 12 minutos",
        onExit = { save(); onClose() },
    ) {
        // Pasos
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            STEPS.forEachIndexed { i, t ->
                Surface(
                    color = if (i == step) cs.primaryContainer else cs.surfaceContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { step = i },
                ) {
                    Text(
                        (if (i < step) "✓ " else "${i + 1} · ") + t,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (i == step) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        fun dateOf(ms: Long) = Instant.fromEpochMilliseconds(ms).toLocalDateTime(Dates.zone).date
        val done = focus.filter { it.ended_at != null }
        val thisFocus = done.filter { dateOf(it.started_at) >= week }.sumOf { it.planned_ms }
        val lastFocus = done.filter { dateOf(it.started_at) < week }.sumOf { it.planned_ms }

        when (step) {
            0 -> RitualSection("1", "Cómo fue la semana") {
                val weekPlanned = tasks.filter { t -> t.planned_date?.let { it >= week.toString() && it <= weekEnd.toString() } == true }
                val diff = thisFocus - lastFocus
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(durationText(thisFocus), "Foco · " + (if (diff >= 0) "+" else "−") + durationText(kotlin.math.abs(diff)) + " vs. anterior")
                    Stat(if (weekPlanned.isEmpty()) "—" else "${weekPlanned.count { it.isDone } * 100 / weekPlanned.size} %", "Plan cumplido")
                    habits.forEach { h ->
                        val c = counts[h.id] ?: emptyMap()
                        if (h.periodEnum == HabitPeriod.WEEK) Stat("${HabitMath.current(h, c, today)} / ${h.target}", h.name)
                        else {
                            val days = (week.daysUntil(today) + 1).coerceAtLeast(1)
                            val total = (0 until days).sumOf { c[week.plusDays(it)] ?: 0 }
                            Stat(((total * 10 / days) / 10.0).toString().replace('.', ','), "${h.name} al día")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val byDay = (0..6).map { week.plusDays(it) }.filter { it <= today }.map { d -> d to done.filter { dateOf(it.started_at) == d }.sumOf { it.planned_ms } }
                byDay.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.let { (d, ms) ->
                    Text("Mejor día: ${Dates.dayName(d)} · ${durationText(ms)} de foco", style = MaterialTheme.typography.bodyMedium)
                }
                val scroll = usage.sumOf { it.scroll_ms }
                if (usage.isNotEmpty()) {
                    Text("Scroll total: ${durationText(scroll)} · de ${durationText(prefs.scrollLimitMs * 7)} permitidas", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    note, { note = it }, label = { Text("¿Algo que quieras recordar de esta semana?") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
            }
            1 -> RitualSection("2", "Qué se acerca") {
                val items = CalendarMath.dayItems(deadlines, milestones, tasks, projects.associateBy { it.id }, today, today.plusDays(21))
                    .filter { !it.done && it.kind != DayItem.Kind.PLANNED }
                if (items.isEmpty()) Text("Nada en las próximas tres semanas.", color = cs.onSurfaceVariant)
                items.forEach { item ->
                    val d = today.daysUntil(item.date)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$d d", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                            color = if (d <= 3 && item.kind != DayItem.Kind.MILESTONE) cs.error else cs.onSurfaceVariant,
                            modifier = Modifier.width(44.dp))
                        if (item.kind == DayItem.Kind.MILESTONE) { Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = cs.primary); Spacer(Modifier.width(4.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(item.title)
                            Text("${Dates.dayName(item.date)} ${Dates.short(item.date)}" + (item.sub.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("¿Hay entregas nuevas en Blackboard que no estén aquí?", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                TextButton(onClick = { newDeadline = true }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Añadir entrega o examen") }
            }
            2 -> RitualSection("3", "Vaciar la bandeja") {
                Text("Decide qué es cada cosa: tarea, nota o nada.", color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                InboxScreen()
            }
            3 -> RitualSection("4", "Tres metas para la semana ${isoWeekOf(week.plusDays(7))}") {
                Text("Lo que haría que la semana fuera buena.", color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                (0..2).forEach { i ->
                    OutlinedTextField(goals[i], { goals[i] = it }, label = { Text("Meta ${i + 1}") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                val suggestions = buildList {
                    milestones.filter { m -> projects.any { it.id == m.project_id } }.minByOrNull { it.due_date }
                        ?.takeIf { kotlinx.datetime.LocalDate.parse(it.due_date) <= today.plusDays(14) }?.let { add("Cerrar «${it.title}»") }
                    deadlines.filter { it.done_at == null && kotlinx.datetime.LocalDate.parse(it.due_date) in today..today.plusDays(9) }.take(2).forEach { add("Entregar «${it.title}»") }
                    habits.filter { it.periodEnum == HabitPeriod.WEEK }.forEach { add("${it.target} veces ${it.name.lowercase()}") }
                }.filter { s -> goals.none { it == s } }
                if (suggestions.isNotEmpty()) {
                    Text("Ideas (toca para usar):", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { sug ->
                            OutlinedButton(onClick = {
                                val i = goals.indexOfFirst { it.isBlank() }
                                if (i >= 0) goals[i] = sug
                            }) { Text(sug) }
                        }
                    }
                }
            }
            4 -> RitualSection("5", "Ajustes de la semana") {
                val scroll = usage.sumOf { it.scroll_ms }
                val allowed = prefs.scrollLimitMs * 7
                var any = false
                if (usage.isNotEmpty() && scroll < allowed * 6 / 10 && prefs.scrollLimitMin > 15) {
                    any = true
                    val next = prefs.scrollLimitMin - 15
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Límite de scroll: has usado el ${scroll * 100 / allowed} %. ¿Bajamos de ${prefs.scrollLimitMin} a $next min al día?", modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { act { putPref(Prefs.SCROLL_LIMIT, next.toString()) } }) { Text("Bajar") }
                    }
                }
                val firstHours = done.filter { dateOf(it.started_at) >= week }.groupBy { dateOf(it.started_at) }.values
                    .map { l -> l.minOf { it.started_at } }
                    .map { Instant.fromEpochMilliseconds(it).toLocalDateTime(Dates.zone).hour }
                if (firstHours.size >= 3) {
                    any = true
                    Text("Tu primer bloque esta semana empezó, de media, hacia las ${firstHours.average().toInt()}:00. Apúntalo como referencia para la semana que viene.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (!any) Text("Nada que ajustar esta semana.", color = cs.onSurfaceVariant)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { save(); step-- }) { Text("Anterior") }
            Spacer(Modifier.weight(1f))
            if (step < STEPS.lastIndex) Button(onClick = { save(); step++ }) { Text("Siguiente") }
            else Button(onClick = {
                scope.launch {
                    repo.saveWeekReview(week, note, goals.toList())
                    repo.finishWeekReview(week)
                    onClose()
                }
            }) { Text("Terminar revisión") }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (newDeadline) DeadlineDialog(projects, null) { newDeadline = false }
}

private fun isoWeekOf(d: kotlinx.datetime.LocalDate): Int = (Dates.weekStart(d).plusDays(3).dayOfYear - 1) / 7 + 1
