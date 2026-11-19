package dev.lifeos.data

import dev.lifeos.db.Deadline
import dev.lifeos.db.Event
import dev.lifeos.db.Focus_session
import dev.lifeos.db.Habit
import dev.lifeos.db.Milestone
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import dev.lifeos.data.Dates.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber

/** Un aviso del asistente. `projectId` sirve para proponer un bloque de estudio. */
data class Hint(val text: String, val kind: Kind, val projectId: String? = null) {
    enum class Kind { PLAN_STUDY, MILESTONE, HABIT, INBOX, SCROLL, NO_FOCUS }
}

/** Datos que mira el asistente. */
data class AssistantInput(
    val today: LocalDate,
    val hour: Int,
    val deadlines: List<Deadline>,
    val events: List<Event>,
    val milestones: List<Milestone>,
    val projects: List<Project>,
    val tasks: List<Task>,
    val habits: List<Habit>,
    val habitCounts: Map<String, HabitCounts>,
    val inboxCount: Int,
    val focusToday: List<Focus_session>,
    val scrollTodayMs: Long?,
    val prefs: Prefs,
)

/** Asistente por reglas (sin IA): avisos cortos, ordenados por importancia, como mucho 3. */
object Assistant {
    fun hints(i: AssistantInput): List<Hint> = buildList {
        val t = i.today
        val projects = i.projects.associateBy { it.id }

        // 1. Entregas y exámenes en ≤ 3 días sin ningún bloque de estudio planificado antes.
        val study = CalendarMath.occurrences(i.events, t, t.plusDays(3)).filter { it.event.kindEnum == EventKind.STUDY }
        i.deadlines.filter { it.done_at == null && t.daysUntil(it.dueDate) in 0..3 }.sortedBy { it.due_date }.forEach { d ->
            val hasBlock = study.any { it.date <= d.dueDate && (d.project_id == null || it.event.project_id == d.project_id) }
            if (!hasBlock) {
                val days = t.daysUntil(d.dueDate)
                val whenText = when (days) { 0 -> "es hoy"; 1 -> "es mañana"; else -> "es el ${Dates.dayName(d.dueDate)}" }
                val what = if (d.kindEnum == DeadlineKind.EXAM) "El examen" else "La entrega"
                add(Hint("$what «${d.title}» $whenText y no tienes bloques de estudio planificados.", Hint.Kind.PLAN_STUDY, d.project_id))
            }
        }

        // 2. Hito del TFG (o de cualquier proyecto) en ≤ 7 días con tareas pendientes.
        i.milestones.filter { t.daysUntil(LocalDate.parse(it.due_date)) in 0..7 }.forEach { m ->
            val left = i.tasks.count { it.milestone_id == m.id && !it.isDone }
            if (left > 0) {
                val days = t.daysUntil(LocalDate.parse(m.due_date))
                val p = projects[m.project_id]?.name ?: "Proyecto"
                add(Hint("$p · «${m.title}» vence en $days día${if (days == 1) "" else "s"}: quedan $left tarea${if (left == 1) "" else "s"}.", Hint.Kind.MILESTONE, m.project_id))
            }
        }

        // 3. Hábito semanal que va justo (quedan menos días que veces por hacer + 1).
        val daysLeft = 7 - t.dayOfWeek.isoDayNumber + 1
        i.habits.filter { it.periodEnum == HabitPeriod.WEEK }.forEach { h ->
            val cur = HabitMath.current(h, i.habitCounts[h.id] ?: emptyMap(), t)
            val missing = h.target.toInt() - cur
            if (missing > 0 && daysLeft <= missing + 1) {
                add(Hint("${h.name}: $cur de ${h.target} esta semana y quedan $daysLeft día${if (daysLeft == 1) "" else "s"}.", Hint.Kind.HABIT))
            }
        }

        // 4. Scroll: queda poco del límite.
        i.scrollTodayMs?.let { used ->
            val left = i.prefs.scrollLimitMs - used
            if (used > 0 && left in 1..(i.prefs.scrollLimitMs / 5)) add(Hint("Te quedan ${left / 60_000} min de TikTok e Instagram hoy.", Hint.Kind.SCROLL))
        }

        // 5. Por la tarde, sin ningún bloque.
        if (i.hour >= 17 && i.focusToday.isEmpty()) add(Hint("Hoy aún no hay bloques de enfoque. Uno de ${i.prefs.blockMin} min basta.", Hint.Kind.NO_FOCUS))

        // 6. Bandeja llena.
        if (i.inboxCount >= 6) add(Hint("Tienes ${i.inboxCount} cosas en la bandeja. Vaciarla lleva 5 minutos.", Hint.Kind.INBOX))
    }.take(3)
}
