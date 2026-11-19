package dev.lifeos.agent

import dev.lifeos.data.FocusMath
import dev.lifeos.data.FocusWatcher
import dev.lifeos.data.Repository
import dev.lifeos.data.backupDatabase
import dev.lifeos.data.createDriver
import dev.lifeos.data.desktopNotify
import dev.lifeos.data.watchExternalChanges
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.sync.createSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * Agente de Life OS para Linux (servicio de usuario de systemd):
 * - sincroniza aunque la app esté cerrada (usa la misma base y la misma sesión);
 * - durante un bloque de enfoque cierra Vesktop/Discord, Steam y Lutris;
 * - avisa al terminar el bloque;
 * - responde en 127.0.0.1 a la extensión del navegador.
 */
fun main(): Unit = runBlocking {
    val driver = createDriver()
    val repo = Repository(LifeOsDatabase(driver))
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    watchExternalChanges(driver, scope)
    createSyncEngine(driver, repo, backup = { backupDatabase() }).start(scope, focusPollMs = 30_000)
    FocusWatcher(repo, onFinished = { s ->
        desktopNotify("Bloque terminado", "50 minutos en «${s.title}». Toca descanso.", critical = true)
    }).start(scope)

    val focus = repo.activeFocus.stateIn(scope, SharingStarted.Eagerly, null)
    Enforcer { focus.value?.let { s -> FocusMath.endsAt(s, System.currentTimeMillis()).takeIf { FocusMath.remaining(s, System.currentTimeMillis()) > 0 } } }
        .start(scope)
    val prefs = repo.prefs.stateIn(scope, SharingStarted.Eagerly, dev.lifeos.data.Prefs())
    StatusServer({ focus.value }, { prefs.value }).start()

    println("Life OS agent en marcha")
    awaitCancellation()
}
