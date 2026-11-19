package dev.lifeos

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.DeadlineKind
import dev.lifeos.data.EventKind
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.createDatabase
import dev.lifeos.data.seedIfNeeded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Genera capturas de las pantallas (sin ventana) para revisar el diseño.
 * Solo hace algo si se define SCREENSHOTS_DIR:
 *   SCREENSHOTS_DIR=/tmp/shots ./gradlew :shared:desktopTest --tests '*Screenshots*'
 */
class Screenshots {
    private val dir = System.getenv("SCREENSHOTS_DIR")?.let(::File)

    private fun sampleRepo(): Repository = runBlocking {
        val r = Repository(createDatabase(path = null))
        r.seedIfNeeded()
        val today = Dates.today()
        val tfg = r.projects.first().first { it.kind == ProjectKind.TFG.name }.id
        val h1 = r.milestones.first().first().id
        val sec = r.addProject("Seguridad", ProjectKind.SUBJECT)
        r.addTask("Elegir plataformas y criterios", tfg, h1, estimateBlocks = 1).also { r.setTaskDone(it, true) }
        val first = r.addTask("Analizar AniList y MyAnimeList", tfg, h1, estimateBlocks = 2)
        r.addTask("Analizar Backloggd y HowLongToBeat", tfg, h1, estimateBlocks = 2)
        r.addTask("Repasar tema 5", sec, dueDate = today.plusDays(2), estimateBlocks = 1, plannedFor = today)
        r.addTask("Renovar el abono del gimnasio", dueDate = today.plusDays(13))
        r.setFirstTask(today, first)
        r.addDeadline("Cuestionario tema 5", DeadlineKind.DELIVERY, today.plusDays(1), sec)
        val habits = r.habits.first()
        repeat(2) { r.logHabit(habits[0].id, today) }
        (1..3).forEach { d -> repeat(3) { r.logHabit(habits[0].id, today.plusDays(-d)) } }
        (1..4).forEach { d -> r.logHabit(habits[1].id, today.plusDays(-d)) }
        r.logHabit(habits[2].id, today.plusDays(-1))
        val ws = Dates.weekStart(today)
        val sd = r.addProject("Sistemas Distribuidos", ProjectKind.SUBJECT)
        r.addEvent("Sistemas Distribuidos", EventKind.CLASS, ws, 9 * 60, 11 * 60, weekly = true, projectId = sd, location = "Aula 2.1")
        r.addEvent("Seguridad", EventKind.CLASS, ws.plusDays(1), 9 * 60, 11 * 60, weekly = true, projectId = sec)
        r.addEvent("Seguridad", EventKind.CLASS, ws.plusDays(3), 11 * 60 + 30, 13 * 60 + 30, weekly = true, projectId = sec)
        r.addEvent("TFG · análisis", EventKind.STUDY, today, 16 * 60, 17 * 60 + 40, projectId = tfg)
        r.addEvent("Repaso tema 5", EventKind.STUDY, today, 16 * 60 + 30, 17 * 60 + 30, projectId = sec)
        r.addEvent("Gimnasio", EventKind.GYM, ws, 18 * 60, 19 * 60, weekly = true)
        r.addEvent("Gimnasio", EventKind.GYM, ws.plusDays(5), 11 * 60, 12 * 60, weekly = true)
        r.addDeadline("Examen Seguridad", DeadlineKind.EXAM, today.plusDays(4), sec)
        r.capture("Mirar si la API de AniList tiene límite de peticiones", Source.ANDROID)
        r.capture("Idea TFG: importar también desde Backloggd", Source.DESKTOP)
        r
    }

    private fun shot(name: String, screen: Screen, width: Int, height: Int, density: Float = 1f) {
        val out = dir ?: return
        out.mkdirs()
        val repo = sampleRepo()
        ImageComposeScene(width, height, Density(density)) {
            App(repo, if (width / density < 600) Source.ANDROID else Source.DESKTOP, screen)
        }.use { scene ->
            // Varios fotogramas para que carguen la fuente y los Flows.
            var image = scene.render(0)
            repeat(12) { i ->
                Thread.sleep(80)
                image = scene.render((i + 1) * 100_000_000L)
            }
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG")
            File(out, "$name.png").writeBytes(data.bytes)
        }
    }

    @Test
    fun desktop() {
        Screen.entries.forEach { shot("desktop-${it.name.lowercase()}", it, 1280, 860) }
        shot("desktop-narrow-today", Screen.TODAY, 908, 1000)
    }

    @Test
    fun phone() {
        Screen.entries.forEach { shot("phone-${it.name.lowercase()}", it, 1080, 2400, 2.75f) }
    }
}
