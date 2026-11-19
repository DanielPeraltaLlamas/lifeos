package dev.lifeos.data

import dev.lifeos.db.Focus_session

/** Duración de un bloque y pausa máxima por bloque (DECISIONES: no se puede salir; pausar, como mucho 10 min). */
const val BLOCK_MS = 50 * 60_000L
const val MAX_PAUSE_MS = 10 * 60_000L
const val BREAK_MS = 10 * 60_000L

object FocusMath {
    /** Pausa de la pausa actual que cuenta (la pausa total por bloque no pasa de MAX_PAUSE_MS). */
    private fun currentPause(s: Focus_session, now: Long): Long {
        val since = s.paused_at ?: return 0
        return (now - since).coerceIn(0, (MAX_PAUSE_MS - s.paused_ms).coerceAtLeast(0))
    }

    fun totalPause(s: Focus_session, now: Long): Long = s.paused_ms + currentPause(s, now)

    /** En pausa ahora (si se acabó el margen de pausa, el bloque sigue solo). */
    fun isPaused(s: Focus_session, now: Long): Boolean {
        val since = s.paused_at ?: return false
        return now - since < MAX_PAUSE_MS - s.paused_ms
    }

    /** Milisegundos de pausa que quedan en este bloque. */
    fun pauseLeft(s: Focus_session, now: Long): Long = (MAX_PAUSE_MS - totalPause(s, now)).coerceAtLeast(0)

    fun elapsed(s: Focus_session, now: Long): Long =
        (now - s.started_at - totalPause(s, now)).coerceIn(0, s.planned_ms)

    fun remaining(s: Focus_session, now: Long): Long = s.planned_ms - elapsed(s, now)

    /** Momento en que acabará si no se pausa más (si está en pausa, cuenta la pausa máxima que queda). */
    fun endsAt(s: Focus_session, now: Long): Long =
        if (isPaused(s, now)) s.paused_at!! + (MAX_PAUSE_MS - s.paused_ms) + remaining(s, now)
        else now + remaining(s, now)

    /** Fin real de un bloque ya cumplido. */
    fun finishedAt(s: Focus_session, now: Long): Long = s.started_at + s.planned_ms + totalPause(s, now)

    /** "49:59" */
    fun clock(ms: Long): String {
        val total = (ms + 999) / 1000
        return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
    }
}
