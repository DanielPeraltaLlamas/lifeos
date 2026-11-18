package dev.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.lifeos.data.NoteKind
import dev.lifeos.data.Source
import dev.lifeos.db.Inbox_item
import dev.lifeos.db.Project
import dev.lifeos.ui.Card
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalSource
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.ScreenHeader
import dev.lifeos.ui.rememberActions

@Composable
fun InboxScreen() {
    val repo = LocalRepository.current
    val source = LocalSource.current
    val act = rememberActions()
    val items by repo.inbox.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    var draft by remember { mutableStateOf("") }

    fun submit() {
        val t = draft
        if (t.isBlank()) return
        draft = ""
        act { capture(t, source) }
    }

    Column(Modifier.widthIn(max = 820.dp)) {
        ScreenHeader("Bandeja", "Apunta en 2 segundos y sigue con lo tuyo. Se vacía en la revisión del domingo.")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Apuntar algo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = ::submit, enabled = draft.isNotBlank()) { Text("Apuntar") }
        }
        Spacer(Modifier.height(18.dp))
        if (items.isEmpty()) {
            Text(
                "Bandeja vacía. Tu cabeza, también.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { InboxRow(it, projects) }
        }
    }
}

@Composable
private fun InboxRow(item: Inbox_item, projects: List<Project>) {
    val act = rememberActions()
    val today = LocalToday.current
    val source = runCatching { Source.valueOf(item.source) }.getOrNull()
    Card(null, Modifier.fillMaxWidth(), padding = 16.dp) {
        Text(item.text, style = MaterialTheme.typography.bodyLarge)
        Text(
            dev.lifeos.data.Dates.stamp(item.created_at, today) + (source?.let { " · desde ${it.label}" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProjectMenuButton("A tarea", projects, noneLabel = "Tarea suelta") { pid -> act { inboxToTask(item, pid) } }
            ProjectMenuButton("A notas", projects, noneLabel = "Notas generales", kinds = true) { pid, kind ->
                act { inboxToNote(item, pid, kind) }
            }
            TextButton(onClick = { act { discardInbox(item.id) } }) { Text("Descartar") }
        }
    }
}

@Composable
private fun ProjectMenuButton(label: String, projects: List<Project>, noneLabel: String, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(noneLabel) }, onClick = { open = false; onPick(null) })
            projects.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { open = false; onPick(p.id) }) }
        }
    }
}

/** Variante para notas: primero el proyecto y después el tipo (idea, nota o enlace). */
@Composable
private fun ProjectMenuButton(
    label: String,
    projects: List<Project>,
    noneLabel: String,
    @Suppress("UNUSED_PARAMETER") kinds: Boolean,
    onPick: (String?, NoteKind) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var project by remember { mutableStateOf<Pair<String?, String>?>(null) }
    Box {
        OutlinedButton(onClick = { project = null; open = true }) { Text(label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val chosen = project
            if (chosen == null) {
                DropdownMenuItem(text = { Text(noneLabel) }, onClick = { project = null to noneLabel })
                projects.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { project = p.id to p.name }) }
            } else {
                Text(
                    "${chosen.second}: guardar como…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                HorizontalDivider()
                NoteKind.entries.forEach { k ->
                    DropdownMenuItem(text = { Text(k.label) }, onClick = { open = false; onPick(chosen.first, k) })
                }
            }
        }
    }
}
