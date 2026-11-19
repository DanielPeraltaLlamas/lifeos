package dev.lifeos.data

import app.cash.sqldelight.db.SqlDriver
import dev.lifeos.sync.SYNC_TABLES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

/**
 * En el PC, la app y el agente comparten la base de datos. Cuando el archivo
 * cambia (lo ha escrito el otro proceso, o este mismo), se avisa a las consultas
 * para que vuelvan a leer. Avisos de más no hacen daño; de menos, sí.
 */
fun watchExternalChanges(driver: SqlDriver, scope: CoroutineScope, path: Path = defaultDatabasePath(), periodMs: Long = 1_500) {
    val tables = (SYNC_TABLES.map { it.name } + "setting").toTypedArray()
    fun stamp(): String = runCatching {
        listOf(path, path.resolveSibling(path.fileName.toString() + "-wal")).joinToString("|") { p ->
            if (Files.exists(p)) "${Files.getLastModifiedTime(p)}:${Files.size(p)}" else "-"
        }
    }.getOrDefault("")
    scope.launch(Dispatchers.IO) {
        var last = stamp()
        while (true) {
            delay(periodMs)
            val now = stamp()
            if (now != last) {
                last = now
                driver.notifyListeners(*tables)
            }
        }
    }
}
