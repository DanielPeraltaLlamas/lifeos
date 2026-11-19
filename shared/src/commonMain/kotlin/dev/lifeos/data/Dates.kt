package dev.lifeos.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

object Dates {
    private val DAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val MONTHS = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    val zone: TimeZone get() = TimeZone.currentSystemDefault()

    fun today(clock: Clock = Clock.System): LocalDate = clock.todayIn(zone)

    /** Emite la fecha de hoy y vuelve a emitir al cambiar de día. */
    fun todayFlow(clock: Clock = Clock.System): Flow<LocalDate> = flow {
        while (true) {
            emit(today(clock))
            delay(30_000)
        }
    }.distinctUntilChanged()

    fun weekStart(d: LocalDate): LocalDate = d.minus(d.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun dayName(d: LocalDate): String = DAYS[d.dayOfWeek.isoDayNumber - 1]
    fun dayLetter(d: LocalDate): String = listOf("L", "M", "X", "J", "V", "S", "D")[d.dayOfWeek.isoDayNumber - 1]
    fun monthName(d: LocalDate): String = MONTHS[d.month.number - 1]

    /** "Jueves, 19 de noviembre" */
    fun long(d: LocalDate): String = "${dayName(d).replaceFirstChar { it.uppercase() }}, ${d.day} de ${monthName(d)}"

    /** "19 nov" */
    fun short(d: LocalDate): String = "${d.day} ${monthName(d).take(3)}"

    /** "martes 2 de febrero" */
    fun medium(d: LocalDate): String = "${dayName(d)} ${d.day} de ${monthName(d)}"

    /** "Hoy", "Mañana", "3 días", "Ayer", "Hace 4 días" */
    fun relative(days: Int): String = when {
        days == 0 -> "Hoy"
        days == 1 -> "Mañana"
        days == -1 -> "Ayer"
        days < 0 -> "Hace ${-days} días"
        else -> "$days días"
    }

    fun time(millis: Long): String {
        val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
        return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
    }

    /** "Hoy 10:31", "Ayer 22:10", "19 nov" */
    fun stamp(millis: Long, today: LocalDate): String {
        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
        return when (date) {
            today -> "Hoy ${time(millis)}"
            today.minus(1, DateTimeUnit.DAY) -> "Ayer ${time(millis)}"
            else -> short(date)
        }
    }

    fun hourNow(clock: Clock = Clock.System): Int = clock.now().toLocalDateTime(zone).hour

    /** Minutos desde medianoche, ahora. */
    fun minuteNow(clock: Clock = Clock.System): Int = clock.now().toLocalDateTime(zone).let { it.hour * 60 + it.minute }

    fun LocalDate.plusDays(n: Int): LocalDate = plus(n, DateTimeUnit.DAY)
}
