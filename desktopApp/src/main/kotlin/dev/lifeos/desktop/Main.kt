package dev.lifeos.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.dp
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lifeos.App
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.FocusWatcher
import dev.lifeos.data.agentRunning
import dev.lifeos.data.agentSyncNow
import dev.lifeos.data.desktopNotify
import dev.lifeos.data.watchExternalChanges
import dev.lifeos.data.backupDatabase
import dev.lifeos.data.createDriver
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.sync.createSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    val driver = createDriver()
    val repository = Repository(LifeOsDatabase(driver))
    val sync = createSyncEngine(driver, repository, backup = { backupDatabase() })
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Si el agente está en marcha, sincroniza él; la app solo muestra el estado.
    if (agentRunning()) {
        sync.onSyncNowPassive = { agentSyncNow() }
        sync.startPassive(scope)
    } else {
        sync.start(scope)
    }
    watchExternalChanges(driver, scope)
    // Si el agente está en marcha, el aviso de fin lo da él (así no sale dos veces).
    FocusWatcher(repository, onFinished = { s -> if (!agentRunning()) notifyBlockFinished(s.title) }).start(scope)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Life OS",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            DisposableEffect(window) {
                val listener = object : WindowAdapter() {
                    override fun windowGainedFocus(e: WindowEvent?) = sync.onAppVisible()
                }
                window.addWindowFocusListener(listener)
                onDispose { window.removeWindowFocusListener(listener) }
            }
            App(repository, Source.DESKTOP, sync = sync)
        }
    }
}

/** Notificación del escritorio (la muestra el demonio de Caelestia) y un pitido. */
private fun notifyBlockFinished(title: String) {
    desktopNotify("Bloque terminado", "50 minutos en «$title». Toca descanso.", critical = true)
    runCatching { java.awt.Toolkit.getDefaultToolkit().beep() }
}
