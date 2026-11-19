package dev.lifeos.data

/**
 * Qué se bloquea en el móvil (DECISIONES, Entrega 4b):
 * - TikTok e Instagram: límite diario compartido; durante un bloque, bloqueadas del todo.
 * - Discord: solo durante los bloques.
 */
object BlockRules {
    const val DAILY_LIMIT_MS = 90 * 60_000L

    /** Paquetes de Android de cada app (varias versiones según región). */
    val LIMITED = mapOf(
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.ss.android.ugc.aweme" to "TikTok",
        "com.zhiliaoapp.musically.go" to "TikTok Lite",
        "com.instagram.android" to "Instagram",
        "com.instagram.lite" to "Instagram Lite",
    )
    val FOCUS_ONLY = mapOf("com.discord" to "Discord")

    fun appName(pkg: String): String? = LIMITED[pkg] ?: FOCUS_ONLY[pkg]

    sealed interface Reason {
        /** Hay un bloque de enfoque en marcha; acaba en `endsAt`. */
        data class Focus(val endsAt: Long) : Reason
        /** Se acabaron los minutos de hoy. */
        data object DailyLimit : Reason
    }

    /** Devuelve por qué se bloquea `pkg`, o null si se puede usar. */
    fun check(pkg: String, focusEndsAt: Long?, usedTodayMs: Long): Reason? = when {
        pkg !in LIMITED && pkg !in FOCUS_ONLY -> null
        focusEndsAt != null -> Reason.Focus(focusEndsAt)
        pkg in LIMITED && usedTodayMs >= DAILY_LIMIT_MS -> Reason.DailyLimit
        else -> null
    }

    /**
     * Tiempo en primer plano de unos paquetes desde `from` hasta `now`, a partir de
     * los eventos del sistema (paquete, true = pasa a primer plano / false = sale, instante).
     */
    fun foregroundMs(events: List<Triple<String, Boolean, Long>>, packages: Set<String>, from: Long, now: Long): Long {
        var total = 0L
        val since = mutableMapOf<String, Long>()
        val seen = mutableSetOf<String>()
        for ((pkg, resumed, t) in events.sortedBy { it.third }) {
            if (pkg !in packages) continue
            if (resumed) {
                since.putIfAbsent(pkg, t)
            } else {
                // Si la primera señal del día es "sale", estaba abierta desde antes de medianoche.
                val start = since.remove(pkg) ?: if (pkg !in seen) from else null
                if (start != null) total += (t - maxOf(start, from)).coerceAtLeast(0)
            }
            seen += pkg
        }
        since.values.forEach { total += (now - maxOf(it, from)).coerceAtLeast(0) }
        return total
    }
}
