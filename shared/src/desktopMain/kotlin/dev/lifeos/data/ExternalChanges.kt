package dev.lifeos.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import dev.lifeos.sync.SYNC_TABLES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * En el PC, la app y el agente comparten la base de datos. SQLite sube
 * `PRAGMA data_version` cuando otro proceso escribe; al notarlo se avisa a las
 * consultas para que la interfaz (o el agente) vea el cambio al momento.
 */
fun watchExternalChanges(driver: SqlDriver, scope: CoroutineScope, periodMs: Long = 1_500) {
    // Siempre el mismo hilo, para leer data_version siempre con la misma conexión.
    val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "lifeos-db-watch").apply { isDaemon = true } }
        .asCoroutineDispatcher()
    val tables = (SYNC_TABLES.map { it.name } + "setting").toTypedArray()
    scope.launch(thread) {
        var last: Long? = null
        while (true) {
            val v = runCatching {
                driver.executeQuery(null, "PRAGMA data_version", { c -> c.next(); QueryResult.Value(c.getLong(0)) }, 0).value
            }.getOrNull()
            if (v != null && last != null && v != last) driver.notifyListeners(*tables)
            if (v != null) last = v
            delay(periodMs)
        }
    }
}
