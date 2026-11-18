package dev.lifeos.data

import dev.lifeos.data.Dates.plusDays
import dev.lifeos.db.Habit
import kotlinx.datetime.LocalDate

/** Días de historial de hábitos que se cargan (9 semanas). */
const val HABIT_HISTORY_DAYS = 63

object HabitMath {
    /** Veces en el periodo actual: hoy (diario) o esta semana, de lunes a hoy (semanal). */
    fun current(h: Habit, counts: HabitCounts, today: LocalDate): Int = when (h.periodEnum) {
        HabitPeriod.DAY -> counts[today] ?: 0
        HabitPeriod.WEEK -> weekTotal(counts, Dates.weekStart(today), today)
    }

    fun weekTotal(counts: HabitCounts, weekStart: LocalDate, until: LocalDate = weekStart.plusDays(6)): Int {
        var d = weekStart
        var sum = 0
        while (d <= until) {
            sum += counts[d] ?: 0
            d = d.plusDays(1)
        }
        return sum
    }

    /**
     * Racha de periodos seguidos cumpliendo el objetivo. El periodo actual
     * cuenta si ya está cumplido; si no, la racha empieza en el anterior,
     * así que no se rompe hasta que el periodo termina sin cumplirse.
     */
    fun streak(h: Habit, counts: HabitCounts, today: LocalDate): Int {
        val target = h.target.toInt()
        return when (h.periodEnum) {
            HabitPeriod.DAY -> {
                var d = if ((counts[today] ?: 0) >= target) today else today.plusDays(-1)
                var n = 0
                while ((counts[d] ?: 0) >= target && n < HABIT_HISTORY_DAYS) { n++; d = d.plusDays(-1) }
                n
            }
            HabitPeriod.WEEK -> {
                val thisWeek = Dates.weekStart(today)
                var w = if (weekTotal(counts, thisWeek) >= target) thisWeek else thisWeek.plusDays(-7)
                var n = 0
                while (weekTotal(counts, w) >= target && n < HABIT_HISTORY_DAYS / 7) { n++; w = w.plusDays(-7) }
                n
            }
        }
    }

    /** "3–4 veces al día", "1 al día", "3 a la semana (4 = semana perfecta)" */
    fun describe(h: Habit): String {
        val t = h.target.toInt()
        val max = h.target_max?.toInt()
        return when (h.periodEnum) {
            HabitPeriod.DAY -> if (max != null) "$t–$max veces al día" else if (t == 1) "1 vez al día" else "$t veces al día"
            HabitPeriod.WEEK -> "Objetivo semanal: $t" + if (max != null) " ($max = semana perfecta)" else ""
        }
    }
}
