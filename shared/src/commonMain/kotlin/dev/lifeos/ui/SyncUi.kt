package dev.lifeos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.lifeos.sync.SyncEngine
import dev.lifeos.sync.SyncState
import dev.lifeos.sync.SyncState.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

val LocalSync = compositionLocalOf<SyncEngine?> { null }

private fun icon(s: SyncState): ImageVector = when (s.status) {
    Status.SIGNED_OUT -> Icons.Outlined.Cloud
    Status.SYNCING -> Icons.Outlined.CloudSync
    Status.IDLE -> Icons.Outlined.CloudDone
    Status.OFFLINE -> Icons.Outlined.CloudOff
    Status.ERROR -> Icons.Outlined.ErrorOutline
}

/** "Sincronizado hace 2 min" y similares. */
@Composable
private fun statusText(s: SyncState): String {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = Clock.System.now().toEpochMilliseconds() } }
    return when (s.status) {
        Status.SIGNED_OUT -> "Sin sincronizar"
        Status.SYNCING -> "Sincronizando…"
        Status.OFFLINE -> "Sin conexión"
        Status.ERROR -> "Error al sincronizar"
        Status.IDLE -> s.lastSync?.let {
            val min = ((now - it) / 60_000).coerceAtLeast(0)
            when {
                min < 1 -> "Sincronizado ahora"
                min < 60 -> "Sincronizado hace $min min"
                else -> "Sincronizado hace ${min / 60} h"
            }
        } ?: "Conectado"
    }
}

/** Línea de estado para el pie del menú lateral (escritorio). */
@Composable
fun SyncStatusRow(modifier: Modifier = Modifier) {
    val sync = LocalSync.current ?: return
    val state by sync.state.collectAsState()
    if (!state.configured) return
    var open by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon(state), null, Modifier.size(18.dp), tint = if (state.status == Status.ERROR) cs.error else cs.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(statusText(state), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1)
    }
    if (open) SyncDialog(sync) { open = false }
}

/** Botón de icono para la cabecera (móvil). */
@Composable
fun SyncIconButton() {
    val sync = LocalSync.current ?: return
    val state by sync.state.collectAsState()
    if (!state.configured) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(
            icon(state), statusText(state),
            tint = if (state.status == Status.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) SyncDialog(sync) { open = false }
}

@Composable
fun SyncDialog(sync: SyncEngine, onDismiss: () -> Unit) {
    val state by sync.state.collectAsState()
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(state.email ?: "") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val cs = MaterialTheme.colorScheme

    if (state.status == Status.SIGNED_OUT) {
        fun run(block: suspend () -> String?) {
            busy = true; message = null
            scope.launch {
                message = block()
                busy = false
            }
        }
        val canSend = !busy && email.contains('@') && password.length >= 6
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sincronizar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Entra con tu cuenta para tener los mismos datos en el ordenador y en el móvil.",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    )
                    state.message?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(
                        email, { email = it.trim() }, label = { Text("Correo") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        password, { password = it }, label = { Text("Contraseña") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                    )
                    message?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { run { sync.signUp(email, password) } }, enabled = canSend) { Text("Crear cuenta nueva") }
                }
            },
            confirmButton = {
                TextButton(onClick = { run { sync.signIn(email, password) } }, enabled = canSend) { Text(if (busy) "Entrando…" else "Entrar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(icon(state), null) },
            title = { Text(statusText(state)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cuenta: ${state.email ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Los cambios se guardan aquí al momento y se envían solos al otro dispositivo cuando hay conexión.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                    )
                    state.message?.let { Text(it, color = if (state.status == Status.ERROR) cs.error else cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { scope.launch { sync.signOut() } }) { Text("Cerrar sesión", color = cs.error) }
                }
            },
            confirmButton = { TextButton(onClick = { sync.syncNow() }, enabled = state.status != Status.SYNCING) { Text("Sincronizar ahora") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        )
    }
}
