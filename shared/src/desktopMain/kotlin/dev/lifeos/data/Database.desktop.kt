package dev.lifeos.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.lifeos.db.LifeOsDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** Base de datos en ~/.local/share/lifeos/lifeos.db (XDG_DATA_HOME si está definido). */
fun defaultDatabasePath(): Path {
    val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
        ?: Path.of(System.getProperty("user.home"), ".local", "share")
    return base.resolve("lifeos").resolve("lifeos.db")
}

fun createDatabase(path: Path? = defaultDatabasePath()): LifeOsDatabase {
    val url = if (path == null) JdbcSqliteDriver.IN_MEMORY else {
        Files.createDirectories(path.parent)
        "jdbc:sqlite:$path"
    }
    return LifeOsDatabase(JdbcSqliteDriver(url, Properties(), LifeOsDatabase.Schema))
}
