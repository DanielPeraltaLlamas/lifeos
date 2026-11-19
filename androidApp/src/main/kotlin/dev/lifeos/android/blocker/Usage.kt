package dev.lifeos.android.blocker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import dev.lifeos.data.BlockRules
import java.util.Calendar

/** Minutos de TikTok + Instagram de hoy y estado de los permisos. */
object Usage {
    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Tiempo en primer plano hoy de las apps con límite (0 si falta el permiso). */
    fun limitedTodayMs(context: Context): Long = limitedMs(context, startOfToday(), System.currentTimeMillis())

    /** Lo mismo para un día cualquiera (hace `daysAgo` días). */
    fun limitedDayMs(context: Context, daysAgo: Int): Long {
        val start = startOfToday() - daysAgo * 86_400_000L
        return limitedMs(context, start, minOf(start + 86_400_000L, System.currentTimeMillis()))
    }

    private fun limitedMs(context: Context, from: Long, now: Long): Long {
        if (!hasUsageAccess(context)) return 0
        val usm = context.getSystemService(UsageStatsManager::class.java)
        // Se pide desde un poco antes de medianoche para saber qué estaba abierto a esa hora.
        val events = usm.queryEvents(from - 6 * 3_600_000L, now)
        val list = mutableListOf<Triple<String, Boolean, Long>>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val resumed = when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> true
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> false
                else -> continue
            }
            if (e.packageName in BlockRules.LIMITED) list += Triple(e.packageName, resumed, e.timeStamp)
        }
        // Los eventos de antes de medianoche solo sirven para saber qué estaba abierto.
        val before = list.filter { it.third < from }
        val openAtMidnight = before.groupBy { it.first }.mapNotNull { (pkg, ev) -> pkg.takeIf { ev.last().second } }
        val today = openAtMidnight.map { Triple(it, true, from) } + list.filter { it.third in from..now }
        return BlockRules.foregroundMs(today, BlockRules.LIMITED.keys, from, now)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibility(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = "${context.packageName}/${BlockerService::class.java.name}"
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { it.equals(me, ignoreCase = true) }
    }
}
