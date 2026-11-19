package dev.lifeos.android

import android.app.Application
import dev.lifeos.data.Repository
import dev.lifeos.data.backupDatabase
import dev.lifeos.data.createDriver
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.sync.SyncEngine
import dev.lifeos.sync.createSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LifeOsApp : Application() {
    private val driver by lazy { createDriver(this) }
    val repository: Repository by lazy { Repository(LifeOsDatabase(driver)) }
    val sync: SyncEngine by lazy { createSyncEngine(driver, repository, backup = { backupDatabase(this) }) }

    override fun onCreate() {
        super.onCreate()
        sync.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
