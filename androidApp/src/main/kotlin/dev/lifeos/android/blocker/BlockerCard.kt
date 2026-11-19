package dev.lifeos.android.blocker

import android.content.Intent
import androidx.core.net.toUri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.lifeos.data.BlockRules
import dev.lifeos.ui.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Tarjeta de Hoy en el móvil: permisos del bloqueo y minutos de scroll de hoy. */
@Composable
fun BlockerCard() {
    val context = LocalContext.current
    var accessibility by remember { mutableStateOf(Usage.hasAccessibility(context)) }
    var usage by remember { mutableStateOf(Usage.hasUsageAccess(context)) }
    var used by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) }

    // Al volver de Ajustes, se revisan los permisos.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessibility = Usage.hasAccessibility(context)
        usage = Usage.hasUsageAccess(context)
        tick++
    }
    LaunchedEffect(usage, tick) {
        while (true) {
            used = withContext(Dispatchers.IO) { Usage.limitedTodayMs(context) }
            delay(60_000)
        }
    }

    Card("Bloqueo", Modifier.fillMaxWidth(), icon = Icons.Outlined.Block) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!accessibility || !usage) {
                Text(
                    "Para bloquear TikTok, Instagram y Discord faltan permisos:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!accessibility) {
                    FilledTonalButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text("1 · Activar accesibilidad") }
                    Text(
                        "Si sale «Ajuste restringido»: Ajustes → Apps → Life OS → ⋮ → Permitir ajustes restringidos, y vuelve a intentarlo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!usage) {
                    FilledTonalButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:${context.packageName}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) { Text("2 · Permitir acceso al uso") }
                }
            } else {
                val limit = dev.lifeos.ui.LocalPrefs.current.scrollLimitMs
                val usedMin = used / 60_000
                Text(
                    if (used >= limit) "TikTok e Instagram: límite de hoy agotado"
                    else "TikTok e Instagram: $usedMin de ${limit / 60_000} min hoy",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { (used.toFloat() / limit).coerceIn(0f, 1f) },
                    color = if (used >= limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                Text(
                    "Durante los bloques de enfoque también se bloquean del todo, junto con Discord.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
