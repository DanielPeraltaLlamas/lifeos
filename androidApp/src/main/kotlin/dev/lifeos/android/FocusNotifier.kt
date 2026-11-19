package dev.lifeos.android

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.lifeos.data.FocusMath
import dev.lifeos.db.Focus_session

/**
 * Notificaciones del modo enfoque en Android:
 * - una fija con la cuenta atrás mientras hay un bloque en marcha;
 * - una alarma exacta al final que avisa aunque la app esté cerrada.
 */
class FocusNotifier(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private var lastKey: String? = null

    init {
        nm.createNotificationChannel(
            NotificationChannel(CH_RUNNING, "Bloque en marcha", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Cuenta atrás del bloque de enfoque"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Bloque terminado", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso al terminar un bloque de enfoque"
            },
        )
    }

    /** Pinta el estado actual. Se puede llamar a menudo: solo actúa si algo cambia. */
    fun render(s: Focus_session?) {
        val now = System.currentTimeMillis()
        val paused = s != null && FocusMath.isPaused(s, now)
        val endsAt = s?.let { FocusMath.endsAt(it, now) }
        val key = s?.let { "${it.id}|$paused|${endsAt!! / 1000}" }
        if (key == lastKey) return
        lastKey = key
        if (s == null || FocusMath.remaining(s, now) <= 0) {
            nm.cancel(ID_RUNNING)
            alarms.cancel(alarmIntent(null))
            return
        }
        val text = if (paused) "En pausa · se reanuda sola" else "Termina a las ${dev.lifeos.data.Dates.time(endsAt!!)}"
        val n = Notification.Builder(context, CH_RUNNING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(s.title)
            .setContentText("Bloque ${s.block_index} de ${s.block_total} · $text")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(openApp())
            .apply {
                if (!paused) {
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setWhen(endsAt!!)
                    setShowWhen(true)
                }
            }
            .build()
        nm.notify(ID_RUNNING, n)
        scheduleEnd(endsAt!!, s.title)
    }

    private fun scheduleEnd(at: Long, title: String) {
        val pi = alarmIntent(title)
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun alarmIntent(title: String?): PendingIntent = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, FocusAlarmReceiver::class.java).putExtra(EXTRA_TITLE, title),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** Aviso de fin (lo usan la alarma y el vigilante; mismo id, así no se duplica). */
    fun showFinished(title: String) {
        nm.cancel(ID_RUNNING)
        lastKey = null
        val n = Notification.Builder(context, CH_DONE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Bloque terminado")
            .setContentText("50 minutos en «$title». Toca descanso.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        nm.notify(ID_DONE, n)
    }

    companion object {
        const val CH_RUNNING = "focus_running"
        const val CH_DONE = "focus_done"
        const val ID_RUNNING = 50
        const val ID_DONE = 51
        const val EXTRA_TITLE = "title"
    }
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LifeOsApp
        app.focusNotifier.showFinished(intent.getStringExtra(FocusNotifier.EXTRA_TITLE) ?: "tu bloque")
    }
}
