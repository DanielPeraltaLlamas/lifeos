package dev.lifeos.data

import dev.lifeos.db.Deadline
import dev.lifeos.db.Habit
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import kotlinx.datetime.LocalDate

enum class ProjectKind { TFG, PERSONAL, SUBJECT }
enum class HabitPeriod { DAY, WEEK }
enum class DeadlineKind(val label: String) { DELIVERY("Entrega"), EXAM("Examen") }
enum class NoteKind(val label: String) { IDEA("Idea"), NOTE("Nota"), LINK("Enlace") }
enum class Source(val label: String) { DESKTOP("ordenador"), ANDROID("móvil"), WIDGET("widget"), FOCUS("modo enfoque") }

val Project.kindEnum: ProjectKind get() = runCatching { ProjectKind.valueOf(kind) }.getOrDefault(ProjectKind.PERSONAL)
val Project.dueDate: LocalDate? get() = due_date?.let(LocalDate::parse)

val Task.isDone: Boolean get() = done_at != null
val Task.dueDate: LocalDate? get() = due_date?.let(LocalDate::parse)
val Task.plannedDate: LocalDate? get() = planned_date?.let(LocalDate::parse)

val Habit.periodEnum: HabitPeriod get() = runCatching { HabitPeriod.valueOf(period) }.getOrDefault(HabitPeriod.DAY)

val Deadline.kindEnum: DeadlineKind get() = runCatching { DeadlineKind.valueOf(kind) }.getOrDefault(DeadlineKind.DELIVERY)
val Deadline.dueDate: LocalDate get() = LocalDate.parse(due_date)

/** Registros de un hábito: fecha -> número de veces. */
typealias HabitCounts = Map<LocalDate, Int>
