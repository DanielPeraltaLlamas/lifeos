package dev.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lifeos.data.BREAK_MS
import dev.lifeos.data.Dates
import dev.lifeos.data.FocusMath
import dev.lifeos.data.Source
import dev.lifeos.data.isDone
import dev.lifeos.db.Focus_session
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalSource
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.rememberActions
import kotlinx.coroutines.delay
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock

private fun nowMs() = Clock.System.now().toEpochMilliseconds()

@Composable
private fun rememberNow(periodMs: Long = 250): Long {
    var now by remember { mutableLongStateOf(nowMs()) }
    LaunchedEffect(periodMs) { while (true) { now = nowMs(); delay(periodMs) } }
    return now
}

/** Lo que se ve tras un bloque: la pregunta de qué hacer, o el descanso. */
private sealed interface AfterFocus {
    data class Ask(val session: Focus_session) : AfterFocus
    data class Break(val session: Focus_session, val until: Long) : AfterFocus
}

/**
 * Envuelve la app: si hay un bloque en marcha (empezado aquí o en el otro
 * dispositivo), muestra la pantalla de enfoque a pantalla completa.
 */
@Composable
fun FocusHost(content: @Composable () -> Unit) {
    val repo = LocalRepository.current
    val active by repo.activeFocus.collectAsState(null)
    var after by remember { mutableStateOf<AfterFocus?>(null) }
    var lastActive by remember { mutableStateOf<Focus_session?>(null) }

    LaunchedEffect(active?.id) {
        val prev = lastActive
        if (active == null && prev != null) after = AfterFocus.Ask(prev)
        if (active != null) after = null
        lastActive = active
    }

    val s = active
    when {
        s != null -> FocusScreen(s)
        after is AfterFocus.Break -> BreakScreen(after as AfterFocus.Break, onDone = { after = AfterFocus.Ask((after as AfterFocus.Break).session) }, onClose = { after = null })
        else -> content()
    }
    (after as? AfterFocus.Ask)?.let { a ->
        AfterBlockDialog(
            a.session,
            onBreak = { after = AfterFocus.Break(a.session, nowMs() + BREAK_MS) },
            onClose = { after = null },
        )
    }
}

@Composable
private fun FocusScreen(s: Focus_session) {
    val act = rememberActions()
    val repo = LocalRepository.current
    val source = LocalSource.current
    val now = rememberNow()
    val cs = MaterialTheme.colorScheme
    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val task = tasks.firstOrNull { it.id == s.task_id }
    val context = task?.let { taskContext(it, projects.associateBy { p -> p.id }, milestones) }
        ?: s.project_id?.let { id -> projects.firstOrNull { it.id == id }?.name }

    val remaining = FocusMath.remaining(s, now)
    val paused = FocusMath.isPaused(s, now)
    val progress = 1f - remaining.toFloat() / s.planned_ms
    var note by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) {
            val wide = maxWidth >= 700.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Modo enfoque · Bloque ${s.block_index} de ${s.block_total}",
                    style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
                Column(Modifier.widthIn(max = 720.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (context != null) {
                        Text(context, style = MaterialTheme.typography.titleSmall, color = cs.primary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        s.title,
                        style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        FocusMath.clock(remaining),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (wide) 112.sp else 76.sp, lineHeight = if (wide) 120.sp else 84.sp,
                            fontWeight = FontWeight.Light, fontFeatureSettings = "tnum",
                        ),
                        color = if (paused) cs.onSurfaceVariant else cs.onSurface,
                    )
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(0.7f).padding(top = 12.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    if (paused) {
                        Button(onClick = { act { resumeFocus(s.id) } }) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Reanudar")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "En pausa · se reanuda sola en ${FocusMath.clock(FocusMath.pauseLeft(s, now))}",
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        )
                    } else {
                        val pauseLeft = FocusMath.pauseLeft(s, now)
                        FilledTonalButton(onClick = { act { pauseFocus(s.id) } }, enabled = pauseLeft > 0) {
                            Icon(Icons.Outlined.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pausar")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (pauseLeft > 0) "Pausa disponible: ${FocusMath.clock(pauseLeft)} · el bloque no se puede terminar antes"
                            else "Sin pausa disponible en este bloque",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                    val next = if (s.block_index < s.block_total) " · luego el bloque ${s.block_index + 1}" else ""
                    Text(
                        "Termina a las ${Dates.time(FocusMath.endsAt(s, now))} · después, descanso de 10 min$next",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Apuntar sin salir
                Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    fun send() {
                        val text = note
                        if (text.isBlank()) return
                        act { capture(text, Source.FOCUS) }
                        note = ""; saved = true
                    }
                    OutlinedTextField(
                        note, { note = it; saved = false },
                        label = { Text(if (saved) "Apuntado en la bandeja" else if (wide) "Apuntar sin salir del enfoque" else "Apuntar en la bandeja") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { send() }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    if (wide) OutlinedButton(onClick = { send() }, enabled = note.isNotBlank()) { Text("A la bandeja") }
                    else FilledTonalIconButton(onClick = { send() }, enabled = note.isNotBlank()) { Icon(Icons.AutoMirrored.Outlined.Send, "A la bandeja") }
                }
                if (source == Source.ANDROID) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AfterBlockDialog(s: Focus_session, onBreak: () -> Unit, onClose: () -> Unit) {
    val act = rememberActions()
    val repo = LocalRepository.current
    val today = LocalToday.current
    val tasks by repo.tasks.collectAsState(emptyList())
    val task = tasks.firstOrNull { it.id == s.task_id }
    val todayStart = today.atStartOfDayIn(Dates.zone).toEpochMilliseconds()

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Bloque terminado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("50 minutos en «${s.title}».", style = MaterialTheme.typography.bodyMedium)
                if (task != null && !task.isDone) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { act { setTaskDone(task.id, true) }; onClose() }) { Text("La tarea está hecha") }
                }
                TextButton(onClick = onBreak) { Text("Descanso de 10 min") }
                TextButton(onClick = { act { startFocus(s.title, s.task_id, s.project_id, todayStart) }; onClose() }) {
                    Text("Otro bloque ya")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("Terminar por ahora") } },
    )
}

@Composable
private fun BreakScreen(b: AfterFocus.Break, onDone: () -> Unit, onClose: () -> Unit) {
    val now = rememberNow(500)
    val left = (b.until - now).coerceAtLeast(0)
    LaunchedEffect(left == 0L) { if (left == 0L) onDone() }
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Descanso", style = MaterialTheme.typography.titleMedium, color = cs.tertiary)
                Text(FocusMath.clock(left), style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp, lineHeight = 84.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"))
                Text("Levántate, bebe agua, mira lejos.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onClose) { Text("Salir del descanso") }
            }
        }
    }
}

/** Para los botones "Empezar": inicio del día de hoy en milisegundos. */
@Composable
fun todayStartMs(): Long = LocalToday.current.atStartOfDayIn(Dates.zone).toEpochMilliseconds()
