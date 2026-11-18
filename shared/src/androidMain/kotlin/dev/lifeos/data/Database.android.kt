package dev.lifeos.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.lifeos.db.LifeOsDatabase

fun createDatabase(context: Context): LifeOsDatabase =
    LifeOsDatabase(AndroidSqliteDriver(LifeOsDatabase.Schema, context, "lifeos.db"))
