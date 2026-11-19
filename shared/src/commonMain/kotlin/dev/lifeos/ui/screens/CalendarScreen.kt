package dev.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lifeos.data.CalendarMath
import dev.lifeos.data.CalendarMath.hm
import dev.lifeos.data.Dates
import dev.lifeos.data.Dates.plusDays
import dev.lifeos.data.DayItem
import dev.lifeos.data.EventKind
import dev.lifeos.data.Occurrence
import dev.lifeos.data.dueDate
import dev.lifeos.data.kindEnum
import dev.lifeos.db.Project
import dev.lifeos.ui.Card
import dev.lifeos.ui.DeadlineDialog
import dev.lifeos.ui.Dot
import dev.lifeos.ui.EmptyText
import dev.lifeos.ui.EventDialog
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.MilestoneDialog
import dev.lifeos.ui.TaskDialog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class CalView(val label: String) { DAY("Día"), WEEK("Semana"), MONTH("Mes") }

private sealed interface CalDialog {
    data class NewEvent(val date: LocalDate, val start: Int) : CalDialog
    data class EditEvent(val occ: Occurrence) : CalDialog
    data class Item(val item: DayItem) : CalDialog
    data class NewDeadline(val date: LocalDate) : CalDialog
}

private fun firstOfMonth(d: LocalDate) = LocalDate(d.year, d.month.number, 1)

/** Semana ISO (la que contiene el jueves). */
private fun isoWeek(d: LocalDate): Int = (Dates.weekStart(d).plusDays(3).dayOfYear - 1) / 7 + 1

private fun monthTitle(d: LocalDate) = Dates.monthName(d).replaceFirstChar { it.uppercase() } + " ${d.year}"

/** Las 6 semanas (42 días) que se ven en un mes, empezando en lunes. */
private fun monthGrid(month: LocalDate): List<LocalDate> {
    val start = Dates.weekStart(firstOfMonth(month))
    return (0 until 42).map { start.plusDays(it) }
}

@Composable
fun CalendarScreen(wide: Boolean) {
    val repo = LocalRepository.current
    val today = LocalToday.current

    val events by repo.events.collectAsState(emptyList())
    val deadlines by repo.deadlines.collectAsState(emptyList())
    val milestones by repo.milestones.collectAsState(emptyList())
    val tasks by repo.tasks.collectAsState(emptyList())
    val projects by repo.projects.collectAsState(emptyList())
    val projectById = projects.associateBy { it.id }

    var view by rememberSaveable { mutableStateOf(if (wide) CalView.WEEK else CalView.DAY) }
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    val anchor = LocalDate.parse(anchorText)
    fun go(d: LocalDate) { anchorText = d.toString() }
    var dialog by remember { mutableStateOf<CalDialog?>(null) }

    val (from, to) = when (view) {
        CalView.DAY -> anchor to anchor
        CalView.WEEK -> Dates.weekStart(anchor).let { it to it.plusDays(6) }
        CalView.MONTH -> monthGrid(anchor).let { it.first() to it.last() }
    }
    val fromMs = from.atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val toMs = to.plusDays(1).atStartOfDayIn(Dates.zone).toEpochMilliseconds()
    val focus by remember(fromMs, toMs) { repo.focusBetween(fromMs, toMs) }.collectAsState(emptyList())
    val occs = CalendarMath.occurrences(events + CalendarMath.focusAsEvents(focus, Dates.nowMs()), from, to)
    val items = CalendarMath.dayItems(deadlines, milestones, tasks, projectById, from, to, includePlanned = view == CalView.DAY)

    fun step(sign: Int) = go(
        when (view) {
            CalView.DAY -> anchor.plusDays(sign)
            CalView.WEEK -> anchor.plusDays(7 * sign)
            CalView.MONTH -> firstOfMonth(anchor).plus(sign, DateTimeUnit.MONTH)
        },
    )

    val title = when (view) {
        CalView.DAY -> Dates.long(anchor)
        CalView.WEEK -> {
            val s = from; val e = to
            if (s.month == e.month) monthTitle(s)
            else "${Dates.monthName(s).take(3).replaceFirstChar { it.uppercase() }} – ${Dates.monthName(e).take(3)} ${e.year}"
        }
        CalView.MONTH -> monthTitle(anchor)
    }
    val subtitle = when (view) {
        CalView.DAY -> {
            val n = occs.size
            val days = today.daysUntil(anchor)
            (if (days > 1) "En $days días" else Dates.relative(days)) + " · " + if (n == 0) "sin eventos" else "$n evento${if (n > 1) "s" else ""}"
        }
        CalView.WEEK -> "Semana ${isoWeek(from)} · ${from.day} – ${Dates.short(to)}"
        CalView.MONTH -> null
    }

    Column {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (wide) {
                Button(onClick = { dialog = CalDialog.NewEvent(if (view == CalView.DAY) anchor else today, 9 * 60) }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nuevo evento")
                }
            } else {
                FilledTonalIconButton(onClick = { dialog = CalDialog.NewEvent(if (view == CalView.DAY) anchor else today, 9 * 60) }) {
                    Icon(Icons.Outlined.Add, "Nuevo evento")
                }
            }
        }
        // Controles
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Anterior") }
                OutlinedButton(onClick = { go(today) }) { Text("Hoy") }
                IconButton(onClick = { step(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Siguiente") }
            }
            SingleChoiceSegmentedButtonRow {
                CalView.entries.forEachIndexed { i, v ->
                    SegmentedButton(
                        selected = view == v,
                        onClick = { view = v },
                        shape = SegmentedButtonDefaults.itemShape(i, CalView.entries.size),
                        icon = {},
                    ) { Text(v.label) }
                }
            }
            if (wide) Legend()
        }

        val main: @Composable (Modifier) -> Unit = { m ->
            Card(null, m, padding = if (wide) 12.dp else 8.dp) {
                when (view) {
                    CalView.DAY, CalView.WEEK -> TimeGrid(
                        days = (0..from.daysUntil(to)).map { from.plusDays(it) },
                        occs = occs,
                        items = items,
                        projects = projectById,
                        compact = !wide && view == CalView.WEEK,
                        onSlot = { d, min -> dialog = CalDialog.NewEvent(d, min) },
                        onEvent = { if (!it.event.id.startsWith(CalendarMath.FOCUS_PREFIX)) dialog = CalDialog.EditEvent(it) },
                        onItem = { dialog = CalDialog.Item(it) },
                        onDay = { go(it); view = CalView.DAY },
                    )
                    CalView.MONTH -> MonthGrid(
                        month = anchor,
                        occs = occs,
                        items = items,
                        compact = !wide,
                        onDay = { go(it); view = CalView.DAY },
                    )
                }
            }
        }

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                main(Modifier.weight(1f))
                Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MiniMonthCard(anchor, deadlines.map { it.dueDate }, milestones.map { LocalDate.parse(it.due_date) }, onPick = ::go)
                    UpcomingDatesCard(today, deadlines, milestones, tasks, projectById,
                        onItem = { dialog = CalDialog.Item(it) }, onAdd = { dialog = CalDialog.NewDeadline(today.plusDays(7)) })
                }
            }
        } else {
            main(Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Legend()
            Spacer(Modifier.height(14.dp))
            UpcomingDatesCard(today, deadlines, milestones, tasks, projectById,
                onItem = { dialog = CalDialog.Item(it) }, onAdd = { dialog = CalDialog.NewDeadline(today.plusDays(7)) })
        }
    }

    when (val d = dialog) {
        is CalDialog.NewEvent -> EventDialog(null, projects, d.date, d.start) { dialog = null }
        is CalDialog.EditEvent -> EventDialog(d.occ.event, projects, d.occ.date, occurrence = d.occ.date) { dialog = null }
        is CalDialog.NewDeadline -> DeadlineDialog(projects, null, initialDate = d.date) { dialog = null }
        is CalDialog.Item -> when (d.item.kind) {
            DayItem.Kind.DELIVERY, DayItem.Kind.EXAM -> deadlines.firstOrNull { it.id == d.item.id }
                ?.let { DeadlineDialog(projects, null, deadline = it) { dialog = null } } ?: run { dialog = null }
            DayItem.Kind.MILESTONE -> milestones.firstOrNull { it.id == d.item.id }
                ?.let { MilestoneDialog(it.project_id, it) { dialog = null } } ?: run { dialog = null }
            DayItem.Kind.TASK_DUE, DayItem.Kind.PLANNED -> tasks.firstOrNull { it.id == d.item.id }
                ?.let { TaskDialog(it, projects, milestones, onDismiss = { dialog = null }) } ?: run { dialog = null }
        }
        null -> {}
    }
}

// ---------------------------------------------------------------- colores

private data class Tone(val container: Color, val content: Color, val dashed: Boolean = false)

@Composable
private fun eventTone(kind: EventKind): Tone {
    val cs = MaterialTheme.colorScheme
    return when (kind) {
        EventKind.CLASS -> Tone(cs.surfaceContainerHighest, cs.onSurface)
        EventKind.STUDY -> Tone(cs.primaryContainer, cs.onPrimaryContainer, dashed = true)
        EventKind.GYM -> Tone(cs.tertiaryContainer, cs.onTertiaryContainer)
        EventKind.OTHER -> Tone(cs.secondaryContainer, cs.onSecondaryContainer)
        EventKind.FOCUS -> Tone(cs.primary, cs.onPrimary)
    }
}

@Composable
private fun itemTone(kind: DayItem.Kind): Tone {
    val cs = MaterialTheme.colorScheme
    return when (kind) {
        DayItem.Kind.DELIVERY, DayItem.Kind.EXAM -> Tone(cs.errorContainer, cs.onErrorContainer)
        DayItem.Kind.MILESTONE -> Tone(cs.primary, cs.onPrimary)
        DayItem.Kind.TASK_DUE -> Tone(cs.surfaceContainerHigh, cs.onSurface)
        DayItem.Kind.PLANNED -> Tone(cs.surfaceContainerLow, cs.onSurfaceVariant, dashed = true)
    }
}

@Composable
private fun strongColor(kind: EventKind): Color {
    val cs = MaterialTheme.colorScheme
    return when (kind) {
        EventKind.CLASS -> cs.outline
        EventKind.STUDY -> cs.primary
        EventKind.GYM -> cs.tertiary
        EventKind.OTHER -> cs.secondary
        EventKind.FOCUS -> cs.primary
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp) = drawWithContent {
    drawContent()
    val w = 1.2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = size.copy(width = size.width - w, height = size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

@Composable
private fun Legend() {
    val cs = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        @Composable
        fun entry(color: Color, text: String) = Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, 8.dp); Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        entry(strongColor(EventKind.CLASS), "Clases")
        entry(strongColor(EventKind.STUDY), "Estudio")
        entry(strongColor(EventKind.GYM), "Gimnasio")
        entry(strongColor(EventKind.OTHER), "Otros")
        entry(strongColor(EventKind.FOCUS), "Foco hecho")
        entry(cs.error, "Entregas y exámenes")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = cs.primary); Spacer(Modifier.width(4.dp))
            Text("Hitos", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- día y semana

private val HOUR = 52.dp
private val GUTTER = 44.dp

@Composable
private fun TimeGrid(
    days: List<LocalDate>,
    occs: List<Occurrence>,
    items: List<DayItem>,
    projects: Map<String, Project>,
    compact: Boolean,
    onSlot: (LocalDate, Int) -> Unit,
    onEvent: (Occurrence) -> Unit,
    onItem: (DayItem) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val today = LocalToday.current
    val single = days.size == 1
    val startHour = minOf(8, occs.minOfOrNull { it.start / 60 } ?: 8)
    val endHour = maxOf(22, occs.maxOfOrNull { (it.end + 59) / 60 } ?: 22).coerceAtMost(24)
    val byDay = occs.groupBy { it.date }
    val itemsByDay = items.groupBy { it.date }
    val gutter = if (compact) 32.dp else GUTTER
    val lineColor = cs.outlineVariant.copy(alpha = 0.5f)

    Column {
        // Cabecera de días
        if (!single) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(gutter))
                days.forEach { d ->
                    val isToday = d == today
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onDay(d) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (compact) Dates.dayLetter(d) else Dates.dayName(d).take(3).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) cs.primary else cs.onSurfaceVariant,
                        )
                        Box(
                            Modifier.size(if (compact) 26.dp else 32.dp).clip(CircleShape)
                                .background(if (isToday) cs.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${d.day}",
                                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isToday) cs.onPrimary else cs.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Fila "todo el día"
        if (items.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(Modifier.width(gutter).padding(top = 4.dp, end = 6.dp), contentAlignment = Alignment.TopEnd) {
                    if (!compact) Text("todo\nel día", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, lineHeight = MaterialTheme.typography.labelSmall.lineHeight)
                }
                days.forEach { d ->
                    Column(Modifier.weight(1f).padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        itemsByDay[d].orEmpty().forEach { item -> ItemChip(item, compact, onClick = { onItem(item) }) }
                    }
                }
            }
        }
        HorizontalDivider(color = cs.outlineVariant)

        // Rejilla horaria
        val hours = endHour - startHour
        val density = LocalDensity.current
        val now = Dates.minuteNow()
        Row(Modifier.fillMaxWidth().height(HOUR * hours).padding(top = 8.dp)) {
            Box(Modifier.width(gutter).fillMaxHeight()) {
                (startHour until endHour).forEach { h ->
                    Text(
                        if (compact) "$h" else "$h:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.offset(y = HOUR * (h - startHour) - 7.dp).padding(end = 6.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
            days.forEach { d ->
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxHeight()
                        .drawBehind {
                            val hourPx = HOUR.toPx()
                            drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), 1f)
                            for (i in 0..hours) drawLine(lineColor, Offset(0f, i * hourPx), Offset(size.width, i * hourPx), 1f)
                        }
                        .pointerInput(d, startHour) {
                            detectTapGestures { pos ->
                                val hourPx = with(density) { HOUR.toPx() }
                                val min = startHour * 60 + (pos.y / hourPx * 60).toInt()
                                onSlot(d, (min / 30 * 30).coerceIn(0, 23 * 60))
                            }
                        },
                ) {
                    val colWidth = maxWidth
                    CalendarMath.layoutDay(byDay[d].orEmpty()).forEach { p ->
                        val top = HOUR * ((p.occ.start - startHour * 60) / 60f)
                        val height = (HOUR * ((p.occ.end - p.occ.start) / 60f)).coerceAtLeast(20.dp)
                        val w = colWidth / p.columns
                        EventBlock(
                            p.occ, projects, compact, narrow = !single || p.columns > 1,
                            modifier = Modifier
                                .offset(x = w * p.column, y = top)
                                .width(w)
                                .height(height)
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                            onClick = { onEvent(p.occ) },
                        )
                    }
                    if (d == today && now in startHour * 60 until endHour * 60) {
                        val y = HOUR * ((now - startHour * 60) / 60f)
                        Box(Modifier.offset(y = y - 1.dp).fillMaxWidth().height(2.dp).background(cs.error))
                        Box(Modifier.offset(x = (-4).dp, y = y - 5.dp).size(10.dp).clip(CircleShape).background(cs.error))
                    }
                }
            }
        }
        if (occs.isEmpty() && single) {
            EmptyText("Nada este día. Toca una hora para añadir un evento.", Modifier.padding(start = gutter, top = 8.dp))
        }
    }
}

@Composable
private fun EventBlock(occ: Occurrence, projects: Map<String, Project>, compact: Boolean, narrow: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val e = occ.event
    val tone = eventTone(e.kindEnum)
    val shape = RoundedCornerShape(7.dp)
    // Box y no Surface(onClick): Surface impone 48 dp de alto mínimo y deformaría los eventos cortos.
    Box(
        modifier.clip(shape).background(tone.container).clickable(onClick = onClick)
            .then(if (tone.dashed) Modifier.dashedBorder(tone.content.copy(alpha = 0.6f), 7.dp) else Modifier),
    ) {
        Column(Modifier.padding(horizontal = if (compact) 3.dp else if (narrow) 5.dp else 7.dp, vertical = 3.dp)) {
            val long = occ.end - occ.start >= 90
            Text(
                e.title,
                style = if (narrow) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tone.content,
                maxLines = if (long) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
            )
            if (!compact && occ.end - occ.start >= 45) {
                val project = e.project_id?.let(projects::get)?.name?.takeIf { it != e.title }
                val time = if (narrow) "${hm(occ.start)}–${hm(occ.end)}" else "${hm(occ.start)} – ${hm(occ.end)}"
                Text(
                    if (narrow) time else listOfNotNull(time, e.location, project).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = tone.content.copy(alpha = 0.8f),
                )
                val location = e.location
                if (narrow && long && location != null) {
                    Text(location, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tone.content.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun ItemChip(item: DayItem, compact: Boolean, onClick: () -> Unit) {
    val tone = itemTone(item.kind)
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).background(tone.container).clickable(onClick = onClick)
            .then(if (tone.dashed) Modifier.dashedBorder(MaterialTheme.colorScheme.outline, 6.dp) else Modifier),
    ) {
        val contentColor = tone.content
        Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.kind == DayItem.Kind.MILESTONE && !compact) {
                Icon(Icons.Outlined.Flag, null, Modifier.size(12.dp), tint = contentColor); Spacer(Modifier.width(3.dp))
            }
            val prefix = if (item.kind == DayItem.Kind.EXAM && !compact) "Examen · " else ""
            Text(
                prefix + item.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            )
        }
    }
}

// ---------------------------------------------------------------- mes

@Composable
private fun MonthGrid(
    month: LocalDate,
    occs: List<Occurrence>,
    items: List<DayItem>,
    compact: Boolean,
    onDay: (LocalDate) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val today = LocalToday.current
    val days = monthGrid(month)
    val byDay = occs.groupBy { it.date }
    val itemsByDay = items.groupBy { it.date }
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
                Text(
                    if (compact) l else listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")[i],
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    val inMonth = d.month == month.month
                    val dayItems = itemsByDay[d].orEmpty()
                    val dayOccs = byDay[d].orEmpty()
                    Column(
                        Modifier.weight(1f).heightIn(min = if (compact) 58.dp else 96.dp)
                            .padding(1.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (inMonth) cs.surfaceContainerLow else Color.Transparent)
                            .clickable { onDay(d) }.padding(4.dp),
                    ) {
                        val isToday = d == today
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(if (isToday) cs.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${d.day}",
                                style = MaterialTheme.typography.labelLarge,
                                color = when { isToday -> cs.onPrimary; inMonth -> cs.onSurface; else -> cs.onSurfaceVariant.copy(alpha = 0.5f) },
                            )
                        }
                        if (compact) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp, start = 2.dp)) {
                                dayItems.take(3).forEach { Dot(if (it.kind == DayItem.Kind.MILESTONE) cs.primary else cs.error, 6.dp) }
                                dayOccs.map { it.event.kindEnum }.distinct().take(3).forEach { Dot(strongColor(it), 6.dp) }
                            }
                        } else {
                            val lines = dayItems.map { it.title to itemTone(it.kind) } +
                                dayOccs.map { "${hm(it.start)} ${it.event.title}" to eventTone(it.event.kindEnum) }
                            lines.take(3).forEach { (text, tone) ->
                                Text(
                                    text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tone.content,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp)).background(tone.container).padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                            if (lines.size > 3) {
                                Text("+${lines.size - 3} más", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- panel lateral

@Composable
private fun MiniMonthCard(anchor: LocalDate, deadlineDates: List<LocalDate>, milestoneDates: List<LocalDate>, onPick: (LocalDate) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val today = LocalToday.current
    var shownText by remember(anchor.year, anchor.month) { mutableStateOf(firstOfMonth(anchor).toString()) }
    val shown = LocalDate.parse(shownText)
    val dl = deadlineDates.toSet()
    val ms = milestoneDates.toSet()
    Card(
        monthTitle(shown), Modifier.fillMaxWidth(), padding = 14.dp,
        action = {
            IconButton(onClick = { shownText = shown.minus(1, DateTimeUnit.MONTH).toString() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mes anterior")
            }
            IconButton(onClick = { shownText = shown.plus(1, DateTimeUnit.MONTH).toString() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Mes siguiente")
            }
        },
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        monthGrid(shown).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    val inMonth = d.month == shown.month
                    val isToday = d == today
                    val selected = d == anchor
                    Box(
                        Modifier.weight(1f).height(32.dp).padding(1.dp).clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isToday -> cs.primary
                                    selected -> cs.secondaryContainer
                                    else -> Color.Transparent
                                },
                            )
                            .clickable { onPick(d) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${d.day}",
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isToday -> cs.onPrimary
                                !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.45f)
                                else -> cs.onSurface
                            },
                        )
                        val mark = when { d in dl -> cs.error; d in ms -> if (isToday) cs.onPrimary else cs.primary; else -> null }
                        if (mark != null) {
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).width(12.dp).height(2.dp).background(mark))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingDatesCard(
    today: LocalDate,
    deadlines: List<dev.lifeos.db.Deadline>,
    milestones: List<dev.lifeos.db.Milestone>,
    tasks: List<dev.lifeos.db.Task>,
    projects: Map<String, Project>,
    onItem: (DayItem) -> Unit,
    onAdd: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val items = CalendarMath.dayItems(deadlines, milestones, emptyList(), projects, today, today.plusDays(90))
        .filter { !it.done }.take(6)
    Card(
        "Próximas fechas", Modifier.fillMaxWidth(), icon = Icons.Outlined.Event, padding = 14.dp,
        action = { IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "Nueva entrega o examen") } },
    ) {
        if (items.isEmpty()) EmptyText("Sin entregas, exámenes ni hitos en los próximos tres meses.")
        items.forEach { item ->
            val days = today.daysUntil(item.date)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onItem(item) }.padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.kind == DayItem.Kind.MILESTONE) Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = cs.primary)
                else Dot(cs.error, 8.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(Dates.dayName(item.date).take(3).replaceFirstChar { it.uppercase() } + " " + Dates.short(item.date), item.sub)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (days == 0) "Hoy" else "$days d",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (days <= 3 && item.kind != DayItem.Kind.MILESTONE) cs.error else cs.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}
