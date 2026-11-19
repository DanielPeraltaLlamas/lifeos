package dev.lifeos.data

/**
 * Preferencias que se cambian en Ajustes y se comparten entre el móvil, el PC,
 * el agente y la extensión (tabla `pref`, sincronizada). Si no hay valor, se usa el de aquí.
 */
data class Prefs(val raw: Map<String, String> = emptyMap()) {
    private fun int(key: String, def: Int) = raw[key]?.toIntOrNull() ?: def
    private fun list(key: String, def: List<String>) =
        raw[key]?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() } ?: def

    /** Límite diario de TikTok + Instagram, en minutos. */
    val scrollLimitMin: Int get() = int(SCROLL_LIMIT, 90)
    val blockMin: Int get() = int(BLOCK, 50)
    val breakMin: Int get() = int(BREAK, 10)
    /** Webs bloqueadas solo durante los bloques. */
    val webFocus: List<String> get() = list(WEB_FOCUS, DEFAULT_WEB_FOCUS)
    /** Webs bloqueadas siempre en el ordenador. */
    val webAlways: List<String> get() = list(WEB_ALWAYS, DEFAULT_WEB_ALWAYS)
    /** Hora (0-23) a partir de la que Hoy propone cerrar el día. */
    val closeHour: Int get() = int(CLOSE_HOUR, 21)
    /** Día de la revisión semanal (1 = lunes … 7 = domingo). */
    val reviewDay: Int get() = int(REVIEW_DAY, 7)

    val scrollLimitMs: Long get() = scrollLimitMin * 60_000L
    val blockMs: Long get() = blockMin * 60_000L
    val breakMs: Long get() = breakMin * 60_000L

    companion object {
        const val SCROLL_LIMIT = "scroll.limit.min"
        const val BLOCK = "focus.block.min"
        const val BREAK = "focus.break.min"
        const val WEB_FOCUS = "web.focus"
        const val WEB_ALWAYS = "web.always"
        const val CLOSE_HOUR = "ritual.close.hour"
        const val REVIEW_DAY = "ritual.review.day"

        val DEFAULT_WEB_FOCUS = listOf("youtube.com", "youtu.be", "reddit.com", "redd.it", "twitch.tv", "discord.com", "discord.gg", "discordapp.com")
        val DEFAULT_WEB_ALWAYS = listOf("tiktok.com", "instagram.com")
    }
}
