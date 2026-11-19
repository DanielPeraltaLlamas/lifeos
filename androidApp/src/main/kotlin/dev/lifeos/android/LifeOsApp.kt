package dev.lifeos.android

import android.app.Application
import dev.lifeos.data.FocusWatcher
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

    val focusNotifier by lazy { FocusNotifier(this) }

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        sync.start(scope, focusPollMs = 60_000)
        FocusWatcher(
            repository,
            onChange = { focusNotifier.render(it) },
            onFinished = { focusNotifier.showFinished(it.title) },
        ).start(scope)
    }
}
