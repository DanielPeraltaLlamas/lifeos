package dev.lifeos

import dev.lifeos.data.Assistant
import dev.lifeos.data.AssistantInput
import dev.lifeos.data.DeadlineKind
import dev.lifeos.data.EventKind
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.Hint
import dev.lifeos.data.Prefs
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.createDatabase
import dev.lifeos.data.seedIfNeeded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantTest {
    private val thu = LocalDate(2026, 11, 19)

    private suspend fun input(r: Repository, today: LocalDate = thu, hour: Int = 10, scroll: Long? = null) = AssistantInput(
        today, hour, r.deadlines.first(), r.events.first(), r.milestones.first(), r.projects.first(), r.tasks.first(),
        r.habits.first(), r.habitCounts(LocalDate(2026, 9, 1), today).first(), r.inbox.first().size, emptyList(), scroll, Prefs(),
    )

    @Test
    fun deadlineWithoutStudyBlockIsFlaggedUntilOneIsPlanned() = runTest {
        val r = Repository(createDatabase(path = null))
        val sec = r.addProject("Seguridad", ProjectKind.SUBJECT)
        r.addDeadline("Cuestionario", DeadlineKind.DELIVERY, thu.plusDays1(1), sec)
        var hints = Assistant.hints(input(r))
        assertEquals(Hint.Kind.PLAN_STUDY, hints.first().kind)
        assertTrue("es mañana" in hints.first().text)
        r.addEvent("Repaso", EventKind.STUDY, thu, 16 * 60, 17 * 60, projectId = sec)
        hints = Assistant.hints(input(r))
        assertTrue(hints.none { it.kind == Hint.Kind.PLAN_STUDY })
    }

    @Test
    fun weeklyHabitBehindAndQuietDay() = runTest {
        val r = Repository(createDatabase(path = null))
        r.seedIfNeeded() // Gimnasio: 3 por semana
        // Sábado sin gimnasio: quedan 2 días para 3 veces.
        val sat = LocalDate(2026, 11, 21)
        val hints = Assistant.hints(input(r, sat, hour = 18))
        assertTrue(hints.any { it.kind == Hint.Kind.HABIT && it.text.startsWith("Gimnasio: 0 de 3") })
        assertTrue(hints.any { it.kind == Hint.Kind.NO_FOCUS })
        assertTrue(hints.size <= 3)
    }

    @Test
    fun nothingToSayOnAQuietMorning() = runTest {
        val r = Repository(createDatabase(path = null))
        assertEquals(emptyList(), Assistant.hints(input(r)))
    }

    private fun LocalDate.plusDays1(n: Int) = with(dev.lifeos.data.Dates) { plusDays(n) }
}
