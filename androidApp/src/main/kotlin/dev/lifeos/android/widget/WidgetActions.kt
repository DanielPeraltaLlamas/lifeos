package dev.lifeos.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.updateAll
import dev.lifeos.android.LifeOsApp
import dev.lifeos.data.Dates
import kotlinx.coroutines.flow.first
import kotlinx.datetime.atStartOfDayIn

private fun repo(context: Context) = (context.applicationContext as LifeOsApp).repository

/** Marcar o desmarcar una tarea desde el widget. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskKey] ?: return
        val done = parameters[ToggleableStateKey] ?: true
        repo(context).setTaskDone(id, done)
        TodayWidget().updateAll(context)
    }
}

/** Sumar un registro a un hábito hoy. */
class LogHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitKey] ?: return
        repo(context).logHabit(id, Dates.today())
        HabitsWidget().updateAll(context)
    }
}

/** Empezar un bloque de 50 min con «lo primero». */
class StartFocusAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskKey] ?: return
        val r = repo(context)
        val task = r.tasks.first().firstOrNull { it.id == id } ?: return
        r.startFocus(task.title, task.id, task.project_id, Dates.today().atStartOfDayIn(Dates.zone).toEpochMilliseconds())
        TodayWidget().updateAll(context)
    }
}
