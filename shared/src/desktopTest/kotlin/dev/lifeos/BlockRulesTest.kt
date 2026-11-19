package dev.lifeos

import dev.lifeos.data.BlockRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockRulesTest {
    private val min = 60_000L
    private val tiktok = "com.zhiliaoapp.musically"
    private val insta = "com.instagram.android"

    @Test
    fun rules() {
        // Fuera de un bloque: Discord libre; TikTok libre hasta el límite.
        assertNull(BlockRules.check("com.discord", null, 0))
        assertNull(BlockRules.check(tiktok, null, 89 * min))
        assertEquals(BlockRules.Reason.DailyLimit, BlockRules.check(insta, null, 90 * min))
        // En un bloque: las tres bloqueadas aunque queden minutos.
        assertEquals(BlockRules.Reason.Focus(123), BlockRules.check("com.discord", 123, 0))
        assertEquals(BlockRules.Reason.Focus(123), BlockRules.check(tiktok, 123, 0))
        // Otras apps, nunca.
        assertNull(BlockRules.check("com.whatsapp", 123, 999 * min))
    }

    @Test
    fun foregroundTimeAddsBothAppsAndOpenSessions() {
        val day = 1_000_000L
        val events = listOf(
            Triple(tiktok, false, day + 5 * min),          // abierta desde antes de medianoche
            Triple(insta, true, day + 10 * min),
            Triple(insta, false, day + 30 * min),          // 20 min
            Triple("com.whatsapp", true, day + 31 * min),  // no cuenta
            Triple(tiktok, true, day + 40 * min),          // sigue abierta
        )
        val now = day + 50 * min
        assertEquals(5 * min + 20 * min + 10 * min, BlockRules.foregroundMs(events, BlockRules.LIMITED.keys, day, now))
    }
}
