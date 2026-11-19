package dev.lifeos.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lifeos.App
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
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
    sync.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Life OS",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            App(repository, Source.DESKTOP, sync = sync)
        }
    }
}
