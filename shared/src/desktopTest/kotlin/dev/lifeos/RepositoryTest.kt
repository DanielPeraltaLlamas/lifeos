package dev.lifeos

import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.NoteKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.createDatabase
import dev.lifeos.data.isDone
import dev.lifeos.data.seedIfNeeded
import dev.lifeos.theme.Caelestia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.Color

class RepositoryTest {
    private fun repo() = Repository(createDatabase(path = null))

    @Test
    fun seedCreatesTfgMilestonesAndHabitsOnlyOnce() = runTest {
        val r = repo()
        r.seedIfNeeded()
        r.seedIfNeeded()
        val projects = r.projects.first()
        assertEquals(1, projects.size)
        assertEquals("TFG", projects[0].name)
        assertEquals("2027-02-02", projects[0].due_date)
        assertEquals(5, r.milestones.first().size)
        assertEquals(listOf("Lavarse los dientes", "Ducharse", "Gimnasio"), r.habits.first().map { it.name })
    }

    @Test
    fun taskLifecycle() = runTest {
        val r = repo()
        val p = r.addProject("Calidad del Software", ProjectKind.SUBJECT)
        val id = r.addTask("Práctica 3", projectId = p, estimateBlocks = 2)
        var t = r.tasks.first().single()
        assertEquals("Práctica 3", t.title)
        assertEquals(2L, t.estimate_blocks)

        r.updateTask(id, "Práctica 3 · memoria", p, null, LocalDate(2026, 11, 25), 3, null)
        r.setTaskDone(id, true)
        t = r.tasks.first().single()
        assertEquals("Práctica 3 · memoria", t.title)
        assertEquals("2026-11-25", t.due_date)
        assertTrue(t.isDone)

        r.setTaskDone(id, false)
        assertTrue(!r.tasks.first().single().isDone)

        r.deleteTask(id)
        assertTrue(r.tasks.first().isEmpty())
    }

    @Test
    fun deletingProjectRemovesItsTasksMilestonesAndDeadlines() = runTest {
        val r = repo()
        val p = r.addProject("Web personal", ProjectKind.PERSONAL)
        val m = r.addMilestone(p, "Diseño", LocalDate(2026, 12, 1), null)
        r.addTask("Boceto", projectId = p, milestoneId = m)
        r.addTask("Suelta")
        r.addDeadline("Demo", dev.lifeos.data.DeadlineKind.DELIVERY, LocalDate(2026, 12, 5), p)
        r.deleteProject(p)
        assertTrue(r.projects.first().isEmpty())
        assertTrue(r.milestones.first().isEmpty())
        assertTrue(r.deadlines.first().isEmpty())
        assertEquals(listOf("Suelta"), r.tasks.first().map { it.title })
    }

    @Test
    fun deletingMilestoneKeepsTasks() = runTest {
        val r = repo()
        val p = r.addProject("TFG", ProjectKind.TFG)
        val m = r.addMilestone(p, "H1", LocalDate(2026, 11, 30), null)
        r.addTask("Analizar AniList", projectId = p, milestoneId = m)
        r.deleteMilestone(m)
        val t = r.tasks.first().single()
        assertEquals(p, t.project_id)
        assertNull(t.milestone_id)
    }

    @Test
    fun firstTaskIsAlsoPlanned() = runTest {
        val r = repo()
        val day = LocalDate(2026, 11, 19)
        val a = r.addTask("A")
        val b = r.addTask("B")
        r.setFirstTask(day, a)
        assertEquals(a, r.dailyPlan(day).first()?.first_task_id)
        r.setFirstTask(day, b)
        assertEquals(b, r.dailyPlan(day).first()?.first_task_id)
        val planned = r.tasks.first().filter { it.planned_date == day.toString() }.map { it.title }.toSet()
        assertEquals(setOf("A", "B"), planned)
        assertNull(r.dailyPlan(LocalDate(2026, 11, 20)).first())
    }

    @Test
    fun habitLogsAndStreaks() = runTest {
        val r = repo()
        val teeth = r.addHabit("Dientes", HabitPeriod.DAY, 3, 4)
        val gym = r.addHabit("Gimnasio", HabitPeriod.WEEK, 3, 4)
        val thu = LocalDate(2026, 11, 19) // jueves
        repeat(3) { r.logHabit(teeth, thu) }
        repeat(3) { r.logHabit(teeth, LocalDate(2026, 11, 18)) }
        r.logHabit(teeth, LocalDate(2026, 11, 17))
        r.unlogHabit(teeth, thu)
        r.logHabit(gym, LocalDate(2026, 11, 16)) // lunes
        r.logHabit(gym, thu)

        val counts = r.habitCounts(LocalDate(2026, 11, 1), thu).first()
        val habits = r.habits.first().associateBy { it.id }
        assertEquals(2, HabitMath.current(habits.getValue(teeth), counts.getValue(teeth), thu))
        // Hoy aún no llega a 3: la racha cuenta desde ayer (1 día; el 17 solo hubo 1).
        assertEquals(1, HabitMath.streak(habits.getValue(teeth), counts.getValue(teeth), thu))
        assertEquals(2, HabitMath.current(habits.getValue(gym), counts.getValue(gym), thu))
    }

    @Test
    fun inboxToTaskAndNote() = runTest {
        val r = repo()
        val p = r.addProject("TFG", ProjectKind.TFG)
        r.capture("   ", Source.DESKTOP)
        assertTrue(r.inbox.first().isEmpty())
        r.capture("Mirar límite de la API de AniList", Source.ANDROID)
        r.capture("Idea: importar desde https://backloggd.com", Source.DESKTOP)
        val items = r.inbox.first()
        assertEquals(2, items.size)
        r.inboxToTask(items.first { it.text.startsWith("Mirar") }, p)
        r.inboxToNote(items.first { it.text.startsWith("Idea") }, p, NoteKind.LINK)
        assertTrue(r.inbox.first().isEmpty())
        assertEquals(p, r.tasks.first().single().project_id)
        val note = r.notes.first().single()
        assertEquals("https://backloggd.com", note.url)
    }

    @Test
    fun caelestiaSchemeIsParsed() {
        val json = """{"name":"dynamic","mode":"light","colours":{"primary":"e4bade","surfaceContainer":"1e181d","onSurface":"#f1e1eb"}}"""
        val s = Caelestia.parse(json)
        assertEquals(Color(0xFFE4BADE), s.primary)
        assertEquals(Color(0xFF1E181D), s.surfaceContainer)
        assertEquals(Color(0xFFF1E1EB), s.onSurface)
        assertNotNull(s.background)
    }
}
