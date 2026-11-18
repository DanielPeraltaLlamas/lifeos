package dev.lifeos.data

import kotlinx.datetime.LocalDate

/**
 * Datos reales con los que arranca la app la primera vez (ver DECISIONES.md
 * y PROPUESTA.md §5). Nada inventado: los proyectos personales y las
 * asignaturas los crea el usuario.
 */
suspend fun Repository.seedIfNeeded() {
    if (setting("seeded") != null) return

    val tfg = addProject(
        name = "TFG",
        kind = ProjectKind.TFG,
        description = "Tracker de anime, manga y videojuegos",
        dueDate = LocalDate(2027, 2, 2),
    )
    listOf(
        Triple("Análisis de plataformas", LocalDate(2026, 11, 30), "Tabla comparativa de funcionalidades + alcance del proyecto"),
        Triple("Modelo de datos", LocalDate(2026, 12, 14), "Diagrama entidad-relación + esquema de la base de datos"),
        Triple("Arquitectura", LocalDate(2026, 12, 21), "Stack elegido, diagrama de arquitectura, repositorio creado"),
        Triple("Esqueleto + importación", LocalDate(2027, 1, 18), "Esqueleto de extremo a extremo + importación desde 1 plataforma"),
        Triple("Preparar revisión", LocalDate(2027, 1, 31), "Demo + documento de avance"),
    ).forEach { (title, date, deliverable) -> addMilestone(tfg, title, date, deliverable) }

    addHabit("Lavarse los dientes", HabitPeriod.DAY, target = 3, targetMax = 4)
    addHabit("Ducharse", HabitPeriod.DAY, target = 1, targetMax = null)
    addHabit("Gimnasio", HabitPeriod.WEEK, target = 3, targetMax = 4)

    putSetting("seeded", "1")
}
