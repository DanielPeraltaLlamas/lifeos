package dev.lifeos

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.lifeos.data.CalendarMath
import dev.lifeos.data.DayItem
import dev.lifeos.data.DeadlineKind
import dev.lifeos.data.EventKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.createDatabase
import dev.lifeos.data.seedIfNeeded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarTest {
    private fun repo() = Repository(createDatabase(path = null))
    private val mon = LocalDate(2026, 11, 16)

    @Test
    fun weeklyEventRepeatsSkipsAndEnds() = runTest {
        val r = repo()
        val id = r.addEvent("Seguridad", EventKind.CLASS, mon, 9 * 60, 11 * 60, weekly = true)
        suspend fun all(from: LocalDate, to: LocalDate) = CalendarMath.occurrences(r.events.first(), from, to).map { it.date }

        // Empieza el lunes 16: nada antes, y una por semana después.
        assertEquals(emptyList(), all(LocalDate(2026, 11, 1), LocalDate(2026, 11, 15)))
        assertEquals(
            listOf(mon, LocalDate(2026, 11, 23), LocalDate(2026, 11, 30), LocalDate(2026, 12, 7)),
            all(LocalDate(2026, 11, 10), LocalDate(2026, 12, 10)),
        )
        // Una ventana que empieza a mitad de semana encuentra el lunes siguiente.
        assertEquals(listOf(LocalDate(2026, 11, 30)), all(LocalDate(2026, 11, 25), LocalDate(2026, 12, 1)))

        r.skipOccurrence(id, LocalDate(2026, 11, 23))
        assertEquals(listOf(mon, LocalDate(2026, 11, 30)), all(mon, LocalDate(2026, 12, 6)))

        r.endSeriesFrom(id, LocalDate(2026, 12, 7))
        assertEquals(listOf(mon, LocalDate(2026, 11, 30)), all(mon, LocalDate(2027, 1, 31)))

        // Cortar desde el primer día borra la serie entera.
        r.endSeriesFrom(id, mon)
        assertTrue(r.events.first().isEmpty())
    }

    @Test
    fun singleEventEditAndDelete() = runTest {
        val r = repo()
        val id = r.addEvent("Gimnasio", EventKind.GYM, mon, 18 * 60, 19 * 60)
        r.updateEvent(id, "Gimnasio pierna", EventKind.GYM, mon.plusDays(2), 17 * 60, 18 * 60 + 30, false, null, null, "Polideportivo")
        val e = r.events.first().single()
        assertEquals("Gimnasio pierna", e.title)
        assertEquals("2026-11-18", e.date)
        assertEquals(17L * 60, e.start_min)
        assertEquals("Polideportivo", e.location)
        assertEquals(1, CalendarMath.occurrences(listOf(e), mon, mon.plusDays(6)).size)
        r.deleteEvent(id)
        assertTrue(r.events.first().isEmpty())
    }

    @Test
    fun deletingSubjectRemovesItsClasses() = runTest {
        val r = repo()
        val sub = r.addProject("Calidad", ProjectKind.SUBJECT)
        r.addEvent("Calidad", EventKind.CLASS, mon, 15 * 60, 17 * 60, weekly = true, projectId = sub)
        r.addEvent("Gimnasio", EventKind.GYM, mon, 18 * 60, 19 * 60)
        r.deleteProject(sub)
        assertEquals(listOf("Gimnasio"), r.events.first().map { it.title })
    }

    @Test
    fun overlappingEventsShareTheWidth() = runTest {
        val r = repo()
        r.addEvent("A", EventKind.CLASS, mon, 9 * 60, 11 * 60)
        r.addEvent("B", EventKind.STUDY, mon, 10 * 60, 12 * 60)
        r.addEvent("C", EventKind.OTHER, mon, 11 * 60, 12 * 60)
        r.addEvent("D", EventKind.GYM, mon, 18 * 60, 19 * 60)
        val placed = CalendarMath.layoutDay(CalendarMath.occurrences(r.events.first(), mon, mon))
            .associate { it.occ.event.title to (it.column to it.columns) }
        assertEquals(0 to 2, placed["A"])
        assertEquals(1 to 2, placed["B"])
        assertEquals(0 to 2, placed["C"]) // reutiliza la columna de A, que ya terminó
        assertEquals(0 to 1, placed["D"]) // no toca a nadie: ancho completo
    }

    @Test
    fun dayItemsCollectDeadlinesMilestonesAndTasks() = runTest {
        val r = repo()
        r.seedIfNeeded()
        val projects = r.projects.first().associateBy { it.id }
        r.addDeadline("Examen final", DeadlineKind.EXAM, LocalDate(2027, 1, 20), null)
        r.addTask("Memoria", dueDate = LocalDate(2027, 1, 25))
        val items = CalendarMath.dayItems(
            r.deadlines.first(), r.milestones.first(), r.tasks.first(), projects,
            LocalDate(2027, 1, 1), LocalDate(2027, 2, 28),
        )
        assertTrue(items.any { it.kind == DayItem.Kind.EXAM && it.title == "Examen final" })
        assertTrue(items.any { it.kind == DayItem.Kind.TASK_DUE && it.title == "Memoria" })
        assertTrue(items.any { it.kind == DayItem.Kind.MILESTONE && it.title.startsWith("Hito ") })
        assertEquals(items.sortedBy { it.date }.map { it.date }, items.map { it.date })
    }

    /** Una base de datos de la Entrega 1 (versión 1, sin tabla event) se migra sin perder datos. */
    @Test
    fun migratesVersion1DatabaseKeepingData() = runTest {
        val dir = Files.createTempDirectory("lifeos-mig")
        val path = dir.resolve("lifeos.db")
        val r1 = Repository(createDatabase(path))
        r1.seedIfNeeded()
        r1.addTask("Tarea de antes")
        // Dejarla como estaba en la Entrega 1.
        JdbcSqliteDriver("jdbc:sqlite:$path", Properties()).use { d ->
            listOf("usage_log", "week_review", "pref", "focus_session").forEach { d.execute(null, "DROP TABLE $it", 0) }
            d.execute(null, "ALTER TABLE daily_plan DROP COLUMN closed_at", 0)
            d.execute(null, "ALTER TABLE note DROP COLUMN title", 0)
            d.execute(null, "DROP INDEX event_date", 0)
            d.execute(null, "DROP TABLE event", 0)
            d.execute(null, "PRAGMA user_version = 1", 0)
        }
        val r2 = Repository(createDatabase(path))
        assertTrue(r2.tasks.first().any { it.title == "Tarea de antes" })
        assertEquals(5, r2.milestones.first().size)
        r2.addEvent("Clase", EventKind.CLASS, mon, 9 * 60, 10 * 60)
        assertEquals(1, r2.events.first().size)
        dir.toFile().deleteRecursively()
    }

    private fun LocalDate.plusDays(n: Int) = with(dev.lifeos.data.Dates) { plusDays(n) }
}
