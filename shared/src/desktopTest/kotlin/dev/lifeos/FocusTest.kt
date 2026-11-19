package dev.lifeos

import dev.lifeos.data.BLOCK_MS
import dev.lifeos.data.FocusMath
import dev.lifeos.data.MAX_PAUSE_MS
import dev.lifeos.data.Repository
import dev.lifeos.data.createDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private class ManualClock(var t: Long = 1_800_000_000_000L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(t)
    fun advance(ms: Long) { t += ms }
}

class FocusTest {
    private val min = 60_000L

    @Test
    fun blockCannotEndEarlyAndPausesAreCapped() = runTest {
        val clock = ManualClock()
        val r = Repository(createDatabase(path = null), clock)
        val id = r.startFocus("Analizar AniList")
        // No se puede empezar otro mientras hay uno en marcha.
        assertEquals(id, r.startFocus("Otra cosa"))

        clock.advance(20 * min)
        assertFalse(r.finishFocusIfDue(id))
        var s = r.activeFocus.first()!!
        assertEquals(30 * min, FocusMath.remaining(s, clock.t))

        // Pausa de 4 min: el tiempo no corre.
        r.pauseFocus(id); clock.advance(4 * min); r.resumeFocus(id)
        s = r.activeFocus.first()!!
        assertEquals(30 * min, FocusMath.remaining(s, clock.t))
        assertEquals(6 * min, FocusMath.pauseLeft(s, clock.t))

        // Pausa larga: a los 6 min restantes se reanuda sola, sin que nadie toque nada.
        r.pauseFocus(id); clock.advance(15 * min)
        s = r.activeFocus.first()!!
        assertFalse(FocusMath.isPaused(s, clock.t))
        assertEquals(30 * min - 9 * min, FocusMath.remaining(s, clock.t))
        assertEquals(0, FocusMath.pauseLeft(s, clock.t))
        r.pauseFocus(id) // ya no queda pausa: no hace nada
        assertNull(r.activeFocus.first()!!.paused_at?.takeIf { FocusMath.isPaused(r.activeFocus.first()!!, clock.t) })

        clock.advance(21 * min)
        assertTrue(r.finishFocusIfDue(id))
        assertNull(r.activeFocus.first())
        val done = r.focusBetween(0, Long.MAX_VALUE).first().single()
        assertEquals(done.started_at + BLOCK_MS + MAX_PAUSE_MS, done.ended_at)
    }

    @Test
    fun blockNumberCountsTodaysBlocksOfTheTask() = runTest {
        val clock = ManualClock()
        val r = Repository(createDatabase(path = null), clock)
        val task = r.addTask("Analizar AniList", estimateBlocks = 2)
        val dayStart = clock.t - 1
        val first = r.startFocus("Analizar AniList", task, todayStart = dayStart)
        assertEquals(1L to 2L, r.activeFocus.first()!!.let { it.block_index to it.block_total })
        clock.advance(BLOCK_MS); r.finishFocusIfDue(first)
        r.startFocus("Analizar AniList", task, todayStart = dayStart)
        assertEquals(2L to 2L, r.activeFocus.first()!!.let { it.block_index to it.block_total })
        clock.advance(BLOCK_MS); r.finishFocusIfDue(r.activeFocus.first()!!.id)
        // Un tercer bloque amplía el total en vez de decir "3 de 2".
        r.startFocus("Analizar AniList", task, todayStart = dayStart)
        assertEquals(3L to 3L, r.activeFocus.first()!!.let { it.block_index to it.block_total })
    }
}
