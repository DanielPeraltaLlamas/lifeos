package dev.lifeos

import dev.lifeos.data.NoteKind
import dev.lifeos.data.Prefs
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.createDatabase
import dev.lifeos.data.isDone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Flujos completos, de principio a fin, como los usaría el usuario. */
class FlowsTest {
    private val today = LocalDate(2026, 11, 19)
    private val tomorrow = LocalDate(2026, 11, 20)
    private fun repo(clock: Clock = Clock.System) = Repository(createDatabase(path = null), clock)

    @Test
    fun inboxItemsBecomeTaskNoteOrNothing() = runTest {
        val r = repo()
        val tfg = r.addProject("TFG", ProjectKind.TFG)
        r.capture("Comprar proteína", Source.WIDGET)
        r.capture("Idea: importar desde Backloggd", Source.ANDROID)
        r.capture("Mirar https://anilist.co/graphql", Source.DESKTOP)
        r.capture("algo sin importancia", Source.FOCUS)
        val items = r.inbox.first().associateBy { it.text }

        r.inboxToTask(items.getValue("Comprar proteína"), null)
        r.inboxToNote(items.getValue("Idea: importar desde Backloggd"), tfg, NoteKind.IDEA)
        r.inboxToNote(items.getValue("Mirar https://anilist.co/graphql"), tfg, NoteKind.LINK)
        r.discardInbox(items.getValue("algo sin importancia").id)

        assertTrue(r.inbox.first().isEmpty())
        assertEquals(listOf("Comprar proteína"), r.tasks.first().map { it.title })
        val notes = r.notes.first().filter { it.project_id == tfg }
        assertEquals(2, notes.size)
        assertEquals("https://anilist.co/graphql", notes.first { it.kind == NoteKind.LINK.name }.url)
    }

    @Test
    fun closingTheDayPreparesTomorrow() = runTest {
        val r = repo()
        val a = r.addTask("Repasar tema 5", plannedFor = today)
        val b = r.addTask("Analizar AniList", plannedFor = today)
        val c = r.addTask("Hecha hoy", plannedFor = today).also { r.setTaskDone(it, true) }
        val d = r.addTask("Informe de pruebas")

        r.closeDay(today, tomorrow, chosen = listOf(b, d), first = d)

        val byId = r.tasks.first().associateBy { it.id }
        assertNull(byId.getValue(a).planned_date)                  // pendiente no elegida: sale del plan
        assertEquals(tomorrow.toString(), byId.getValue(b).planned_date)
        assertEquals(tomorrow.toString(), byId.getValue(d).planned_date)
        assertEquals(today.toString(), byId.getValue(c).planned_date) // lo hecho se queda en su día
        assertTrue(byId.getValue(c).isDone)
        assertEquals(d, r.dailyPlan(tomorrow).first()?.first_task_id)
        assertNotNull(r.dailyPlan(today).first()?.closed_at)
    }

    @Test
    fun notesCanBeCreatedEditedMovedAndDeleted() = runTest {
        val r = repo()
        val tfg = r.addProject("TFG", ProjectKind.TFG)
        val personal = r.addProject("Blog", ProjectKind.PERSONAL)
        val id = r.addNote(tfg, NoteKind.NOTE, "Preguntas para el tutor", "¿Hace falta memoria parcial?", null)
        r.updateNote(id, personal, NoteKind.LINK, "Artículo", "Para leer", "https://example.org")
        val n = r.notes.first().single()
        assertEquals(personal, n.project_id)
        assertEquals("Artículo", n.title)
        assertEquals("https://example.org", n.url)
        r.deleteNote(id)
        assertTrue(r.notes.first().isEmpty())
    }

    @Test
    fun settingsChangeBlockLengthAndLimits() = runTest {
        val r = repo()
        assertEquals(50, r.prefs.first().blockMin)
        r.putPref(Prefs.BLOCK, "25")
        r.putPref(Prefs.SCROLL_LIMIT, "60")
        r.putPref(Prefs.WEB_FOCUS, "youtube.com, X.com ,")
        val p = r.prefs.first()
        assertEquals(25 * 60_000L, p.blockMs)
        assertEquals(60 * 60_000L, p.scrollLimitMs)
        assertEquals(listOf("youtube.com", "x.com"), p.webFocus)
        r.startFocus("Prueba")
        assertEquals(25 * 60_000L, r.activeFocus.first()!!.planned_ms)
    }

    @Test
    fun weekReviewKeepsNoteAndThreeGoals() = runTest {
        val r = repo()
        val week = LocalDate(2026, 11, 16)
        r.saveWeekReview(week, "Buena semana", listOf("Cerrar el H1", "", "3 veces gimnasio", "sobra"))
        var w = r.weekReview(week).first()!!
        assertEquals("Buena semana", w.note)
        assertEquals("Cerrar el H1\n3 veces gimnasio\nsobra", w.goals)
        assertNull(w.done_at)
        r.finishWeekReview(week)
        w = r.weekReview(week).first()!!
        assertNotNull(w.done_at)
    }

    @Test
    fun scrollIsOnlyRewrittenWhenItChangesAMinute() = runTest {
        var t = 1_800_000_000_000L
        val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(t) }
        val r = repo(clock)
        r.putScroll(today, 10 * 60_000L)
        val first = r.usageBetween(today, today).first().single().updated_at
        t += 1_000
        r.putScroll(today, 10 * 60_000L + 30_000) // +30 s: no se reescribe (no hace falta sincronizar)
        assertEquals(first, r.usageBetween(today, today).first().single().updated_at)
        t += 1_000
        r.putScroll(today, 12 * 60_000L)
        assertEquals(12 * 60_000L, r.usageBetween(today, today).first().single().scroll_ms)
    }
}
