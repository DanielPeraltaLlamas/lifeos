package dev.lifeos.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import dev.lifeos.android.LifeOsApp
import dev.lifeos.android.MainActivity
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.isDone
import dev.lifeos.data.periodEnum
import kotlinx.coroutines.flow.first

// ------------------------------------------------------------------ piezas comunes

@Composable
private fun WidgetFrame(content: @Composable () -> Unit) {
    Box(
        GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(14.dp),
    ) { content() }
}

@Composable
private fun Header(title: String, right: String? = null) {
    Row(
        GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        if (right != null) Text(right, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
    }
}

@Composable
private fun Pill(text: String, onClick: androidx.glance.action.Action, filled: Boolean = true) {
    Box(
        GlanceModifier
            .background(if (filled) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(
                color = if (filled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private fun app(context: Context) = context.applicationContext as LifeOsApp

// ------------------------------------------------------------------ Hoy

private data class TodayRow(val id: String, val title: String, val done: Boolean, val first: Boolean)

class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = app(context).repository
        val today = Dates.today()
        val tasks = repo.tasks.first()
        val plan = repo.dailyPlan(today).first()
        val firstId = plan?.first_task_id
        val rows = tasks.filter { it.planned_date == today.toString() || it.id == firstId }
            .map { TodayRow(it.id, it.title, it.isDone, it.id == firstId) }
            .sortedWith(compareBy({ !it.first }, { it.done }))
        val focus = repo.activeFocus.first()
        provideContent { GlanceTheme { TodayContent(rows, focus?.title) } }
    }
}

@Composable
private fun TodayContent(rows: List<TodayRow>, focusTitle: String?) {
    WidgetFrame {
        Column(GlanceModifier.fillMaxSize()) {
            val done = rows.count { it.done }
            Header("Hoy", if (rows.isNotEmpty()) "$done de ${rows.size}" else null)
            Spacer(GlanceModifier.height(8.dp))
            val first = rows.firstOrNull { it.first && !it.done }
            when {
                focusTitle != null -> {
                    Text("En un bloque de enfoque", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
                    Text(focusTitle, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                    Spacer(GlanceModifier.height(8.dp))
                }
                first != null -> {
                    Text("Lo primero", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
                    Text(first.title, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                    Spacer(GlanceModifier.height(6.dp))
                    Pill("▶  Empezar · 50 min", actionRunCallback<StartFocusAction>(actionParametersOf(TaskKey to first.id)))
                    Spacer(GlanceModifier.height(8.dp))
                }
            }
            if (rows.isEmpty()) {
                Text(
                    "Nada en el plan. Toca para elegir lo primero.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                )
            } else {
                LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(rows.filter { !(it.first && !it.done && focusTitle == null) }, itemId = { it.id.hashCode().toLong() }) { r ->
                        CheckBox(
                            checked = r.done,
                            onCheckedChange = actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskKey to r.id)),
                            text = r.title,
                            style = TextStyle(
                                color = if (r.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                textDecoration = if (r.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// ------------------------------------------------------------------ Hábitos

private data class HabitRow(val id: String, val name: String, val progress: String, val met: Boolean)

class HabitsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = app(context).repository
        val today = Dates.today()
        val habits = repo.habits.first()
        val counts = repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today).first()
        val rows = habits.map { h ->
            val c = counts[h.id] ?: emptyMap()
            val cur = HabitMath.current(h, c, today)
            val target = h.target.toInt()
            val unit = if (h.periodEnum == HabitPeriod.WEEK) " sem." else ""
            HabitRow(h.id, h.name, "$cur/$target$unit", cur >= target)
        }
        provideContent { GlanceTheme { HabitsContent(rows) } }
    }
}

@Composable
private fun HabitsContent(rows: List<HabitRow>) {
    WidgetFrame {
        Column(GlanceModifier.fillMaxSize()) {
            Header("Hábitos", "${rows.count { it.met }}/${rows.size}")
            Spacer(GlanceModifier.height(6.dp))
            LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(rows, itemId = { it.id.hashCode().toLong() }) { r ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(r.name, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp), maxLines = 1)
                            Text(
                                r.progress,
                                style = TextStyle(
                                    color = if (r.met) GlanceTheme.colors.tertiary else GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                        Box(
                            GlanceModifier.size(36.dp).cornerRadius(18.dp)
                                .background(if (r.met) GlanceTheme.colors.tertiary else GlanceTheme.colors.tertiaryContainer)
                                .clickable(actionRunCallback<LogHabitAction>(actionParametersOf(HabitKey to r.id))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (r.met) "✓" else "+",
                                style = TextStyle(
                                    color = if (r.met) GlanceTheme.colors.onTertiary else GlanceTheme.colors.onTertiaryContainer,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

// ------------------------------------------------------------------ Captura rápida

class CaptureWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { CaptureContent() } }
    }
}

@Composable
private fun CaptureContent() {
    WidgetFrame {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            Pill("+ Nota", actionStartActivity<QuickCaptureActivity>(actionParametersOf(KindKey to QuickCaptureActivity.NOTE)), filled = false)
            Spacer(GlanceModifier.width(10.dp))
            Pill("+ Tarea", actionStartActivity<QuickCaptureActivity>(actionParametersOf(KindKey to QuickCaptureActivity.TASK)))
        }
    }
}

class CaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaptureWidget()
}

internal val TaskKey = ActionParameters.Key<String>("task")
internal val HabitKey = ActionParameters.Key<String>("habit")
internal val KindKey = ActionParameters.Key<String>(QuickCaptureActivity.EXTRA_KIND)
