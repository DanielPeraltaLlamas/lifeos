package dev.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lifeos.data.CalendarMath
import dev.lifeos.data.Dates
import dev.lifeos.data.EventKind
import dev.lifeos.data.Occurrence
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.dueDate
import dev.lifeos.data.isDone
import dev.lifeos.data.kindEnum
import dev.lifeos.data.periodEnum
import dev.lifeos.db.Habit
import dev.lifeos.db.Milestone
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import dev.lifeos.ui.Card
import dev.lifeos.ui.CaptureDialog
import dev.lifeos.ui.Chip
import dev.lifeos.ui.DeadlineDialog
import dev.lifeos.ui.EmptyText
import dev.lifeos.ui.EventDialog
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.MenuAction
import dev.lifeos.ui.SyncIconButton
import dev.lifeos.ui.TaskDialog
import dev.lifeos.ui.TaskPickerDialog
import dev.lifeos.ui.TaskRow
import dev.lifeos.ui.rememberActions
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Texto corto que describe de dónde es una tarea: "TFG · H1", "Calidad", "Tarea suelta". */
fun taskContext(t: Task, projects: Map<String, Project>, milestones: List<Milestone>): String {
    val p = t.project_id?.let(projects::get) ?: return "Tarea suelta"
    val ms = milestones.filter { it.project_id == p.id }
    val idx = ms.indexOfFirst { it.id == t.milestone_id }
    return if (idx >= 0) "${p.name} · H${idx + 1}" else p.name
}

fun estimateText(t: Task, blockMin: Int = 50): String? = t.estimate_blocks?.toInt()?.takeIf { it > 0 }?.let {
    if (it == 1) "1 bloque × $blockMin min" else "$it bloques × $blockMin min"
}

@Composable
fun TodayScreen(
    wide: Boolean,
    onOpenHabits: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenRitual: (dev.lifeos.Ritual) -> Unit = {},
    extras: (@Composable () -> Unit)? = null,
) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val act = rememberActions()

    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val deadlines by repo.deadlines.collectAsState(emptyList())
    val habits by repo.habits.collectAsState(emptyList())
    val events by repo.events.collectAsState(emptyList())
    val plan by remember(today) { repo.dailyPlan(today) }.collectAsState(null)
    val counts by remember(today) { repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today) }.collectAsState(emptyMap())

    val projectById = projects.associateBy { it.id }
    val first = plan?.first_task_id?.let { id -> tasks.firstOrNull { it.id == id } }
    val planned = tasks.filter { it.planned_date == today.toString() }
    val todayOccs = CalendarMath.occurrences(events, today, today)
    val classes = todayOccs.filter { it.event.kindEnum == EventKind.CLASS }
    val dayNote = when {
        classes.isEmpty() -> "sin clase"
        classes.size == 1 -> "1 clase a las ${CalendarMath.hm(classes[0].start)}"
        else -> "${classes.size} clases desde las ${CalendarMath.hm(classes[0].start)}"
    }

    var dialog by remember { mutableStateOf<TodayDialog?>(null) }
    var planStudyFor by remember { mutableStateOf<String?>(null) }
    val todayStart = todayStartMs()

    val hour = Dates.hourNow()
    val greeting = when (hour) { in 6..13 -> "Buenos días"; in 14..20 -> "Buenas tardes"; else -> "Buenas noches" }

    Column {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Column(Modifier.weight(1f)) {
                Text(greeting, style = MaterialTheme.typography.headlineMedium)
                Text(Dates.long(today) + " · " + dayNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (wide) {
                OutlinedButton(onClick = { dialog = TodayDialog.Capture }) { Text("Apuntar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { dialog = TodayDialog.NewTask }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nueva tarea")
                }
            } else {
                SyncIconButton()
                IconButton(onClick = { dialog = TodayDialog.Capture }) { Icon(Icons.Outlined.Edit, "Apuntar en la bandeja") }
                FilledTonalIconButton(onClick = { dialog = TodayDialog.NewTask }) { Icon(Icons.Outlined.Add, "Nueva tarea") }
            }
        }

        FirstCard(
            task = first,
            context = first?.let { taskContext(it, projectById, milestones) },
            chosenAt = plan?.chosen_at,
            wide = wide,
            onChoose = { dialog = TodayDialog.PickFirst },
            onStart = { first?.let { t -> act { startFocus(t.title, t.id, t.project_id, todayStart) } } },
            onDone = { done -> first?.let { t -> act { setTaskDone(t.id, done) } } },
        )
        Spacer(Modifier.height(14.dp))
        RitualBanner(plan?.closed_at, onOpenRitual)
        AssistantCard(
            onPlanStudy = { pid -> dialog = null; planStudyFor = pid; dialog = TodayDialog.PlanStudy },
        )
        if (extras != null) {
            extras()
            Spacer(Modifier.height(14.dp))
        }

        val upcoming: @Composable (Modifier) -> Unit = { m ->
            UpcomingCard(today, deadlines, milestones, projectById, tasks, m, onAdd = { dialog = TodayDialog.NewDeadline })
        }
        val agenda: @Composable (Modifier) -> Unit = { m ->
            AgendaCard(todayOccs, projectById, m, onOpen = onOpenCalendar, onAdd = { dialog = TodayDialog.NewEvent })
        }
        val planCard: @Composable (Modifier) -> Unit = { m ->
            PlanCard(
                planned, projectById, milestones, m,
                onToggle = { t, d -> act { setTaskDone(t.id, d) } },
                onRemove = { t -> act { setTaskPlanned(t.id, null) } },
                onAdd = { dialog = TodayDialog.AddToPlan },
                onStart = { t -> act { startFocus(t.title, t.id, t.project_id, todayStart) } },
            )
        }
        val habitsCard: @Composable (Modifier) -> Unit = { m ->
            HabitsCard(habits, counts, today, m, onOpenHabits,
                onLog = { h -> act { logHabit(h.id, today) } },
                onUnlog = { h -> act { unlogHabit(h.id, today) } })
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 14.dp
            if (maxWidth >= 560.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        agenda(Modifier.weight(1.2f).fillMaxHeight())
                        upcoming(Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        planCard(Modifier.weight(1.2f).fillMaxHeight())
                        habitsCard(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    planCard(Modifier.fillMaxWidth())
                    agenda(Modifier.fillMaxWidth())
                    habitsCard(Modifier.fillMaxWidth())
                    upcoming(Modifier.fillMaxWidth())
                }
            }
        }
    }

    when (dialog) {
        TodayDialog.Capture -> CaptureDialog { dialog = null }
        TodayDialog.NewTask -> TaskDialog(null, projects, milestones, onDismiss = { dialog = null })
        TodayDialog.NewDeadline -> DeadlineDialog(projects, null) { dialog = null }
        TodayDialog.NewEvent -> EventDialog(null, projects, today, initialStart = ((Dates.hourNow() + 1).coerceAtMost(22)) * 60) { dialog = null }
        TodayDialog.PlanStudy -> EventDialog(
            null, projects, today, initialStart = ((Dates.hourNow() + 1).coerceAtMost(22)) * 60,
            initialKind = EventKind.STUDY, initialProject = planStudyFor,
        ) { dialog = null }
        TodayDialog.PickFirst -> {
            val open = tasks.filter { !it.isDone }
                .sortedWith(compareBy<Task> { it.planned_date != today.toString() }.thenBy { it.due_date ?: "9999" })
            TaskPickerDialog(
                title = "¿Qué es lo primero hoy?",
                tasks = open,
                describe = { listOfNotNull(taskContext(it, projectById, milestones), estimateText(it)).joinToString(" · ") },
                emptyText = "No tienes tareas pendientes. Crea una primero.",
                onPick = { t -> act { setFirstTask(today, t.id) } },
                onDismiss = { dialog = null },
                onCreateNew = { dialog = TodayDialog.NewTask },
            )
        }
        TodayDialog.AddToPlan -> {
            val candidates = tasks.filter { !it.isDone && it.planned_date != today.toString() }
            TaskPickerDialog(
                title = "Añadir al plan de hoy",
                tasks = candidates,
                describe = { taskContext(it, projectById, milestones) },
                emptyText = "No quedan tareas pendientes fuera del plan.",
                onPick = { t -> act { setTaskPlanned(t.id, today) } },
                onDismiss = { dialog = null },
                onCreateNew = { dialog = TodayDialog.NewTask },
            )
        }
        null -> {}
    }
}

/** Avisos del asistente por reglas (solo si hay algo que decir). */
@Composable
private fun AssistantCard(onPlanStudy: (String?) -> Unit) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val prefs = dev.lifeos.ui.LocalPrefs.current
    val deadlines by repo.deadlines.collectAsState(emptyList())
    val events by repo.events.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val tasks by repo.tasks.collectAsState(emptyList())
    val habits by repo.habits.collectAsState(emptyList())
    val inbox by repo.inbox.collectAsState(emptyList())
    val counts by remember(today) { repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today) }.collectAsState(emptyMap())
    val dayStart = todayStartMs()
    val focus by remember(dayStart) { repo.focusBetween(dayStart, dayStart + 86_400_000) }.collectAsState(emptyList())
    val usage by remember(today) { repo.usageBetween(today, today) }.collectAsState(emptyList())
    val hints = dev.lifeos.data.Assistant.hints(
        dev.lifeos.data.AssistantInput(
            today, Dates.hourNow(), deadlines, events, milestones, projects, tasks, habits, counts,
            inbox.size, focus, usage.firstOrNull()?.scroll_ms, prefs,
        ),
    )
    if (hints.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    Card("Asistente", Modifier.fillMaxWidth().padding(bottom = 14.dp), icon = Icons.Outlined.TipsAndUpdates) {
        hints.forEach { h ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (h.kind == dev.lifeos.data.Hint.Kind.PLAN_STUDY) {
                    TextButton(onClick = { onPlanStudy(h.projectId) }) { Text("Planificar") }
                }
            }
        }
    }
}

/** Por la noche propone el cierre del día; el día de la revisión, la revisión semanal. */
@Composable
private fun RitualBanner(closedAt: Long?, onOpen: (dev.lifeos.Ritual) -> Unit) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val prefs = dev.lifeos.ui.LocalPrefs.current
    val weekStart = Dates.weekStart(today)
    val review by remember(weekStart) { repo.weekReview(weekStart) }.collectAsState(null)
    val hour = Dates.hourNow()
    val showClose = closedAt == null && hour >= prefs.closeHour
    val showReview = review?.done_at == null && today.dayOfWeek.ordinal + 1 == prefs.reviewDay && hour >= 9
    if (!showClose && !showReview) return
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
        if (showReview) {
            Card(null, Modifier.fillMaxWidth(), color = cs.tertiaryContainer, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Revisión semanal", style = MaterialTheme.typography.titleSmall, color = cs.onTertiaryContainer)
                        Text("10–15 minutos para mirar la semana y elegir tres metas.", style = MaterialTheme.typography.bodySmall, color = cs.onTertiaryContainer)
                    }
                    Button(onClick = { onOpen(dev.lifeos.Ritual.REVIEW) }) { Text("Empezar") }
                }
            }
        }
        if (showClose) {
            Card(null, Modifier.fillMaxWidth(), color = cs.secondaryContainer, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Cierra el día", style = MaterialTheme.typography.titleSmall, color = cs.onSecondaryContainer)
                        Text("Unos 2 minutos: qué tal fue y qué será lo primero mañana.", style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
                    }
                    Button(onClick = { onOpen(dev.lifeos.Ritual.CLOSE) }) { Text("Cerrar") }
                }
            }
        }
    }
}

private enum class TodayDialog { Capture, NewTask, NewDeadline, NewEvent, PlanStudy, PickFirst, AddToPlan }

@Composable
private fun FirstCard(
    task: Task?,
    context: String?,
    chosenAt: Long?,
    wide: Boolean,
    onChoose: () -> Unit,
    onDone: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        title = null,
        modifier = Modifier.fillMaxWidth(),
        padding = if (wide) 28.dp else 20.dp,
        color = cs.primaryContainer,
    ) {
        Text(
            "Lo primero" + (chosenAt?.let { " · elegido a las ${Dates.time(it)}" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = cs.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(8.dp))
        if (task == null) {
            Text(
                "Elige una sola cosa para empezar el día.",
                style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                color = cs.onPrimaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onChoose) { Text("Elegir lo primero") }
        } else {
            val done = task.isDone
            Text(
                task.title,
                style = if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                color = cs.onPrimaryContainer,
                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                context?.let { Chip(it, color = cs.surfaceContainerHighest.copy(alpha = 0.6f)) }
                estimateText(task)?.let { Chip(it, color = cs.surfaceContainerHighest.copy(alpha = 0.6f)) }
            }
            Spacer(Modifier.height(18.dp))
            if (done) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Check, null, tint = cs.onPrimaryContainer)
                Text("Hecho. Lo siguiente está en tu plan.", color = cs.onPrimaryContainer, modifier = Modifier.weight(1f, fill = false))
                TextButton(onClick = { onDone(false) }) { Text("Deshacer") }
            } else {
                // En el móvil no caben los tres en una fila: pasan a la siguiente.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart) { Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Empezar · ${dev.lifeos.ui.LocalPrefs.current.blockMin} min") }
                    OutlinedButton(onClick = { onDone(true) }) { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Hecha") }
                    OutlinedButton(onClick = onChoose) { Text("Cambiar") }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    planned: List<Task>,
    projects: Map<String, Project>,
    milestones: List<Milestone>,
    modifier: Modifier,
    onToggle: (Task, Boolean) -> Unit,
    onRemove: (Task) -> Unit,
    onAdd: () -> Unit,
    onStart: (Task) -> Unit,
) {
    val done = planned.count { it.isDone }
    val blockMin = dev.lifeos.ui.LocalPrefs.current.blockMin
    Card(
        "Plan de hoy", modifier, icon = Icons.Outlined.ChecklistRtl,
        action = {
            if (planned.isNotEmpty()) Text("$done de ${planned.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    ) {
        if (planned.isNotEmpty()) {
            LinearProgressIndicator(progress = { done.toFloat() / planned.size }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
        if (planned.isEmpty()) EmptyText("Aún no hay nada en el plan. Mejor 1–3 cosas que diez.")
        planned.sortedBy { it.isDone }.forEach { t ->
            TaskRow(
                title = t.title,
                meta = listOfNotNull(taskContext(t, projects, milestones), estimateText(t)).joinToString(" · "),
                done = t.isDone,
                onToggle = { onToggle(t, it) },
                accent = projectAccent(t.project_id?.let(projects::get)),
                menu = listOfNotNull(
                    if (!t.isDone) MenuAction("Empezar bloque · $blockMin min") { onStart(t) } else null,
                    MenuAction("Quitar del plan") { onRemove(t) },
                ),
            )
        }
        Spacer(Modifier.weight(1f, fill = true).height(0.dp))
        TextButton(onClick = onAdd) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Añadir al plan") }
    }
}

@Composable
fun projectAccent(p: Project?): Color = when (p?.kindEnum) {
    dev.lifeos.data.ProjectKind.TFG -> MaterialTheme.colorScheme.primary
    dev.lifeos.data.ProjectKind.PERSONAL -> MaterialTheme.colorScheme.secondary
    dev.lifeos.data.ProjectKind.SUBJECT -> MaterialTheme.colorScheme.outline
    null -> MaterialTheme.colorScheme.outlineVariant
}

private data class UpcomingItem(val date: LocalDate, val title: String, val sub: String, val kind: Kind) {
    enum class Kind { DEADLINE, MILESTONE, TASK }
}

@Composable
private fun UpcomingCard(
    today: LocalDate,
    deadlines: List<dev.lifeos.db.Deadline>,
    milestones: List<Milestone>,
    projects: Map<String, Project>,
    tasks: List<Task>,
    modifier: Modifier,
    onAdd: () -> Unit,
) {
    val horizon = today.plusDays(30)
    val items = buildList {
        deadlines.filter { it.done_at == null && it.dueDate <= horizon }.forEach { d ->
            add(UpcomingItem(d.dueDate, d.title, listOfNotNull(dev.lifeos.data.DeadlineKind.valueOf(d.kind).label, d.project_id?.let(projects::get)?.name).joinToString(" · "), UpcomingItem.Kind.DEADLINE))
        }
        milestones.groupBy { it.project_id }.forEach { (pid, ms) ->
            ms.forEachIndexed { i, m ->
                val date = LocalDate.parse(m.due_date)
                if (date in today..horizon) add(UpcomingItem(date, "Hito ${i + 1} · ${m.title}", projects[pid]?.name ?: "", UpcomingItem.Kind.MILESTONE))
            }
        }
        tasks.filter { !it.isDone && it.dueDate != null && it.dueDate!! <= horizon }.forEach { t ->
            add(UpcomingItem(t.dueDate!!, t.title, t.project_id?.let(projects::get)?.name ?: "Tarea", UpcomingItem.Kind.TASK))
        }
    }.sortedBy { it.date }.take(6)

    Card(
        "Se acerca", modifier, icon = Icons.Outlined.CalendarMonth,
        action = { IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "Nueva entrega o examen") } },
    ) {
        if (items.isEmpty()) EmptyText("Nada en los próximos 30 días.")
        items.forEach { item ->
            val days = today.daysUntil(item.date)
            val cs = MaterialTheme.colorScheme
            val color = when {
                days < 0 -> cs.error
                item.kind == UpcomingItem.Kind.DEADLINE && days <= 3 -> cs.error
                item.kind == UpcomingItem.Kind.MILESTONE -> cs.primary
                else -> cs.onSurfaceVariant
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Dates.relative(days),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.widthIn(min = 64.dp),
                )
                if (item.kind == UpcomingItem.Kind.MILESTONE) {
                    Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = cs.primary); Spacer(Modifier.width(4.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    if (item.sub.isNotBlank()) Text(item.sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AgendaCard(
    occs: List<Occurrence>,
    projects: Map<String, Project>,
    modifier: Modifier,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val now = Dates.minuteNow()
    Card(
        "Agenda", modifier, icon = Icons.Outlined.Schedule,
        action = {
            IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "Nuevo evento hoy") }
            TextButton(onClick = onOpen) { Text("Calendario") }
        },
    ) {
        if (occs.isEmpty()) EmptyText("Nada en el calendario hoy.")
        occs.forEach { o ->
            val past = o.end <= now
            val current = now in o.start until o.end
            val alpha = if (past) 0.5f else 1f
            val bar = when (o.event.kindEnum) {
                EventKind.CLASS -> cs.outline
                EventKind.STUDY -> cs.primary
                EventKind.GYM -> cs.tertiary
                EventKind.OTHER -> cs.secondary
                EventKind.FOCUS -> cs.primary
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CalendarMath.hm(o.start),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface.copy(alpha = alpha),
                    modifier = Modifier.widthIn(min = 48.dp),
                )
                androidx.compose.foundation.layout.Box(
                    Modifier.width(3.dp).height(32.dp)
                        .background(bar.copy(alpha = alpha), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = cs.onSurface.copy(alpha = alpha), maxLines = 1)
                    val project = o.event.project_id?.let(projects::get)?.name?.takeIf { it != o.event.title }
                    Text(
                        listOfNotNull("hasta las ${CalendarMath.hm(o.end)}", o.event.location, project).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant.copy(alpha = alpha), maxLines = 1,
                    )
                }
                if (current) Chip("Ahora", color = cs.primaryContainer)
            }
        }
    }
}

@Composable
private fun HabitsCard(
    habits: List<Habit>,
    counts: Map<String, Map<LocalDate, Int>>,
    today: LocalDate,
    modifier: Modifier,
    onOpen: () -> Unit,
    onLog: (Habit) -> Unit,
    onUnlog: (Habit) -> Unit,
) {
    Card(
        "Hábitos", modifier, icon = Icons.Outlined.Loop,
        action = { TextButton(onClick = onOpen) { Text("Ver todos") } },
    ) {
        if (habits.isEmpty()) EmptyText("Sin hábitos todavía.")
        habits.forEach { h ->
            val c = counts[h.id] ?: emptyMap()
            val cur = HabitMath.current(h, c, today)
            HabitQuickRow(h, cur, c[today] ?: 0, HabitMath.streak(h, c, today), onLog = { onLog(h) }, onUnlog = { onUnlog(h) })
        }
    }
}

@Composable
fun HabitQuickRow(h: Habit, current: Int, todayCount: Int, streak: Int, onLog: () -> Unit, onUnlog: () -> Unit, showName: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val target = h.target.toInt()
    val met = current >= target
    val sub = when {
        h.periodEnum == HabitPeriod.WEEK -> "$current/$target esta semana"
        target > 1 -> "$current/$target hoy" + if (streak >= 2) " · racha de $streak días" else ""
        streak >= 2 -> "Racha de $streak días"
        else -> if (met) "Hecho hoy" else "Pendiente hoy"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (showName) {
                Text(h.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = if (met) cs.tertiary else cs.onSurfaceVariant)
            } else {
                Text(sub, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (met) cs.tertiary else cs.onSurface)
            }
        }
        IconButton(onClick = onUnlog, enabled = todayCount > 0) { Icon(Icons.Outlined.Remove, "Quitar un registro de ${h.name}") }
        FilledTonalIconButton(
            onClick = onLog,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (met) cs.tertiary else cs.tertiaryContainer,
                contentColor = if (met) cs.onTertiary else cs.onTertiaryContainer,
            ),
        ) {
            if (met) Icon(Icons.Outlined.Check, "Registrar ${h.name} otra vez")
            else Icon(Icons.Outlined.Add, "Registrar ${h.name}")
        }
    }
}
