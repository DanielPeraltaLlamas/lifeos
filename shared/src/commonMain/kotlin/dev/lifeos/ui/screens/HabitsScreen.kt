package dev.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.HABIT_HISTORY_DAYS
import dev.lifeos.data.HabitMath
import dev.lifeos.data.HabitPeriod
import dev.lifeos.data.periodEnum
import dev.lifeos.db.Habit
import dev.lifeos.ui.Card
import dev.lifeos.ui.ConfirmDialog
import dev.lifeos.ui.EmptyText
import dev.lifeos.ui.HabitDialog
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.MenuAction
import dev.lifeos.ui.OverflowMenu
import dev.lifeos.ui.ScreenHeader
import dev.lifeos.ui.rememberActions
import kotlinx.datetime.LocalDate

@Composable
fun HabitsScreen() {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val act = rememberActions()
    val habits by repo.habits.collectAsState(emptyList())
    val counts by remember(today) { repo.habitCounts(today.plusDays(-HABIT_HISTORY_DAYS), today) }.collectAsState(emptyMap())
    var editing by remember { mutableStateOf<Habit?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    Column {
        ScreenHeader("Hábitos", "Cada hábito con su propio ritmo. Un día de descanso del gimnasio no cuenta como fallo.") {
            Button(onClick = { creating = true }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nuevo hábito")
            }
        }
        if (habits.isEmpty()) EmptyText("Sin hábitos todavía.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            habits.forEach { h ->
                val c = counts[h.id] ?: emptyMap()
                Card(null, Modifier.fillMaxWidth(), padding = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(h.name, style = MaterialTheme.typography.titleMedium)
                            Text(habitSummary(h, c, today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OverflowMenu(listOf(
                            MenuAction("Editar") { editing = h },
                            MenuAction("Eliminar", destructive = true) { deleting = h },
                        ))
                    }
                    Spacer(Modifier.height(12.dp))
                    if (h.periodEnum == HabitPeriod.DAY) DayCells(h, c, today) else WeekCells(h, c, today)
                    Spacer(Modifier.height(8.dp))
                    HabitQuickRow(
                        h, HabitMath.current(h, c, today), c[today] ?: 0, HabitMath.streak(h, c, today),
                        onLog = { act { logHabit(h.id, today) } },
                        onUnlog = { act { unlogHabit(h.id, today) } },
                        showName = false,
                    )
                }
            }
        }
    }

    if (creating) HabitDialog(null) { creating = false }
    editing?.let { HabitDialog(it) { editing = null } }
    deleting?.let { h ->
        ConfirmDialog("Eliminar hábito", "«${h.name}» y su historial dejarán de aparecer.", "Eliminar", { act { deleteHabit(h.id) } }) { deleting = null }
    }
}

private fun habitSummary(h: Habit, c: Map<LocalDate, Int>, today: LocalDate): String {
    val base = HabitMath.describe(h)
    val streak = HabitMath.streak(h, c, today)
    return when (h.periodEnum) {
        HabitPeriod.DAY -> {
            val week = (0..6).map { c[today.plusDays(-it)] ?: 0 }
            val avg = week.sum() / 7.0
            if (h.target > 1) "$base · media ${formatDecimal(avg)} esta semana"
            else if (streak > 0) "$base · racha de $streak día${if (streak > 1) "s" else ""}"
            else base
        }
        HabitPeriod.WEEK -> if (streak > 0) "$base · $streak semana${if (streak > 1) "s" else ""} seguidas" else base
    }
}

private fun formatDecimal(v: Double): String {
    val r = kotlin.math.round(v * 10).toInt()
    return "${r / 10},${r % 10}"
}

@Composable
private fun DayCells(h: Habit, c: Map<LocalDate, Int>, today: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val target = h.target.toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (6 downTo 0).forEach { back ->
            val d = today.plusDays(-back)
            val n = c[d] ?: 0
            val ratio = (n.toFloat() / target).coerceAtMost(1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Dates.dayLetter(d), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
                        .background(if (n == 0) cs.surfaceContainerHighest else cs.tertiary.copy(alpha = 0.3f + 0.7f * ratio)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (n > 0) Text(
                        if (target == 1) "✓" else "$n",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ratio >= 1f) cs.onTertiary else cs.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCells(h: Habit, c: Map<LocalDate, Int>, today: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val target = h.target.toInt()
    val thisWeek = Dates.weekStart(today)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (3 downTo 0).forEach { back ->
            val ws = thisWeek.plusDays(-7 * back)
            val n = HabitMath.weekTotal(c, ws)
            val met = n >= target
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (back == 0) "Esta" else Dates.short(ws), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Box(
                    Modifier.size(width = 60.dp, height = 30.dp).clip(RoundedCornerShape(7.dp))
                        .background(if (met) cs.tertiary else if (n > 0) cs.tertiary.copy(alpha = 0.35f) else cs.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n/$target", style = MaterialTheme.typography.labelMedium, color = if (met) cs.onTertiary else cs.onSurface)
                }
            }
        }
    }
}

