package dev.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults

import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.dueDate
import dev.lifeos.data.isDone
import dev.lifeos.data.kindEnum
import dev.lifeos.db.Deadline
import dev.lifeos.db.Milestone
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import dev.lifeos.ui.Card
import dev.lifeos.ui.ConfirmDialog
import dev.lifeos.ui.DeadlineDialog
import dev.lifeos.ui.EmptyText
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.MenuAction
import dev.lifeos.ui.MilestoneDialog
import dev.lifeos.ui.OverflowMenu
import dev.lifeos.ui.ProjectDialog
import dev.lifeos.ui.SectionLabel
import dev.lifeos.ui.TaskDialog
import dev.lifeos.ui.TaskRow
import dev.lifeos.ui.rememberActions
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Selección de la pantalla: id de proyecto o [LOOSE] para las tareas sueltas. */
const val LOOSE = "__loose__"

private sealed interface TasksDialog {
    data class EditTask(val task: Task?, val projectId: String?, val milestoneId: String?) : TasksDialog
    data class EditProject(val project: Project?, val kind: ProjectKind) : TasksDialog
    data class EditMilestone(val projectId: String, val milestone: Milestone?) : TasksDialog
    data class NewDeadline(val projectId: String?) : TasksDialog
    data class Confirm(val title: String, val text: String, val action: () -> Unit) : TasksDialog
}

@Composable
fun TasksScreen(wide: Boolean, selected: String?, onSelect: (String?) -> Unit) {
    val repo = LocalRepository.current
    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val deadlines by repo.deadlines.collectAsState(emptyList())
    var dialog by remember { mutableStateOf<TasksDialog?>(null) }

    // En pantallas anchas siempre hay algo seleccionado (el TFG por defecto).
    val effective = selected?.takeIf { it == LOOSE || projects.any { p -> p.id == it } }
        ?: if (wide) projects.firstOrNull { it.kindEnum == ProjectKind.TFG }?.id ?: LOOSE else null

    val list: @Composable (Modifier) -> Unit = { m ->
        ProjectList(projects, tasks, effective, m, onSelect,
            onNew = { kind -> dialog = TasksDialog.EditProject(null, kind) })
    }
    val detail: @Composable (Modifier) -> Unit = { m ->
        val project = projects.firstOrNull { it.id == effective }
        ProjectDetail(
            project = project,
            tasks = tasks.filter { if (project == null) it.project_id == null else it.project_id == project.id },
            milestones = milestones.filter { it.project_id == project?.id },
            deadlines = deadlines.filter { project != null && it.project_id == project.id },
            projects = projects,
            allMilestones = milestones,
            modifier = m,
            onBack = if (wide) null else ({ onSelect(null) }),
            openDialog = { dialog = it },
        )
    }

    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            list(Modifier.width(260.dp))
            detail(Modifier.weight(1f))
        }
    } else {
        if (effective == null) list(Modifier.fillMaxWidth()) else detail(Modifier.fillMaxWidth())
    }

    when (val d = dialog) {
        is TasksDialog.EditTask -> TaskDialog(d.task, projects, milestones, d.projectId, d.milestoneId) { dialog = null }
        is TasksDialog.EditProject -> ProjectDialog(d.project, d.kind, onDismiss = { dialog = null }, onCreated = { onSelect(it) })
        is TasksDialog.EditMilestone -> MilestoneDialog(d.projectId, d.milestone) { dialog = null }
        is TasksDialog.NewDeadline -> DeadlineDialog(projects, d.projectId) { dialog = null }
        is TasksDialog.Confirm -> ConfirmDialog(d.title, d.text, "Eliminar", d.action) { dialog = null }
        null -> {}
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    tasks: List<Task>,
    selected: String?,
    modifier: Modifier,
    onSelect: (String?) -> Unit,
    onNew: (ProjectKind) -> Unit,
) {
    val open = tasks.filter { !it.isDone }.groupingBy { it.project_id ?: LOOSE }.eachCount()

    @Composable
    fun item(id: String, label: String) {
        NavigationDrawerItem(
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = id == selected,
            onClick = { onSelect(id) },
            badge = open[id]?.let { n -> { Text("$n", style = MaterialTheme.typography.labelMedium) } },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.height(44.dp),
        )
    }

    @Composable
    fun addButton(label: String, kind: ProjectKind) {
        TextButton(onClick = { onNew(kind) }) {
            Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(label)
        }
    }

    Card(null, modifier) {
        val byKind = projects.groupBy { it.kindEnum }
        byKind[ProjectKind.TFG]?.let { tfg ->
            SectionLabel("Proyectos")
            tfg.forEach { item(it.id, it.name) }
            Spacer(Modifier.height(12.dp))
        }
        SectionLabel("Personales")
        byKind[ProjectKind.PERSONAL].orEmpty().forEach { item(it.id, it.name) }
        addButton("Nuevo proyecto personal", ProjectKind.PERSONAL)
        Spacer(Modifier.height(12.dp))
        SectionLabel("Asignaturas")
        byKind[ProjectKind.SUBJECT].orEmpty().forEach { item(it.id, it.name) }
        addButton("Nueva asignatura", ProjectKind.SUBJECT)
        Spacer(Modifier.height(12.dp))
        SectionLabel("Otros")
        item(LOOSE, "Tareas sueltas")
    }
}

@Composable
private fun ProjectDetail(
    project: Project?,
    tasks: List<Task>,
    milestones: List<Milestone>,
    deadlines: List<Deadline>,
    projects: List<Project>,
    allMilestones: List<Milestone>,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    openDialog: (TasksDialog) -> Unit,
) {
    val act = rememberActions()
    val today = LocalToday.current
    val currentMilestone = milestones.firstOrNull { m ->
        val mt = tasks.filter { it.milestone_id == m.id }
        LocalDate.parse(m.due_date) >= today && (mt.isEmpty() || mt.any { !it.isDone })
    }
    var selectedMilestoneId by remember(project?.id) { mutableStateOf<String?>(null) }
    val selectedMilestone = milestones.firstOrNull { it.id == selectedMilestoneId } ?: currentMilestone

    val todayStart = todayStartMs()
    val blockMin = dev.lifeos.ui.LocalPrefs.current.blockMin
    fun taskMenu(t: Task) = listOfNotNull(
        if (!t.isDone) MenuAction("Empezar bloque · $blockMin min") { act { startFocus(t.title, t.id, t.project_id, todayStart) } } else null,
        MenuAction("Editar") { openDialog(TasksDialog.EditTask(t, t.project_id, t.milestone_id)) },
        if (t.planned_date == today.toString()) MenuAction("Quitar de hoy") { act { setTaskPlanned(t.id, null) } }
        else MenuAction("Añadir a hoy") { act { setTaskPlanned(t.id, today) } },
        MenuAction("Eliminar", destructive = true) {
            openDialog(TasksDialog.Confirm("Eliminar tarea", "«${t.title}» se eliminará.") { act { deleteTask(t.id) } })
        },
    )

    @Composable
    fun taskList(list: List<Task>) {
        list.sortedBy { it.isDone }.forEach { t ->
            TaskRow(
                title = t.title,
                meta = taskMeta(t, today),
                done = t.isDone,
                onToggle = { d -> act { setTaskDone(t.id, d) } },
                badge = if (t.planned_date == today.toString() && !t.isDone) "Hoy" else null,
                menu = taskMenu(t),
                onClick = { openDialog(TasksDialog.EditTask(t, t.project_id, t.milestone_id)) },
            )
        }
    }

    Column(modifier) {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        project == null -> "Tareas sueltas"
                        project.description != null -> "${project.name} · ${project.description}"
                        else -> project.name
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                val due = project?.dueDate
                val sub = when {
                    project == null -> "Lo que no pertenece a ningún proyecto."
                    due != null -> {
                        val n = today.daysUntil(due)
                        "Entrega el ${Dates.medium(due)} · " + if (n >= 0) "faltan $n días" else "pasó hace ${-n} días"
                    }
                    else -> null
                }
                sub?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Button(onClick = { openDialog(TasksDialog.EditTask(null, project?.id, selectedMilestone?.id)) }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nueva tarea")
            }
            if (project != null) {
                OverflowMenu(
                    listOf(
                        MenuAction("Editar proyecto") { openDialog(TasksDialog.EditProject(project, project.kindEnum)) },
                        MenuAction("Nuevo hito") { openDialog(TasksDialog.EditMilestone(project.id, null)) },
                        MenuAction("Nueva entrega o examen") { openDialog(TasksDialog.NewDeadline(project.id)) },
                        MenuAction("Eliminar proyecto", destructive = true) {
                            openDialog(TasksDialog.Confirm("Eliminar ${project.name}", "Se eliminarán también sus hitos, tareas y entregas.") {
                                act { deleteProject(project.id) }
                            })
                        },
                    ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Hitos
            if (project != null && (milestones.isNotEmpty() || project.kindEnum != ProjectKind.SUBJECT)) {
                Card(
                    "Hitos", Modifier.fillMaxWidth(), icon = Icons.Outlined.Flag,
                    action = { TextButton(onClick = { openDialog(TasksDialog.EditMilestone(project.id, null)) }) { Text("Nuevo hito") } },
                ) {
                    if (milestones.isEmpty()) EmptyText("Divide el proyecto en hitos con fecha y entregable.")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        milestones.forEachIndexed { i, m ->
                            val mt = tasks.filter { it.milestone_id == m.id }
                            MilestoneTile(
                                index = i + 1,
                                milestone = m,
                                done = mt.count { it.isDone },
                                total = mt.size,
                                state = when {
                                    m.id == currentMilestone?.id -> MilestoneState.NOW
                                    LocalDate.parse(m.due_date) < today || (mt.isNotEmpty() && mt.all { it.isDone }) -> MilestoneState.DONE
                                    else -> MilestoneState.LATER
                                },
                                selected = m.id == selectedMilestone?.id,
                                onClick = { selectedMilestoneId = m.id },
                            )
                        }
                    }
                }
            }

            // Tareas del hito seleccionado
            if (project != null && selectedMilestone != null) {
                val idx = milestones.indexOf(selectedMilestone) + 1
                val mt = tasks.filter { it.milestone_id == selectedMilestone.id }
                Card(
                    "H$idx · ${selectedMilestone.title}", Modifier.fillMaxWidth(),
                    action = {
                        Text("${mt.count { it.isDone }} de ${mt.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OverflowMenu(
                            listOf(
                                MenuAction("Editar hito") { openDialog(TasksDialog.EditMilestone(project.id, selectedMilestone)) },
                                MenuAction("Eliminar hito", destructive = true) {
                                    openDialog(TasksDialog.Confirm("Eliminar hito", "Sus tareas se quedan en el proyecto, sin hito.") {
                                        act { deleteMilestone(selectedMilestone.id) }
                                    })
                                },
                            ),
                        )
                    },
                ) {
                    val info = listOfNotNull(
                        "Fecha: ${Dates.medium(LocalDate.parse(selectedMilestone.due_date))}",
                        selectedMilestone.deliverable?.let { "entregable: $it" },
                    ).joinToString(" · ")
                    Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                    if (mt.isEmpty()) EmptyText("Sin tareas. Divide el hito en pasos de 1–2 bloques.")
                    taskList(mt)
                    TextButton(onClick = { openDialog(TasksDialog.EditTask(null, project.id, selectedMilestone.id)) }) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Tarea en este hito")
                    }
                }
            }

            // Tareas sin hito
            val loose = tasks.filter { it.milestone_id == null || milestones.none { m -> m.id == it.milestone_id } }
            if (loose.isNotEmpty() || milestones.isEmpty()) {
                Card(if (milestones.isEmpty()) "Tareas" else "Sin hito", Modifier.fillMaxWidth()) {
                    if (loose.isEmpty()) EmptyText(if (project == null) "No hay tareas sueltas." else "Todavía no hay tareas.")
                    taskList(loose)
                }
            }

            // Entregas y exámenes
            if (project != null && (deadlines.isNotEmpty() || project.kindEnum == ProjectKind.SUBJECT)) {
                Card(
                    "Entregas y exámenes", Modifier.fillMaxWidth(),
                    action = { TextButton(onClick = { openDialog(TasksDialog.NewDeadline(project.id)) }) { Text("Añadir") } },
                ) {
                    if (deadlines.isEmpty()) EmptyText("Ninguna registrada.")
                    deadlines.forEach { d -> DeadlineRow(d, today, openDialog) }
                }
            }
        }
    }
}

private fun taskMeta(t: Task, today: LocalDate): String? {
    val parts = mutableListOf<String>()
    t.dueDate?.let { parts += if (it < today && !t.isDone) "Vencida (${Dates.short(it)})" else "Antes del ${Dates.short(it)}" }
    estimateText(t)?.let { parts += it }
    return parts.joinToString(" · ").ifEmpty { null }
}

private enum class MilestoneState { DONE, NOW, LATER }

@Composable
private fun MilestoneTile(index: Int, milestone: Milestone, done: Int, total: Int, state: MilestoneState, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bar = when (state) {
        MilestoneState.DONE -> cs.tertiary
        MilestoneState.NOW -> cs.primary
        MilestoneState.LATER -> cs.outlineVariant
    }
    Surface(
        color = if (selected) cs.surfaceContainerHighest else cs.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        contentColor = cs.onSurface,
        modifier = Modifier.width(176.dp).clickable(onClick = onClick),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(bar))
            Column(Modifier.padding(12.dp)) {
                Text(
                    "H$index" + when (state) { MilestoneState.NOW -> " · EN CURSO"; MilestoneState.DONE -> " · HECHO"; else -> "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state == MilestoneState.NOW) cs.primary else cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(milestone.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    Dates.short(LocalDate.parse(milestone.due_date)) + if (total > 0) " · $done/$total" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DeadlineRow(d: Deadline, today: LocalDate, openDialog: (TasksDialog) -> Unit) {
    val act = rememberActions()
    val days = today.daysUntil(LocalDate.parse(d.due_date))
    val done = d.done_at != null
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(done, { act { setDeadlineDone(d.id, it) } })
        Column(Modifier.weight(1f)) {
            Text(d.title, style = MaterialTheme.typography.bodyMedium, textDecoration = if (done) TextDecoration.LineThrough else null)
            Text(
                "${dev.lifeos.data.DeadlineKind.valueOf(d.kind).label} · ${Dates.medium(LocalDate.parse(d.due_date))}" + if (!done) " · ${Dates.relative(days)}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (!done && days <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OverflowMenu(listOf(MenuAction("Eliminar", destructive = true) {
            openDialog(TasksDialog.Confirm("Eliminar", "«${d.title}» se eliminará.") { act { deleteDeadline(d.id) } })
        }))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

