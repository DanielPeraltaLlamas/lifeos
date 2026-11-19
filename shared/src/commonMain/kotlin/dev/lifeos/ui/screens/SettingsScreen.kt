package dev.lifeos.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Prefs
import dev.lifeos.ui.Card
import dev.lifeos.ui.LocalPrefs
import dev.lifeos.ui.LocalSync
import dev.lifeos.ui.Picker
import dev.lifeos.ui.ScreenHeader
import dev.lifeos.ui.SyncDialog
import dev.lifeos.ui.rememberActions

private val DAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

@Composable
fun SettingsScreen(wide: Boolean) {
    val prefs = LocalPrefs.current
    val act = rememberActions()
    fun put(key: String, value: String) = act { putPref(key, value) }

    Column {
        ScreenHeader("Ajustes", "Se aplican a la vez en el móvil, el ordenador y el navegador.")
        val gap = 14.dp

        val blocking: @Composable (Modifier) -> Unit = { m ->
            Card("Bloqueo de distracciones", m, icon = Icons.Outlined.Block) {
                Picker(
                    "Límite diario de TikTok + Instagram (móvil)",
                    (1..12).map { it * 15 }.map { it to minutesText(it) + " al día" },
                    prefs.scrollLimitMin,
                ) { put(Prefs.SCROLL_LIMIT, it.toString()) }
                Spacer(Modifier.height(14.dp))
                Text("Durante los bloques de enfoque", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Móvil: TikTok, Instagram y Discord. Ordenador: se cierran Vesktop, Steam y Lutris. Navegador: estas webs.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SiteList(prefs.webFocus) { put(Prefs.WEB_FOCUS, it.joinToString(",")) }
                Spacer(Modifier.height(14.dp))
                Text("Siempre bloqueadas en el ordenador", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Para que no sean la forma de saltarse el límite del móvil.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SiteList(prefs.webAlways) { put(Prefs.WEB_ALWAYS, it.joinToString(",")) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Un bloque no se puede terminar antes de tiempo; solo pausar (10 min como mucho).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val focus: @Composable (Modifier) -> Unit = { m ->
            Card("Enfoque", m, icon = Icons.Outlined.Timer) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Picker("Duración del bloque", listOf(25, 30, 40, 45, 50, 60, 75, 90).map { it to "$it min" }, prefs.blockMin) { put(Prefs.BLOCK, it.toString()) }
                    Picker("Descanso", listOf(5, 10, 15, 20).map { it to "$it min" }, prefs.breakMin) { put(Prefs.BREAK, it.toString()) }
                }
                Text(
                    "Se aplica a los bloques que empieces a partir de ahora.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        val rituals: @Composable (Modifier) -> Unit = { m ->
            Card("Rituales", m, icon = Icons.Outlined.EventRepeat) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Picker("Proponer el cierre desde", (17..23).map { it to "las $it:00" }, prefs.closeHour) { put(Prefs.CLOSE_HOUR, it.toString()) }
                    Picker("Revisión semanal", (1..7).map { it to DAYS[it - 1] }, prefs.reviewDay) { put(Prefs.REVIEW_DAY, it.toString()) }
                }
            }
        }
        val account: @Composable (Modifier) -> Unit = { m ->
            val sync = LocalSync.current
            var open by remember { mutableStateOf(false) }
            Card("Cuenta y sincronización", m, icon = Icons.Outlined.Cloud) {
                if (sync == null) {
                    Text("Esta versión se compiló sin sincronización.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val state by sync.state.collectAsState()
                    Text(state.email?.let { "Conectado como $it" } ?: "Sin cuenta: los datos solo están en este dispositivo.")
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { open = true }) { Text(if (state.email == null) "Entrar" else "Ver estado") }
                    if (open) SyncDialog(sync) { open = false }
                }
            }
        }

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                blocking(Modifier.weight(1.2f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                    focus(Modifier.fillMaxWidth()); rituals(Modifier.fillMaxWidth()); account(Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                blocking(Modifier.fillMaxWidth()); focus(Modifier.fillMaxWidth()); rituals(Modifier.fillMaxWidth()); account(Modifier.fillMaxWidth())
            }
        }
    }
}

private fun minutesText(min: Int) = if (min >= 60) "${min / 60} h" + (if (min % 60 > 0) " ${min % 60} min" else "") else "$min min"

@Composable
private fun SiteList(sites: List<String>, onChange: (List<String>) -> Unit) {
    var adding by remember { mutableStateOf("") }
    fun add() {
        val host = adding.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
        if (host.contains('.') && host !in sites) onChange(sites + host)
        adding = ""
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
        sites.forEach { s ->
            InputChip(
                selected = false,
                onClick = { onChange(sites - s) },
                label = { Text(s) },
                trailingIcon = { Icon(Icons.Outlined.Close, "Quitar $s", Modifier.size(16.dp)) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            adding, { adding = it }, placeholder = { Text("Añadir web (p. ej. x.com)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = { add() }, enabled = adding.isNotBlank()) { Text("Añadir") }
    }
}
