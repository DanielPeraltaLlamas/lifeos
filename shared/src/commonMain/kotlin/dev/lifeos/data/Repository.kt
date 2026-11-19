package dev.lifeos.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.lifeos.db.Daily_plan
import dev.lifeos.db.Deadline
import dev.lifeos.db.Event
import dev.lifeos.db.Focus_session
import dev.lifeos.db.Habit
import dev.lifeos.db.Inbox_item
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.db.Milestone
import dev.lifeos.db.Note
import dev.lifeos.db.Project
import dev.lifeos.db.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import dev.lifeos.data.Dates.plusDays
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
    private val _localChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emite tras cada escritura local (la sincronización lo usa para subir cambios). */
    val localChanges: SharedFlow<Unit> = _localChanges

    private suspend fun <T> write(block: () -> T): T = withContext(io) { block() }.also { _localChanges.tryEmit(Unit) }

    // ---------- Lecturas ----------
    val projects: Flow<List<Project>> = db.projectQueries.selectAll().asFlow().mapToList(io)
    val milestones: Flow<List<Milestone>> = db.milestoneQueries.selectAll().asFlow().mapToList(io)
    val tasks: Flow<List<Task>> = db.taskQueries.selectAll().asFlow().mapToList(io)
    val habits: Flow<List<Habit>> = db.habitQueries.selectAll().asFlow().mapToList(io)
    val deadlines: Flow<List<Deadline>> = db.deadlineQueries.selectAll().asFlow().mapToList(io)
    val inbox: Flow<List<Inbox_item>> = db.inboxQueries.selectAll().asFlow().mapToList(io)
    val notes: Flow<List<Note>> = db.noteQueries.selectAll().asFlow().mapToList(io)
    val events: Flow<List<Event>> = db.eventQueries.selectAll().asFlow().mapToList(io)

    /** El bloque de enfoque en marcha (en cualquier dispositivo), o null. */
    val activeFocus: Flow<Focus_session?> = db.focusSessionQueries.selectActive().asFlow().mapToOneOrNull(io)

    /** Bloques que empezaron entre dos instantes (milisegundos). */
    fun focusBetween(from: Long, to: Long): Flow<List<Focus_session>> =
        db.focusSessionQueries.selectBetween(from, to).asFlow().mapToList(io)

    fun dailyPlans(from: LocalDate, to: LocalDate): Flow<List<Daily_plan>> =
        db.dailyPlanQueries.selectBetween(from.toString(), to.toString()).asFlow().mapToList(io)

    fun usageBetween(from: LocalDate, to: LocalDate): Flow<List<dev.lifeos.db.Usage_log>> =
        db.usageLogQueries.selectBetween(from.toString(), to.toString()).asFlow().mapToList(io)

    fun weekReview(weekStart: LocalDate): Flow<dev.lifeos.db.Week_review?> =
        db.weekReviewQueries.selectById(weekStart.toString()).asFlow().mapToOneOrNull(io)

    /** Preferencias compartidas (Ajustes): clave -> valor. */
    val prefs: Flow<Prefs> =
        db.prefQueries.selectAll().asFlow().mapToList(io).map { rows -> Prefs(rows.associate { it.id to it.value_ }) }

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

    /** Borra el proyecto con sus hitos, tareas, entregas y eventos. */
    suspend fun deleteProject(id: String) = write {
        db.transaction {
            val t = now()
            db.projectQueries.softDelete(t, id)
            db.milestoneQueries.softDeleteByProject(t, id)
            db.taskQueries.softDeleteByProject(t, id)
            db.deadlineQueries.softDeleteByProject(t, id)
            db.eventQueries.softDeleteByProject(t, id)
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

    /**
     * Cierre del día: lo elegido pasa al plan de mañana (y `first` será lo primero);
     * lo que quedó pendiente hoy y no se eligió sale del plan. El día queda cerrado.
     */
    suspend fun closeDay(today: LocalDate, tomorrow: LocalDate, chosen: List<String>, first: String?) = write {
        db.transaction {
            val t = now()
            db.taskQueries.selectAll().executeAsList()
                .filter { it.planned_date == today.toString() && it.done_at == null && it.id !in chosen }
                .forEach { db.taskQueries.setPlanned(null, t, it.id) }
            chosen.forEach { db.taskQueries.setPlanned(tomorrow.toString(), t, it) }
            db.dailyPlanQueries.ensure(tomorrow.toString(), t)
            if (first != null) db.dailyPlanQueries.setFirst(first, t, tomorrow.toString())
            db.dailyPlanQueries.ensure(today.toString(), t)
            db.dailyPlanQueries.setClosed(t, today.toString())
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

    // ---------- Calendario ----------
    suspend fun addEvent(
        title: String,
        kind: EventKind,
        date: LocalDate,
        startMin: Int,
        endMin: Int,
        weekly: Boolean = false,
        until: LocalDate? = null,
        projectId: String? = null,
        location: String? = null,
    ): String = write {
        require(endMin > startMin) { "El evento debe terminar después de empezar" }
        val id = newId()
        val t = now()
        db.eventQueries.insert(
            id, projectId, title.trim(), kind.name, date.toString(), startMin.toLong(), endMin.toLong(),
            if (weekly) 1L else 0L, until?.takeIf { weekly }?.toString(), location?.trim()?.ifEmpty { null }, t, t,
        )
        id
    }

    suspend fun updateEvent(
        id: String,
        title: String,
        kind: EventKind,
        date: LocalDate,
        startMin: Int,
        endMin: Int,
        weekly: Boolean,
        until: LocalDate?,
        projectId: String?,
        location: String?,
    ) = write {
        require(endMin > startMin) { "El evento debe terminar después de empezar" }
        db.eventQueries.update(
            projectId, title.trim(), kind.name, date.toString(), startMin.toLong(), endMin.toLong(),
            if (weekly) 1L else 0L, until?.takeIf { weekly }?.toString(), location?.trim()?.ifEmpty { null }, now(), id,
        )
    }

    suspend fun deleteEvent(id: String) = write { db.eventQueries.softDelete(now(), id) }

    /** Quita una sola semana de un evento semanal. */
    suspend fun skipOccurrence(id: String, date: LocalDate) = write {
        db.transaction {
            val e = db.eventQueries.selectById(id).executeAsOneOrNull() ?: return@transaction
            val dates = (e.skipped + date).sorted().joinToString(",")
            db.eventQueries.setSkipDates(dates, now(), id)
        }
    }

    /** Corta un evento semanal: desaparece desde `date` (incluida) en adelante. */
    suspend fun endSeriesFrom(id: String, date: LocalDate) = write {
        db.transaction {
            val e = db.eventQueries.selectById(id).executeAsOneOrNull() ?: return@transaction
            if (date <= e.startDate) db.eventQueries.softDelete(now(), id)
            else db.eventQueries.setUntil(date.plusDays(-1).toString(), now(), id)
        }
    }

    // ---------- Enfoque ----------
    /**
     * Empieza un bloque. Si ya hay uno en marcha, no crea otro y devuelve ese.
     * Para una tarea, el número de bloque cuenta los que ya hizo hoy.
     */
    suspend fun startFocus(title: String, taskId: String? = null, projectId: String? = null, todayStart: Long? = null): String = write {
        db.transactionWithResult {
            val t = now()
            db.focusSessionQueries.selectActive().executeAsOneOrNull()
                ?.takeIf { FocusMath.remaining(it, t) > 0 }?.let { return@transactionWithResult it.id }
            val task = taskId?.let { db.taskQueries.selectById(it).executeAsOneOrNull() }
            val doneToday = if (taskId != null && todayStart != null)
                db.focusSessionQueries.selectBetween(todayStart, t + 1).executeAsList().count { it.task_id == taskId && it.ended_at != null }
            else 0
            val index = doneToday + 1
            val total = maxOf(task?.estimate_blocks?.toInt() ?: 1, index)
            val id = newId()
            val prefs = Prefs(db.prefQueries.selectAll().executeAsList().associate { it.id to it.value_ })
            db.focusSessionQueries.insert(
                id, taskId, projectId ?: task?.project_id, title.trim(), t, prefs.blockMs,
                index.toLong(), total.toLong(), t, t,
            )
            id
        }
    }

    suspend fun pauseFocus(id: String) = write {
        db.transaction {
            val s = db.focusSessionQueries.selectActive().executeAsOneOrNull()?.takeIf { it.id == id } ?: return@transaction
            val t = now()
            if (s.paused_at == null && FocusMath.pauseLeft(s, t) > 0) db.focusSessionQueries.setPause(t, s.paused_ms, t, id)
        }
    }

    suspend fun resumeFocus(id: String) = write {
        db.transaction {
            val s = db.focusSessionQueries.selectActive().executeAsOneOrNull()?.takeIf { it.id == id } ?: return@transaction
            val t = now()
            if (s.paused_at != null) db.focusSessionQueries.setPause(null, FocusMath.totalPause(s, t), t, id)
        }
    }

    /** Cierra el bloque si ya cumplió su tiempo. Devuelve true si lo ha cerrado ahora. */
    suspend fun finishFocusIfDue(id: String): Boolean = write {
        db.transactionWithResult {
            val s = db.focusSessionQueries.selectActive().executeAsOneOrNull()?.takeIf { it.id == id } ?: return@transactionWithResult false
            val t = now()
            if (FocusMath.remaining(s, t) > 0) return@transactionWithResult false
            db.focusSessionQueries.finish(FocusMath.finishedAt(s, t), t, id)
            true
        }
    }

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

    // ---------- Notas ----------
    suspend fun addNote(projectId: String?, kind: NoteKind, title: String?, body: String, url: String?): String = write {
        val id = newId()
        val t = now()
        db.noteQueries.insertFull(id, projectId, kind.name, title?.trim()?.ifEmpty { null }, body.trim(), url?.trim()?.ifEmpty { null }, t, t)
        id
    }

    suspend fun updateNote(id: String, projectId: String?, kind: NoteKind, title: String?, body: String, url: String?) = write {
        db.noteQueries.update(projectId, kind.name, title?.trim()?.ifEmpty { null }, body.trim(), url?.trim()?.ifEmpty { null }, now(), id)
    }

    suspend fun deleteNote(id: String) = write { db.noteQueries.softDelete(now(), id) }

    /** Convierte un proyecto personal en uno con hitos (pasa a tipo TFG/proyecto con fecha). */
    suspend fun setProjectKind(id: String, kind: ProjectKind) = write {
        val p = db.projectQueries.selectById(id).executeAsOneOrNull() ?: return@write
        db.projectQueries.update(p.name, kind.name, p.description, p.due_date, now(), id)
    }

    // ---------- Uso del móvil ----------
    /** Guarda los minutos de scroll de un día (solo si cambian, para no sincronizar de más). */
    suspend fun putScroll(date: LocalDate, ms: Long) = write {
        val old = db.usageLogQueries.selectById(date.toString()).executeAsOneOrNull()
        if (old == null || kotlin.math.abs(old.scroll_ms - ms) >= 60_000) {
            val t = now()
            db.usageLogQueries.upsert(date.toString(), ms, old?.created_at ?: t, t)
        }
    }

    // ---------- Revisión semanal ----------
    suspend fun saveWeekReview(weekStart: LocalDate, note: String?, goals: List<String>) = write {
        db.transaction {
            val t = now()
            db.weekReviewQueries.ensure(weekStart.toString(), t)
            db.weekReviewQueries.save(note?.trim()?.ifEmpty { null }, goals.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"), t, weekStart.toString())
        }
    }

    suspend fun finishWeekReview(weekStart: LocalDate) = write {
        db.transaction {
            val t = now()
            db.weekReviewQueries.ensure(weekStart.toString(), t)
            db.weekReviewQueries.finish(t, weekStart.toString())
        }
    }

    // ---------- Preferencias compartidas ----------
    suspend fun putPref(key: String, value: String) = write {
        val t = now()
        db.prefQueries.put(key, value, t, t)
    }

    // ---------- Ajustes ----------
    suspend fun setting(key: String): String? = write { db.settingQueries.get(key).executeAsOneOrNull() }
    suspend fun putSetting(key: String, value: String) = write { db.settingQueries.put(key, value, now()) }
}
