package dev.lifeos.data

import dev.lifeos.data.Dates.plusDays
import dev.lifeos.db.Deadline
import dev.lifeos.db.Event
import dev.lifeos.db.Focus_session
import dev.lifeos.db.Milestone
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** FOCUS no se crea a mano: son los bloques de enfoque hechos, que el calendario pinta como eventos. */
enum class EventKind(val label: String) { CLASS("Clase"), STUDY("Estudio"), GYM("Gimnasio"), OTHER("Otro"), FOCUS("Foco") }

val Event.kindEnum: EventKind get() = runCatching { EventKind.valueOf(kind) }.getOrDefault(EventKind.OTHER)
val Event.startDate: LocalDate get() = LocalDate.parse(date)
val Event.untilDate: LocalDate? get() = until_date?.let(LocalDate::parse)
val Event.isWeekly: Boolean get() = repeat_weekly != 0L
val Event.skipped: Set<LocalDate>
    get() = skip_dates.split(',').filter { it.isNotBlank() }.map(LocalDate::parse).toSet()

/** Un evento en un día concreto (los semanales tienen una por semana). */
data class Occurrence(val event: Event, val date: LocalDate) {
    val start: Int get() = event.start_min.toInt()
    val end: Int get() = event.end_min.toInt()
    val key: String get() = "${event.id}@$date"
}

/** Lo que va en la fila "todo el día": entregas, exámenes, hitos y tareas. */
data class DayItem(
    val date: LocalDate,
    val title: String,
    val sub: String,
    val kind: Kind,
    val id: String,
    val done: Boolean,
) {
    enum class Kind { DELIVERY, EXAM, MILESTONE, TASK_DUE, PLANNED }
}

object CalendarMath {
    /** "9:00", "13:30" */
    fun hm(min: Int): String = "${min / 60}:${(min % 60).toString().padStart(2, '0')}"

    /** Todas las apariciones de los eventos entre dos fechas (incluidas), ordenadas. */
    fun occurrences(events: List<Event>, from: LocalDate, to: LocalDate): List<Occurrence> = buildList {
        for (e in events) {
            val start = e.startDate
            if (!e.isWeekly) {
                if (start in from..to) add(Occurrence(e, start))
                continue
            }
            val until = e.untilDate
            val skip = e.skipped
            var d = if (start >= from) start else start.plusDays((start.daysUntil(from) + 6) / 7 * 7)
            while (d <= to && (until == null || d <= until)) {
                if (d !in skip) add(Occurrence(e, d))
                d = d.plusDays(7)
            }
        }
    }.sortedWith(compareBy({ it.date }, { it.start }, { it.end }))

    data class Placed(val occ: Occurrence, val column: Int, val columns: Int)

    /**
     * Reparte los eventos de un día en columnas para que los que se solapan
     * queden uno al lado del otro. Los grupos que no se tocan usan todo el ancho.
     */
    fun layoutDay(occs: List<Occurrence>): List<Placed> {
        val result = mutableListOf<Placed>()
        val group = mutableListOf<Pair<Occurrence, Int>>()
        val columnEnds = mutableListOf<Int>()
        var groupEnd = Int.MIN_VALUE
        fun flush() {
            val n = columnEnds.size
            group.forEach { (o, c) -> result += Placed(o, c, n) }
            group.clear(); columnEnds.clear(); groupEnd = Int.MIN_VALUE
        }
        for (o in occs.sortedWith(compareBy({ it.start }, { -it.end }))) {
            if (group.isNotEmpty() && o.start >= groupEnd) flush()
            var col = columnEnds.indexOfFirst { it <= o.start }
            if (col < 0) { columnEnds += o.end; col = columnEnds.lastIndex } else columnEnds[col] = o.end
            group += o to col
            groupEnd = maxOf(groupEnd, o.end)
        }
        flush()
        return result
    }

    const val FOCUS_PREFIX = "focus:"

    /** Los bloques de enfoque como eventos de un día (el que está en marcha llega hasta su fin previsto). */
    fun focusAsEvents(sessions: List<Focus_session>, now: Long): List<Event> = sessions.mapNotNull { s ->
        val end = s.ended_at ?: FocusMath.endsAt(s, now)
        val a = Instant.fromEpochMilliseconds(s.started_at).toLocalDateTime(Dates.zone)
        val b = Instant.fromEpochMilliseconds(end).toLocalDateTime(Dates.zone)
        val startMin = a.hour * 60 + a.minute
        val endMin = if (b.date == a.date) b.hour * 60 + b.minute else 24 * 60 - 1
        if (endMin <= startMin) return@mapNotNull null
        Event(
            FOCUS_PREFIX + s.id, s.project_id, s.title, EventKind.FOCUS.name, a.date.toString(),
            startMin.toLong(), endMin.toLong(), 0, null, "", null, s.created_at, s.updated_at, null,
        )
    }

    /** Entregas, exámenes, hitos y tareas con fecha límite entre dos fechas (incluidas). */
    fun dayItems(
        deadlines: List<Deadline>,
        milestones: List<Milestone>,
        tasks: List<Task>,
        projects: Map<String, Project>,
        from: LocalDate,
        to: LocalDate,
        includePlanned: Boolean = false,
    ): List<DayItem> = buildList {
        deadlines.filter { it.dueDate in from..to }.forEach { d ->
            val kind = if (d.kindEnum == DeadlineKind.EXAM) DayItem.Kind.EXAM else DayItem.Kind.DELIVERY
            val sub = listOfNotNull(d.kindEnum.label, d.project_id?.let(projects::get)?.name).joinToString(" · ")
            add(DayItem(d.dueDate, d.title, sub, kind, d.id, d.done_at != null))
        }
        milestones.groupBy { it.project_id }.forEach { (pid, ms) ->
            ms.sortedBy { it.due_date }.forEachIndexed { i, m ->
                val date = LocalDate.parse(m.due_date)
                if (date in from..to) add(DayItem(date, "Hito ${i + 1} · ${m.title}", projects[pid]?.name ?: "", DayItem.Kind.MILESTONE, m.id, false))
            }
        }
        tasks.forEach { t ->
            val due = t.dueDate
            if (due != null && due in from..to) {
                add(DayItem(due, t.title, t.project_id?.let(projects::get)?.name ?: "Tarea", DayItem.Kind.TASK_DUE, t.id, t.isDone))
            }
            val planned = t.plannedDate
            if (includePlanned && planned != null && planned in from..to && planned != due) {
                add(DayItem(planned, t.title, "En el plan", DayItem.Kind.PLANNED, t.id, t.isDone))
            }
        }
    }.sortedWith(compareBy({ it.date }, { it.kind.ordinal }))
}
