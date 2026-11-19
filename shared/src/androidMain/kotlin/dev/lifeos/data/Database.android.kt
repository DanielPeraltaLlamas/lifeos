package dev.lifeos.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.lifeos.db.LifeOsDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DB_NAME = "lifeos.db"

fun createDriver(context: Context): SqlDriver = AndroidSqliteDriver(LifeOsDatabase.Schema, context, DB_NAME)

fun createDatabase(context: Context): LifeOsDatabase = LifeOsDatabase(createDriver(context))

/** Copia la base de datos (y su diario WAL) a la carpeta privada de la app. */
fun backupDatabase(context: Context) {
    val db = context.getDatabasePath(DB_NAME)
    if (!db.exists()) return
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
    val dir = File(context.filesDir, "copias").apply { mkdirs() }
    listOf("", "-wal", "-shm").forEach { suffix ->
        val f = File(db.path + suffix)
        if (f.exists()) f.copyTo(File(dir, "$DB_NAME$suffix.copia-$stamp"), overwrite = true)
    }
}
