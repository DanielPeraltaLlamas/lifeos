package dev.lifeos

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.DeadlineKind
import dev.lifeos.data.EventKind
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.NoteKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.createDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test

/**
 * Capturas y fotogramas para el README, con datos sintéticos («Tarea 1», «Hábito 1»…).
 * Usa una base de datos en memoria: no toca la base real ni Supabase.
 * Solo hace algo si se define README_MEDIA_DIR:
 *   README_MEDIA_DIR=/ruta ./gradlew :shared:desktopTest --tests '*ReadmeMedia*'
 */
class ReadmeMedia {
    private val dir = System.getenv("README_MEDIA_DIR")?.let(::File)

    private fun demoRepo(): Repository = runBlocking {
        val r = Repository(createDatabase(path = null))
        r.putSetting("seeded", "1") // sin la precarga real
        val today = Dates.today()
        val ws = Dates.weekStart(today)

        val p1 = r.addProject("Proyecto 1", ProjectKind.TFG, "Proyecto principal de ejemplo", today.plusDays(70))
        val a = r.addProject("Asignatura A", ProjectKind.SUBJECT)
        val b = r.addProject("Asignatura B", ProjectKind.SUBJECT)
        r.addProject("Proyecto personal 1", ProjectKind.PERSONAL)

        val h1 = r.addMilestone(p1, "Fase 1", today.plusDays(8), "Entregable de la fase 1")
        val h2 = r.addMilestone(p1, "Fase 2", today.plusDays(22), "Entregable de la fase 2")
        r.addMilestone(p1, "Fase 3", today.plusDays(40), "Entregable de la fase 3")

        r.addTask("Tarea 1", p1, h1, estimateBlocks = 1).also { r.setTaskDone(it, true) }
        val t2 = r.addTask("Tarea 2", p1, h1, estimateBlocks = 2, plannedFor = today)
        r.addTask("Tarea 3", p1, h1, estimateBlocks = 2)
        r.addTask("Tarea 4", p1, h2, estimateBlocks = 3)
        r.addTask("Tarea 5", a, dueDate = today.plusDays(2), estimateBlocks = 1, plannedFor = today)
        r.addTask("Tarea 6", b, dueDate = today.plusDays(5), plannedFor = today)
        r.addTask("Tarea 7", dueDate = today.plusDays(12))
        r.setFirstTask(today, t2)

        r.addDeadline("Entrega 1", DeadlineKind.DELIVERY, today.plusDays(1), a)
        r.addDeadline("Examen 1", DeadlineKind.EXAM, today.plusDays(4), b)

        val hb1 = r.addHabit("Hábito 1", HabitPeriod.DAY, target = 3, targetMax = 4)
        val hb2 = r.addHabit("Hábito 2", HabitPeriod.DAY, target = 1, targetMax = null)
        val hb3 = r.addHabit("Hábito 3", HabitPeriod.WEEK, target = 3, targetMax = 4)
        r.logHabit(hb1, today)
        (1..6).forEach { d -> repeat(3) { r.logHabit(hb1, today.plusDays(-d)) } }
        (1..5).forEach { d -> r.logHabit(hb2, today.plusDays(-d)) }
        listOf(1, 3).forEach { d -> r.logHabit(hb3, today.plusDays(-d)) }

        r.addEvent("Clase A", EventKind.CLASS, ws, 9 * 60, 11 * 60, weekly = true, projectId = a, location = "Aula 1")
        r.addEvent("Clase B", EventKind.CLASS, ws.plusDays(1), 9 * 60, 11 * 60, weekly = true, projectId = b, location = "Aula 2")
        r.addEvent("Clase A", EventKind.CLASS, ws.plusDays(3), 11 * 60 + 30, 13 * 60 + 30, weekly = true, projectId = a)
        r.addEvent("Estudio 1", EventKind.STUDY, today, 16 * 60, 17 * 60 + 40, projectId = p1)
        r.addEvent("Estudio 2", EventKind.STUDY, today, 16 * 60 + 30, 17 * 60 + 30, projectId = a)
        r.addEvent("Gimnasio", EventKind.GYM, ws, 18 * 60, 19 * 60, weekly = true)
        r.addEvent("Gimnasio", EventKind.GYM, ws.plusDays(4), 18 * 60, 19 * 60, weekly = true)
        r.addEvent("Evento 1", EventKind.OTHER, ws.plusDays(5), 11 * 60, 12 * 60)

        r.addNote(p1, NoteKind.IDEA, "Idea 1", "Texto de ejemplo de la idea 1.", null)
        r.addNote(p1, NoteKind.NOTE, "Nota 1", "Texto de ejemplo de la nota 1.", null)
        r.addNote(p1, NoteKind.LINK, "Enlace 1", "Referencia de ejemplo.", "https://example.com")
        r.addNote(a, NoteKind.NOTE, "Nota 2", "Apuntes de ejemplo de la asignatura A.", null)

        r.capture("Captura 1", Source.ANDROID)
        r.capture("Captura 2", Source.DESKTOP)
        r
    }

    private fun Image.png(): ByteArray = encodeToData(EncodedImageFormat.PNG)!!.bytes

    /** Abre una escena, deja que carguen fuentes y Flows y entrega un «grabador» de fotogramas. */
    private fun scene(
        repo: Repository, width: Int, height: Int, density: Float, screen: Screen = Screen.TODAY,
        ritual: Ritual? = null, block: (snap: (String) -> Unit, frames: (String, Int) -> Unit) -> Unit,
    ) {
        val src = if (width / density < 600) Source.ANDROID else Source.DESKTOP
        ImageComposeScene(width, height, Density(density)) {
            App(repo, src, screen, initialRitual = ritual)
        }.use { s ->
            var clock = 0L
            fun step(): Image { Thread.sleep(60); clock += 50_000_000L; return s.render(clock) }
            repeat(12) { step() }
            var n = 0
            val snap: (String) -> Unit = { name -> repeat(6) { step() }; File(dir, "$name.png").writeBytes(step().png()) }
            val frames: (String, Int) -> Unit = { prefix, count ->
                repeat(count) { File(dir, "$prefix-%04d.png".format(n++)).writeBytes(step().png()) }
            }
            block(snap, frames)
        }
    }

    private fun still(name: String, screen: Screen, w: Int, h: Int, d: Float = 1f, ritual: Ritual? = null) =
        scene(demoRepo(), w, h, d, screen, ritual) { snap, _ -> snap(name) }

    @Test
    fun screenshots() {
        val out = dir ?: return
        out.mkdirs()
        listOf(Screen.TODAY, Screen.CALENDAR, Screen.TASKS, Screen.NOTES, Screen.HABITS, Screen.INBOX, Screen.EVOLUTION, Screen.SETTINGS)
            .forEach { still("desktop-${it.name.lowercase()}", it, 1280, if (it == Screen.TODAY) 1450 else 1000) }
        Ritual.entries.forEach { still("desktop-ritual-${it.name.lowercase()}", Screen.TODAY, 1280, 1000, ritual = it) }
        listOf(Screen.TODAY, Screen.CALENDAR, Screen.TASKS, Screen.HABITS)
            .forEach { still("phone-${it.name.lowercase()}", it, 1080, 2400, 2.75f) }
        val repo = demoRepo()
        runBlocking {
            val t = repo.tasks.first().first { it.title == "Tarea 2" }
            repo.startFocus(t.title, t.id, t.project_id)
        }
        scene(repo, 1280, 1000, 1f) { snap, _ -> snap("desktop-focus") }
        scene(repo, 1080, 2400, 2.75f) { snap, _ -> snap("phone-focus") }
    }

    /** Fotogramas de Hoy mientras se marcan hábitos, se completa una tarea y se apunta algo. */
    @Test
    fun interaction() {
        val out = dir ?: return
        val frames = File(out, "frames-interaction").apply { deleteRecursively(); mkdirs() }
        val repo = demoRepo()
        val (hb1, t5, t6) = runBlocking {
            Triple(
                repo.habits.first().first { it.name == "Hábito 1" }.id,
                repo.tasks.first().first { it.title == "Tarea 5" }.id,
                repo.tasks.first().first { it.title == "Tarea 6" }.id,
            )
        }
        val today = Dates.today()
        scene(repo, 1280, 1450, 1f) { _, rec ->
            fun f(n: Int) = rec("frames-interaction/f", n).let { }
            f(20)
            runBlocking { repo.logHabit(hb1, today) }; f(14)
            runBlocking { repo.logHabit(hb1, today) }; f(14)
            runBlocking { repo.setTaskDone(t5, true) }; f(16)
            runBlocking { repo.setTaskDone(t6, true) }; f(16)
            runBlocking { repo.capture("Captura 3", Source.DESKTOP) }; f(24)
        }
    }

    /** Fotogramas del modo enfoque con el temporizador corriendo. */
    @Test
    fun focusTimer() {
        val out = dir ?: return
        File(out, "frames-focus").apply { deleteRecursively(); mkdirs() }
        val repo = demoRepo()
        runBlocking {
            val t = repo.tasks.first().first { it.title == "Tarea 2" }
            repo.startFocus(t.title, t.id, t.project_id)
        }
        scene(repo, 1280, 820, 1f) { _, rec ->
            // Cada fotograma espera ~1 s real para que el reloj avance.
            repeat(8) { Thread.sleep(940); rec("frames-focus/f", 1) }
        }
    }
}
