package dev.lifeos.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.lifeos.db.Daily_plan
import dev.lifeos.db.Deadline
import dev.lifeos.db.Habit
import dev.lifeos.db.Inbox_item
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.db.Milestone
import dev.lifeos.db.Note
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Acceso a la base de datos local. Todas las lecturas son Flows que se
 * actualizan solos; todas las escrituras son funciones suspend.
 * Los borrados son lógicos (deleted_at) para poder sincronizar más adelante.
 */
class Repository(
    private val db: LifeOsDatabase,
    private val clock: Clock = Clock.System,
) {
    private val io = Dispatchers.IO
    private fun now() = clock.now().toEpochMilliseconds()
    private fun newId() = Uuid.random().toString()
    private suspend fun <T> write(block: () -> T): T = withContext(io) { block() }

    // ---------- Lecturas ----------
    val projects: Flow<List<Project>> = db.projectQueries.selectAll().asFlow().mapToList(io)
    val milestones: Flow<List<Milestone>> = db.milestoneQueries.selectAll().asFlow().mapToList(io)
    val tasks: Flow<List<Task>> = db.taskQueries.selectAll().asFlow().mapToList(io)
    val habits: Flow<List<Habit>> = db.habitQueries.selectAll().asFlow().mapToList(io)
    val deadlines: Flow<List<Deadline>> = db.deadlineQueries.selectAll().asFlow().mapToList(io)
    val inbox: Flow<List<Inbox_item>> = db.inboxQueries.selectAll().asFlow().mapToList(io)
    val notes: Flow<List<Note>> = db.noteQueries.selectAll().asFlow().mapToList(io)

    fun dailyPlan(date: LocalDate): Flow<Daily_plan?> =
        db.dailyPlanQueries.selectByDate(date.toString()).asFlow().mapToOneOrNull(io)

    /** habit_id -> (fecha -> veces), entre dos fechas incluidas. */
    fun habitCounts(from: LocalDate, to: LocalDate): Flow<Map<String, HabitCounts>> =
        db.habitQueries.logsBetween(from.toString(), to.toString()).asFlow().mapToList(io).map { rows ->
            rows.groupBy { it.habit_id }.mapValues { (_, v) -> v.associate { LocalDate.parse(it.date) to it.n.toInt() } }
        }

    // ---------- Proyectos ----------
    suspend fun addProject(name: String, kind: ProjectKind, description: String? = null, dueDate: LocalDate? = null): String =
        write {
            val id = newId()
            val t = now()
            db.projectQueries.insert(id, name.trim(), kind.name, description?.trim()?.ifEmpty { null }, dueDate?.toString(), kind.ordinal.toLong(), t, t)
            id
        }

    suspend fun updateProject(id: String, name: String, kind: ProjectKind, description: String?, dueDate: LocalDate?) = write {
        db.projectQueries.update(name.trim(), kind.name, description?.trim()?.ifEmpty { null }, dueDate?.toString(), now(), id)
    }

    /** Borra el proyecto con sus hitos, tareas y entregas. */
    suspend fun deleteProject(id: String) = write {
        db.transaction {
            val t = now()
            db.projectQueries.softDelete(t, id)
            db.milestoneQueries.softDeleteByProject(t, id)
            db.taskQueries.softDeleteByProject(t, id)
            db.deadlineQueries.softDeleteByProject(t, id)
        }
    }

    // ---------- Hitos ----------
    suspend fun addMilestone(projectId: String, title: String, dueDate: LocalDate, deliverable: String?): String = write {
        val id = newId()
        val t = now()
        db.milestoneQueries.insert(id, projectId, title.trim(), deliverable?.trim()?.ifEmpty { null }, dueDate.toString(), t, t)
        id
    }

    suspend fun updateMilestone(id: String, title: String, dueDate: LocalDate, deliverable: String?) = write {
        db.milestoneQueries.update(title.trim(), deliverable?.trim()?.ifEmpty { null }, dueDate.toString(), now(), id)
    }

    /** Borra el hito; sus tareas se quedan en el proyecto, sin hito. */
    suspend fun deleteMilestone(id: String) = write {
        db.transaction {
            val t = now()
            db.milestoneQueries.softDelete(t, id)
            db.taskQueries.detachMilestone(t, id)
        }
    }

    // ---------- Tareas ----------
    suspend fun addTask(
        title: String,
        projectId: String? = null,
        milestoneId: String? = null,
        dueDate: LocalDate? = null,
        estimateBlocks: Int? = null,
        plannedFor: LocalDate? = null,
        notes: String? = null,
    ): String = write {
        val id = newId()
        val t = now()
        db.taskQueries.insert(
            id, projectId, milestoneId, title.trim(), notes?.trim()?.ifEmpty { null },
            dueDate?.toString(), plannedFor?.toString(), estimateBlocks?.toLong(), t, t,
        )
        id
    }

    suspend fun updateTask(
        id: String,
        title: String,
        projectId: String?,
        milestoneId: String?,
        dueDate: LocalDate?,
        estimateBlocks: Int?,
        notes: String?,
    ) = write {
        db.taskQueries.update(
            projectId, milestoneId, title.trim(), notes?.trim()?.ifEmpty { null },
            dueDate?.toString(), estimateBlocks?.toLong(), now(), id,
        )
    }

    suspend fun setTaskDone(id: String, done: Boolean) = write {
        val t = now()
        db.taskQueries.setDone(if (done) t else null, t, id)
    }

    suspend fun setTaskPlanned(id: String, date: LocalDate?) = write {
        db.taskQueries.setPlanned(date?.toString(), now(), id)
    }

    suspend fun deleteTask(id: String) = write { db.taskQueries.softDelete(now(), id) }

    // ---------- Plan del día ----------
    /** Elige "lo primero" del día; la tarea entra también en el plan de ese día. */
    suspend fun setFirstTask(date: LocalDate, taskId: String?) = write {
        db.transaction {
            val t = now()
            db.dailyPlanQueries.ensure(date.toString(), t)
            db.dailyPlanQueries.setFirst(taskId, t, date.toString())
            if (taskId != null) db.taskQueries.setPlanned(date.toString(), t, taskId)
        }
    }

    // ---------- Hábitos ----------
    suspend fun addHabit(name: String, period: HabitPeriod, target: Int, targetMax: Int?): String = write {
        val id = newId()
        val t = now()
        val sort = db.habitQueries.maxSort().executeAsOne() + 1
        db.habitQueries.insert(id, name.trim(), period.name, target.toLong(), targetMax?.toLong(), sort, t, t)
        id
    }

    suspend fun updateHabit(id: String, name: String, period: HabitPeriod, target: Int, targetMax: Int?) = write {
        db.habitQueries.update(name.trim(), period.name, target.toLong(), targetMax?.toLong(), now(), id)
    }

    suspend fun deleteHabit(id: String) = write { db.habitQueries.softDelete(now(), id) }

    suspend fun logHabit(habitId: String, date: LocalDate) = write {
        val t = now()
        db.habitQueries.insertLog(newId(), habitId, date.toString(), t, t, t)
    }

    /** Deshace el último registro del día. */
    suspend fun unlogHabit(habitId: String, date: LocalDate) = write {
        db.habitQueries.lastLogOn(habitId, date.toString()).executeAsOneOrNull()?.let {
            db.habitQueries.softDeleteLog(now(), it)
        }
    }

    // ---------- Entregas y exámenes ----------
    suspend fun addDeadline(title: String, kind: DeadlineKind, dueDate: LocalDate, projectId: String?): String = write {
        val id = newId()
        val t = now()
        db.deadlineQueries.insert(id, projectId, title.trim(), kind.name, dueDate.toString(), t, t)
        id
    }

    suspend fun updateDeadline(id: String, title: String, kind: DeadlineKind, dueDate: LocalDate, projectId: String?) = write {
        db.deadlineQueries.update(projectId, title.trim(), kind.name, dueDate.toString(), now(), id)
    }

    suspend fun setDeadlineDone(id: String, done: Boolean) = write {
        val t = now()
        db.deadlineQueries.setDone(if (done) t else null, t, id)
    }

    suspend fun deleteDeadline(id: String) = write { db.deadlineQueries.softDelete(now(), id) }

    // ---------- Bandeja ----------
    suspend fun capture(text: String, source: Source): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return write {
            val id = newId()
            val t = now()
            db.inboxQueries.insert(id, clean, source.name, t, t)
            id
        }
    }

    suspend fun discardInbox(id: String) = write { db.inboxQueries.softDelete(now(), id) }

    suspend fun inboxToTask(item: Inbox_item, projectId: String?) = write {
        db.transaction {
            val t = now()
            db.taskQueries.insert(newId(), projectId, null, item.text, null, null, null, null, t, t)
            db.inboxQueries.softDelete(t, item.id)
        }
    }

    suspend fun inboxToNote(item: Inbox_item, projectId: String?, kind: NoteKind) = write {
        db.transaction {
            val t = now()
            val url = item.text.takeIf { kind == NoteKind.LINK }?.let { Regex("https?://\\S+").find(it)?.value }
            db.noteQueries.insert(newId(), projectId, kind.name, item.text, url, t, t)
            db.inboxQueries.softDelete(t, item.id)
        }
    }

    // ---------- Ajustes ----------
    suspend fun setting(key: String): String? = write { db.settingQueries.get(key).executeAsOneOrNull() }
    suspend fun putSetting(key: String, value: String) = write { db.settingQueries.put(key, value, now()) }
}
