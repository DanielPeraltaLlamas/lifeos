package dev.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.NoteKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.kindEnum
import dev.lifeos.db.Note
import dev.lifeos.db.Project
import dev.lifeos.ui.Chip
import dev.lifeos.ui.ConfirmDialog
import dev.lifeos.ui.EmptyText
import dev.lifeos.ui.FormDialog
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.Picker
import dev.lifeos.ui.ProjectDialog
import dev.lifeos.ui.ScreenHeader
import dev.lifeos.ui.SectionLabel
import dev.lifeos.ui.projectLabel
import dev.lifeos.ui.rememberActions

private const val NO_PROJECT = "__none__"

val Note.kindEnum: NoteKind get() = runCatching { NoteKind.valueOf(kind) }.getOrDefault(NoteKind.NOTE)

/** Notas por carpeta: una por proyecto o asignatura, más «Sin proyecto». */
@Composable
fun NotesScreen(wide: Boolean) {
    val repo = LocalRepository.current
    val notes by repo.notes.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Pair<Note?, String?>?>(null) }
    var newProject by remember { mutableStateOf(false) }

    val uni = projects.filter { it.kindEnum == ProjectKind.TFG || it.kindEnum == ProjectKind.SUBJECT }
    val personal = projects.filter { it.kindEnum == ProjectKind.PERSONAL }
    val loose = notes.filter { it.project_id == null || projects.none { p -> p.id == it.project_id } }
    val current = selected ?: uni.firstOrNull()?.id ?: if (loose.isNotEmpty()) NO_PROJECT else personal.firstOrNull()?.id
    val currentProject = projects.firstOrNull { it.id == current }
    val shown = if (current == NO_PROJECT) loose else notes.filter { it.project_id == current }
    fun count(id: String) = notes.count { it.project_id == id }

    Column {
        ScreenHeader("Notas", "Ideas, notas y enlaces de cada proyecto") {
            Button(onClick = { editing = null to current?.takeIf { it != NO_PROJECT } }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nueva nota")
            }
        }

        val folders: @Composable () -> Unit = {
            SectionLabel("Universidad")
            uni.forEach { Folder(it.name, count(it.id), current == it.id) { selected = it.id } }
            Spacer(Modifier.height(12.dp))
            SectionLabel("Proyectos personales")
            if (personal.isEmpty()) {
                Text(
                    "Aún no tienes proyectos personales. Cuando empieces uno, créalo aquí y podrás mandarle ideas desde la Bandeja.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            personal.forEach { Folder(it.name, count(it.id), current == it.id) { selected = it.id } }
            TextButton(onClick = { newProject = true }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Nuevo proyecto") }
            if (loose.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Folder("Sin proyecto", loose.size, current == NO_PROJECT) { selected = NO_PROJECT }
            }
        }

        val list: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                val title = currentProject?.name ?: if (current == NO_PROJECT) "Sin proyecto" else "—"
                Text(title, style = MaterialTheme.typography.titleLarge)
                currentProject?.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(12.dp))
                if (shown.isEmpty()) EmptyText("No hay notas aquí todavía. Desde la Bandeja puedes mandar cualquier cosa a esta carpeta.")
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 640.dp) 2 else 1
                    val gap = 12.dp
                    val cardWidth = (maxWidth - gap * (columns - 1)) / columns
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                        shown.forEach { n -> NoteCard(n, Modifier.width(cardWidth)) { editing = n to n.project_id } }
                    }
                }
            }
        }

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(240.dp)) { folders() }
                list(Modifier.weight(1f))
            }
        } else {
            // En el móvil, las carpetas como fichas arriba.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (uni + personal).forEach { p ->
                    FilterChip(selected = current == p.id, onClick = { selected = p.id }, label = { Text("${p.name} · ${count(p.id)}") })
                }
                if (loose.isNotEmpty()) FilterChip(selected = current == NO_PROJECT, onClick = { selected = NO_PROJECT }, label = { Text("Sin proyecto · ${loose.size}") })
                FilterChip(selected = false, onClick = { newProject = true }, label = { Text("+ Proyecto") })
            }
            Spacer(Modifier.height(16.dp))
            list(Modifier.fillMaxWidth())
        }
    }

    editing?.let { (note, projectId) -> NoteDialog(note, projectId, projects) { editing = null } }
    if (newProject) ProjectDialog(null, ProjectKind.PERSONAL, onDismiss = { newProject = false }, onCreated = { selected = it })
}

@Composable
private fun Folder(name: String, n: Int, on: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (on) cs.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Folder, null, Modifier.size(18.dp), tint = if (on) cs.primary else cs.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(name, modifier = Modifier.weight(1f), fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) cs.onSurface else cs.onSurfaceVariant)
        Text("$n", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun NoteCard(n: Note, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val today = LocalToday.current
    val uri = LocalUriHandler.current
    Surface(color = cs.surfaceContainer, shape = MaterialTheme.shapes.large, onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(n.kindEnum.label, color = when (n.kindEnum) {
                    NoteKind.IDEA -> cs.tertiaryContainer
                    NoteKind.LINK -> cs.secondaryContainer
                    NoteKind.NOTE -> cs.surfaceContainerHighest
                })
                Spacer(Modifier.weight(1f))
                Text(Dates.stamp(n.created_at, today), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            val title = n.title ?: n.body.lineSequence().first()
            val body = if (n.title != null) n.body else n.body.lineSequence().drop(1).joinToString("\n")
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            n.url?.let { url ->
                TextButton(onClick = { runCatching { uri.openUri(url) } }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(url.removePrefix("https://").removePrefix("http://").take(48), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(note: Note?, initialProject: String?, projects: List<Project>, onDismiss: () -> Unit) {
    val act = rememberActions()
    var kind by remember { mutableStateOf(note?.kindEnum ?: NoteKind.NOTE) }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var url by remember { mutableStateOf(note?.url ?: "") }
    var projectId by remember { mutableStateOf(note?.project_id ?: initialProject) }
    var deleting by remember { mutableStateOf(false) }
    val valid = title.isNotBlank() || body.isNotBlank()

    fun save() {
        if (!valid) return
        val b = body.ifBlank { title }
        val t = title.takeIf { body.isNotBlank() }
        val u = url.takeIf { kind == NoteKind.LINK }
        if (note == null) act { addNote(projectId, kind, t, b, u) } else act { updateNote(note.id, projectId, kind, t, b, u) }
        onDismiss()
    }

    FormDialog(if (note == null) "Nueva nota" else "Editar nota", canConfirm = valid, onConfirm = ::save, onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NoteKind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) }) }
        }
        OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text("Texto") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        if (kind == NoteKind.LINK) OutlinedTextField(url, { url = it.trim() }, label = { Text("Enlace (https://…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Picker(
            "Carpeta",
            listOf<Pair<String?, String>>(null to "Sin proyecto") + projects.map { it.id to projectLabel(it) },
            projectId,
        ) { projectId = it }
        if (note != null) TextButton(onClick = { deleting = true }) { Text("Borrar nota", color = MaterialTheme.colorScheme.error) }
    }
    if (deleting && note != null) {
        ConfirmDialog("¿Borrar la nota?", "Se borra en todos tus dispositivos.", "Borrar",
            onConfirm = { act { deleteNote(note.id) }; onDismiss() }, onDismiss = { deleting = false })
    }
}
