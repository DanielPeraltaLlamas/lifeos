package dev.lifeos.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.lifeos.db.LifeOsDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

/** Base de datos en ~/.local/share/lifeos/lifeos.db (XDG_DATA_HOME si está definido). */
fun defaultDatabasePath(): Path {
    val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
        ?: Path.of(System.getProperty("user.home"), ".local", "share")
    return base.resolve("lifeos").resolve("lifeos.db")
}

fun createDriver(path: Path? = defaultDatabasePath()): SqlDriver {
    val url = if (path == null) JdbcSqliteDriver.IN_MEMORY else {
        Files.createDirectories(path.parent)
        "jdbc:sqlite:$path"
    }
    return JdbcSqliteDriver(url, Properties(), LifeOsDatabase.Schema)
}

fun createDatabase(path: Path? = defaultDatabasePath()): LifeOsDatabase = LifeOsDatabase(createDriver(path))

/** Copia lifeos.db a lifeos.db.copia-AAAAMMDD-HHMMSS en la misma carpeta. */
fun backupDatabase(path: Path = defaultDatabasePath()) {
    if (!Files.exists(path)) return
    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    Files.copy(path, path.resolveSibling("${path.fileName}.copia-$stamp"), StandardCopyOption.REPLACE_EXISTING)
}
