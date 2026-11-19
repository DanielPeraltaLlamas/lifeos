package dev.lifeos.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.lifeos.data.Dates
import dev.lifeos.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/** Repinta los widgets cuando cambian los datos (aquí o por sincronización) y al cambiar de día. */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun startWidgetUpdates(context: Context, repo: Repository, scope: CoroutineScope) {
    scope.launch {
        Dates.todayFlow().flatMapLatest { today ->
            combine(
                repo.tasks, repo.habits, repo.dailyPlan(today), repo.activeFocus,
                repo.habitCounts(today, today),
            ) { _, _, _, _, _ -> today }
        }.debounce(800).collect {
            runCatching {
                TodayWidget().updateAll(context)
                HabitsWidget().updateAll(context)
            }
        }
    }
}
