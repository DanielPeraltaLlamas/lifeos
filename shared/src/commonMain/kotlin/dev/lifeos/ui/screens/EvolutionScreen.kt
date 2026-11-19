package dev.lifeos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.ProjectKind
import dev.lifeos.data.isDone
import dev.lifeos.data.kindEnum
import dev.lifeos.ui.Card
import dev.lifeos.ui.LocalPrefs
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.ScreenHeader
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val WEEKS = 8

private fun isoWeek(d: LocalDate): Int = (Dates.weekStart(d).plusDays(3).dayOfYear - 1) / 7 + 1

/** Una barra: etiqueta corta, valor y texto del valor. */
data class Bar(val label: String, val value: Float, val text: String)

@Composable
fun EvolutionScreen(wide: Boolean) {
    val repo = LocalRepository.current
    val today = LocalToday.current
    val prefs = LocalPrefs.current
    val thisWeek = Dates.weekStart(today)
    val firstWeek = thisWeek.plusDays(-7 * (WEEKS - 1))
    val fromMs = firstWeek.atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val toMs = today.plusDays(1).atStartOfDayIn(Dates.zone).toEpochMilliseconds()

    val focus by remember(fromMs) { repo.focusBetween(fromMs, toMs) }.collectAsState(emptyList())
    val usage by remember(firstWeek) { repo.usageBetween(firstWeek, today) }.collectAsState(emptyList())
    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())

    fun dateOf(ms: Long) = Instant.fromEpochMilliseconds(ms).toLocalDateTime(Dates.zone).date
    val done = focus.filter { it.ended_at != null }
    val weeks = (0 until WEEKS).map { firstWeek.plusDays(7 * it) }

    // Horas de foco por semana
    val focusBars = weeks.map { w ->
        val ms = done.filter { Dates.weekStart(dateOf(it.started_at)) == w }.sumOf { it.planned_ms }
        Bar("S${isoWeek(w)}", ms / 3_600_000f, durationText(ms))
    }
    val focusThisWeek = done.filter { Dates.weekStart(dateOf(it.started_at)) == thisWeek }.sumOf { it.planned_ms }

    // Scroll por semana
    val scrollBars = weeks.map { w ->
        val ms = usage.filter { Dates.weekStart(LocalDate.parse(it.id)) == w }.sumOf { it.scroll_ms }
        Bar("S${isoWeek(w)}", ms / 3_600_000f, durationText(ms))
    }
    val weeklyLimitH = prefs.scrollLimitMin * 7 / 60f

    // Primer bloque de cada día (últimos 14)
    val days14 = (13 downTo 0).map { today.plusDays(-it) }
    val firstBlocks = days14.map { d -> d to focus.filter { dateOf(it.started_at) == d }.minOfOrNull { it.started_at } }
    val observed = firstBlocks.mapNotNull { it.second }
    val avgMinute = observed.takeIf { it.isNotEmpty() }?.map {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(Dates.zone).let { t -> t.hour * 60 + t.minute }
    }?.average()?.toInt()

    // Plan cumplido (últimos 14 días): tareas que siguen planificadas ese día
    val planDays = days14.map { d ->
        val p = tasks.filter { it.planned_date == d.toString() }
        d to (p.count { it.isDone } to p.size)
    }
    val planTotal = planDays.sumOf { it.second.second }
    val planDone = planDays.sumOf { it.second.first }

    // TFG
    val tfg = projects.firstOrNull { it.kindEnum == ProjectKind.TFG }
    val tfgMilestones = milestones.filter { it.project_id == tfg?.id }.sortedBy { it.due_date }
    val currentMs = tfgMilestones.firstOrNull { LocalDate.parse(it.due_date) >= today }
    val msTasks = tasks.filter { it.milestone_id == currentMs?.id }
    val tfgFocus = done.filter { it.project_id == tfg?.id }.sumOf { it.planned_ms }

    Column {
        ScreenHeader("Evolución", "Datos para entenderte, no para juzgarte · últimas $WEEKS semanas")

        val focusCard: @Composable (Modifier) -> Unit = { m ->
            Card("Horas de foco por semana", m) {
                Text("${durationText(focusThisWeek)} esta semana", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                BarChart(focusBars, unit = "h")
            }
        }
        val scrollCard: @Composable (Modifier) -> Unit = { m ->
            Card("Scroll por semana · TikTok e Instagram", m) {
                val now = scrollBars.last()
                Text(
                    if (usage.isEmpty()) "Sin datos todavía: los manda el móvil cuando tiene el acceso al uso."
                    else "${now.text} esta semana · límite ${durationText((weeklyLimitH * 3_600_000).toLong())}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                BarChart(scrollBars, unit = "h", reference = weeklyLimitH, referenceLabel = "límite")
            }
        }
        val firstCard: @Composable (Modifier) -> Unit = { m ->
            Card("Hora del primer bloque", m) {
                Text(
                    if (avgMinute == null) "Aún no hay bloques en estos 14 días."
                    else "${Dates.time(today.atStartOfDayIn(Dates.zone).toEpochMilliseconds() + avgMinute * 60_000L)} de media · ${observed.size} de 14 días con bloque",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FirstBlockChart(firstBlocks)
            }
        }
        val planCard: @Composable (Modifier) -> Unit = { m ->
            Card("Plan cumplido", m) {
                Text(
                    if (planTotal == 0) "Sin tareas planificadas en estos 14 días."
                    else "${planDone * 100 / planTotal} % de las tareas planificadas · últimos 14 días",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                PlanStrip(planDays)
            }
        }
        val tfgCard: @Composable (Modifier) -> Unit = { m ->
            Card("TFG · camino hacia el ${tfg?.due_date?.let { Dates.medium(LocalDate.parse(it)).substringAfter(' ') } ?: "final"}", m) {
                if (tfg == null) {
                    Text("No hay ningún proyecto de tipo TFG.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val idx = tfgMilestones.indexOf(currentMs)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat(if (currentMs != null) "H${idx + 1} de ${tfgMilestones.size}" else "—", "Hito en curso")
                        Stat(durationText(tfgFocus), "Foco en el TFG ($WEEKS sem.)")
                        tfg.due_date?.let { Stat("${today.daysUntil(LocalDate.parse(it))} d", "Hasta la entrega") }
                    }
                    if (currentMs != null) {
                        Spacer(Modifier.height(14.dp))
                        val doneN = msTasks.count { it.isDone }
                        Text(
                            "${currentMs.title} · vence ${Dates.short(LocalDate.parse(currentMs.due_date))} · ${doneN} de ${msTasks.size} tareas hechas",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { if (msTasks.isEmpty()) 0f else doneN.toFloat() / msTasks.size },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        val gap = 14.dp
        if (wide) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) { focusCard(Modifier.weight(1f)); scrollCard(Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) { firstCard(Modifier.weight(1f)); planCard(Modifier.weight(1f)) }
                tfgCard(Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                focusCard(Modifier.fillMaxWidth()); scrollCard(Modifier.fillMaxWidth())
                firstCard(Modifier.fillMaxWidth()); planCard(Modifier.fillMaxWidth()); tfgCard(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Barras verticales de una sola serie. Valor escrito en la última y en la más alta;
 * al pasar el ratón o tocar una barra, su valor aparece arriba.
 */
@Composable
fun BarChart(bars: List<Bar>, unit: String, height: Dp = 150.dp, reference: Float? = null, referenceLabel: String? = null) {
    val cs = MaterialTheme.colorScheme
    var hover by remember { mutableStateOf<Int?>(null) }
    val maxV = maxOf(bars.maxOfOrNull { it.value } ?: 0f, reference ?: 0f, 0.0001f)
    val maxIdx = bars.indices.maxByOrNull { bars[it].value }
    val shown = hover ?: bars.lastIndex
    Column {
        Text(
            bars.getOrNull(shown)?.let { "${it.label}: ${it.text}" } ?: "",
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
            val n = bars.size.coerceAtLeast(1)
            Canvas(
                Modifier.fillMaxWidth().height(height).pointerInput(bars) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            val x = e.changes.firstOrNull()?.position?.x ?: continue
                            hover = when (e.type) {
                                PointerEventType.Exit -> null
                                else -> (x / (size.width / n)).toInt().coerceIn(0, n - 1)
                            }
                        }
                    }
                },
            ) {
                val slot = size.width / n
                val barW = minOf(slot * 0.56f, 28.dp.toPx())
                val baseY = size.height
                // Línea base y, si la hay, línea de referencia (discontinua).
                drawLine(cs.outlineVariant, Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
                if (reference != null && reference > 0f) {
                    val y = baseY - (reference / maxV) * (size.height - 4.dp.toPx())
                    drawLine(cs.outline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                }
                bars.forEachIndexed { i, b ->
                    if (b.value <= 0f) return@forEachIndexed
                    val h = (b.value / maxV) * (size.height - 4.dp.toPx())
                    val x = slot * i + (slot - barW) / 2
                    val r = minOf(4.dp.toPx(), barW / 2, h)
                    val color = if (i == shown) cs.primary else cs.primary.copy(alpha = 0.55f)
                    // Esquinas redondeadas solo arriba: la base apoya en el eje.
                    val path = Path().apply {
                        moveTo(x, baseY)
                        lineTo(x, baseY - h + r)
                        quadraticTo(x, baseY - h, x + r, baseY - h)
                        lineTo(x + barW - r, baseY - h)
                        quadraticTo(x + barW, baseY - h, x + barW, baseY - h + r)
                        lineTo(x + barW, baseY)
                        close()
                    }
                    drawPath(path, color)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { i, b ->
                Text(
                    b.label + if (i == maxIdx && i != bars.lastIndex && b.value > 0) "\n${b.text}" else "",
                    style = MaterialTheme.typography.labelSmall, color = if (i == shown) cs.onSurface else cs.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        if (reference != null && referenceLabel != null) {
            Text("— — $referenceLabel", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Puntos: a qué hora empezó el primer bloque cada día (eje vertical 8:00–22:00). */
@Composable
private fun FirstBlockChart(days: List<Pair<LocalDate, Long?>>) {
    val cs = MaterialTheme.colorScheme
    val fromMin = 8 * 60f
    val toMin = 22 * 60f
    Column {
        Row {
            Column(Modifier.height(120.dp).width(36.dp), verticalArrangement = Arrangement.SpaceBetween) {
                listOf("8:00", "15:00", "22:00").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant) }
            }
            Canvas(Modifier.weight(1f).height(120.dp)) {
                val slot = size.width / days.size
                listOf(0f, 0.5f, 1f).forEach { f ->
                    drawLine(cs.outlineVariant.copy(alpha = 0.5f), Offset(0f, size.height * f), Offset(size.width, size.height * f), 1f)
                }
                days.forEachIndexed { i, (_, ms) ->
                    val cx = slot * i + slot / 2
                    if (ms == null) {
                        drawCircle(cs.outlineVariant, 2.dp.toPx(), Offset(cx, size.height))
                    } else {
                        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(Dates.zone)
                        val m = (t.hour * 60 + t.minute).toFloat().coerceIn(fromMin, toMin)
                        val y = (m - fromMin) / (toMin - fromMin) * size.height
                        drawCircle(cs.surfaceContainer, 6.dp.toPx(), Offset(cx, y))
                        drawCircle(cs.primary, 4.5.dp.toPx(), Offset(cx, y))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp)) {
            days.forEach { (d, _) ->
                Text(Dates.dayLetter(d), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Una celda por día: completo, parcial o sin cumplir (con leyenda). */
@Composable
private fun PlanStrip(days: List<Pair<LocalDate, Pair<Int, Int>>>) {
    val cs = MaterialTheme.colorScheme
    val full = cs.primary
    val partial = cs.primary.copy(alpha = 0.45f)
    val none = cs.surfaceContainerHighest
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.forEach { (d, p) ->
                val (done, total) = p
                val c = when {
                    total == 0 -> Color.Transparent
                    done == total -> full
                    done > 0 -> partial
                    else -> none
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = c, shape = RoundedCornerShape(4.dp),
                        border = if (total == 0) androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant) else null,
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                    ) {}
                    Text(Dates.dayLetter(d), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(full to "Completo", partial to "Parcial", none to "Sin cumplir").forEach { (c, l) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = c, shape = RoundedCornerShape(3.dp), modifier = Modifier.size(10.dp)) {}
                    Spacer(Modifier.width(5.dp))
                    Text(l, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}
