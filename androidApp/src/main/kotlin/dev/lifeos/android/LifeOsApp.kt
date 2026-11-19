package dev.lifeos.android

import android.app.Application
import dev.lifeos.android.widget.startWidgetUpdates
import dev.lifeos.android.blocker.Usage
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.FocusWatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        startWidgetUpdates(this, repository, scope)
        // Minutos de TikTok + Instagram por día, para el Cierre y la Evolución (también en el PC).
        scope.launch {
            var backfilled = false
            while (true) {
                runCatching {
                    if (Usage.hasUsageAccess(this@LifeOsApp)) {
                        val today = Dates.today()
                        val days = if (backfilled) 0..0 else 0..7
                        for (d in days) repository.putScroll(today.plusDays(-d), Usage.limitedDayMs(this@LifeOsApp, d))
                        backfilled = true
                    }
                }
                delay(5 * 60_000)
            }
        }
    }
}
