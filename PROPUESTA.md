# Personal Life OS — Propuesta de producto (v1)

_Fase 1 · Pendiente de aprobación · 17/11/2026_

---

## 1. Filosofía

**"Menos gestionar, más empezar."**

Tu problema no es falta de tiempo ni de capacidad, sino de **arranque**: te levantas sin un plan claro y el móvil llena ese vacío. Por eso el sistema no es un dashboard de toda tu vida. Es una herramienta que **te pone en marcha cada día y te protege mientras trabajas**.

Principios de diseño:
1. **5 segundos.** Al abrir la app sabes qué toca hacer ahora, sin tener que pensar.
2. **10 segundos.** Cualquier acción habitual (apuntar algo, marcar un hábito, empezar un bloque) cuesta menos de 10 s.
3. **Sin culpa.** Un día malo no rompe nada. El sistema te ayuda a retomar en vez de castigarte.
4. **Crece con el uso.** Solo se añade un módulo cuando el anterior ya forma parte de tu rutina.
5. **El ocio se gana, no se esconde.** Cuando cumples el plan, descansas sin remordimientos.

---

## 2. Arquitectura de secciones

| # | Sección | Para qué sirve |
|---|---|---|
| 1 | **Hoy** (pantalla principal) | Qué toca ahora, el plan del día, los hábitos, las próximas entregas y los avisos |
| 2 | **Enfoque** | Bloque de concentración activo, con temporizador y bloqueo de distracciones |
| 3 | **Tareas y proyectos** | Lista de tareas y proyectos con hitos (el TFG es el principal) |
| 4 | **Calendario** | Horario de la uni de lunes a miércoles, entregas y exámenes con cuenta atrás, y bloques planificados |
| 5 | **Hábitos** | Seguimiento con frecuencia flexible |
| 6 | **Bandeja** | Notas e ideas apuntadas al vuelo, para ordenar después |
| 7 | **Evolución** | Plan frente a lo hecho, horas de foco, hábitos y avance del TFG |
| 8 | **Ajustes** | Hábitos, horarios, bloqueo, integraciones |

Además hay dos **flujos guiados** que no son secciones fijas: el **cierre nocturno** (diario) y la **revisión semanal** (los domingos).

---

## 3. Funcionalidades

### 🔴 Imprescindibles

**3.1 Pantalla "Hoy" con arranque**
- **Qué hace:** arriba del todo muestra una sola cosa, *"Lo primero"*, con un botón **Empezar** que lanza un bloque de concentración. Debajo están el resto del plan (1–3 tareas), los hábitos de hoy, las próximas entregas y los avisos.
- **Problema que resuelve:** el "¿por dónde empiezo?" de cada mañana, que acaba en Discord.
- **Uso:** abres la app y pulsas Empezar.
- **Por qué:** es el núcleo del sistema. Si esta pantalla no funciona, lo demás da igual.

**3.2 Plan de la noche anterior**
- **Qué hace:** un flujo de unos 2 minutos. Repasas lo que hiciste hoy, eliges de 1 a 3 cosas para mañana y marcas cuál va primero.
- **Problema que resuelve:** despertarte sin plan.
- **Uso:** te llega un recordatorio a la hora que elijas, por ejemplo a las 23:00.
- **Por qué:** sin el plan de la noche, la pantalla "Hoy" no sabe qué proponerte.

**3.3 Bloques de concentración**
- **Qué hace:** un temporizador ligado a una tarea, con duración configurable (por ejemplo 50/10). Registra el tiempo real que trabajas en cada cosa.
- **Problema que resuelve:** distraerte a los pocos minutos y no saber cuánto trabajas de verdad.
- **Uso:** un toque para empezar y otro para terminar.
- **Por qué:** es el momento en que el plan se convierte en trabajo hecho, y alimenta las estadísticas.

**3.4 Bloqueo firme de distracciones**
- **Qué hace:** mientras dura un bloque, Discord, las redes y las webs que elijas quedan bloqueadas en el móvil (Android) y en el ordenador (Linux).
- **Problema que resuelve:** el móvil y Discord se comen tu día.
- **Uso:** es automático: empieza con el bloque y termina con él.
- **Por qué:** lo pediste tú, y la fuerza de voluntad sola no está bastando.
- **Nota técnica:** necesita un componente en cada dispositivo. La solución concreta se decide en la fase 3.

**3.5 Tareas y proyectos con hitos**
- **Qué hace:** tareas sueltas con fecha opcional, y proyectos divididos en hitos con fecha y entregable. El TFG es el primer proyecto.
- **Problema que resuelve:** un proyecto grande y difuso, sin hitos externos, se procrastina.
- **Uso:** al elegir el plan de mañana, la app te sugiere la siguiente tarea del hito que esté en curso.
- **Por qué:** el TFG es tu mayor riesgo de este curso.

**3.6 Entregas y exámenes siempre visibles**
- **Qué hace:** importa automáticamente desde el calendario de Blackboard (iCal), permite añadir cosas a mano y muestra una cuenta atrás en "Hoy" y en el calendario.
- **Problema que resuelve:** tener presente lo que se acerca para decidir bien qué hacer hoy.
- **Uso:** pasivo, no tienes que hacer nada salvo añadir lo que Blackboard no traiga.

**3.6b Calendario unificado** _(subido a imprescindible a petición del usuario)_
- **Qué hace:** una única vista con todo:
  - clases de lunes a miércoles;
  - entregas y exámenes;
  - hitos del TFG;
  - bloques de concentración, tanto planificados como hechos;
  - sesiones de gimnasio;
  - plan del día.

  Tiene vista de día, de semana y de mes, filtros por tipo y un código de color sobrio.
- **Problema que resuelve:** ver de un vistazo cómo está la semana o el mes y decidir con contexto.
- **Uso:** consulta rápida desde el móvil y planificación en el ordenador. Arrastras una tarea a un hueco y se convierte en un bloque planificado.
- **Por qué:** es la vista que une todo lo demás.

**3.7 Hábitos con frecuencia flexible**
- **Qué hace:** cada hábito tiene su frecuencia:
  - Dientes: 3–4 veces al día, con un contador de toques.
  - Ducha: una vez al día.
  - Gimnasio: un objetivo semanal de 3 sesiones (con 4, semana perfecta). Un día sin gimnasio no cuenta como fallo.
- **Problema que resuelve:** quieres crear estos hábitos y te cuestan.
- **Uso:** un toque desde "Hoy".

**3.8 Bandeja rápida**
- **Qué hace:** un campo para apuntar cualquier cosa en 2 segundos, desde cualquier pantalla. Luego conviertes cada nota en tarea o la guardas en una nota del TFG.
- **Problema que resuelve:** tenerlo todo en la cabeza, que es lo que te satura.

**3.9 Sincronización móvil ↔ ordenador**
- **Qué hace:** la misma app y los mismos datos en los dos dispositivos, con un diseño adaptado de verdad a cada uno.

### 🟠 Muy recomendables

**3.10 Cierre del día: plan frente a lo hecho.** Va dentro del flujo nocturno. Ves lo que planeaste, lo que hiciste y tus horas de foco, sin juzgarte. Te da conciencia sin culpa.

**3.11 Ocio desbloqueado.** Cuando completas el plan, "Hoy" cambia a un estado de *"Día cumplido 🎮 — disfruta"*. Así el ocio pasa a ser una recompensa en lugar de una huida.

**3.12 Revisión semanal (domingo, 10–15 minutos).** Un flujo guiado en cinco pasos:
1. Cómo fue la semana: horas de foco, hábitos y plan cumplido.
2. Qué se acerca: entregas y el hito del TFG.
3. Vaciar la bandeja.
4. Fijar las 3 metas de la semana.
5. Ajustar los hábitos si hace falta.

Es lo que evita que el sistema se abandone.

**3.13 Asistente de vida (basado en reglas).** Una franja de "Hoy" con avisos útiles e interpretados. Por ejemplo:
- *"Llevas 4 días sin tocar el TFG y el hito 2 vence en 6 días."*
- *"Esta semana llevas 1 de 3 sesiones de gimnasio. Hoy y el sábado los tienes libres."*
- *"Ayer empezaste tu primer bloque a las 16:40. ¿Probamos antes de las 12?"*

**3.14 Evolución.** Horas de foco por semana y por proyecto, porcentaje de plan cumplido, hábitos, avance del TFG por hitos y la hora de tu primer bloque.

### 🟡 Opcionales

**3.15 Hora de despertar.** Como aún no sabes qué hora quieres, la propuesta es **observar primero**. Durante 2 semanas solo se registra a qué hora empiezas tu primer bloque, sin ningún objetivo. Después decidimos una hora realista con datos reales.

**3.16 Tiempo de uso del móvil.** Leer de Android cuánto tiempo pasas en Discord y en las redes y mostrarlo en el cierre del día.

**3.17 Asistente con IA (LLM).** Un asistente con lenguaje natural que te propone el plan de mañana, resume la semana o divide un hito en tareas.

### 🔵 Futuras mejoras
- **Prácticas:** cuando las consigas, añadimos su horario y reorganizamos la semana.
- **Oposiciones:** cuando acabes la carrera, un módulo con temario, plan de estudio a largo plazo y repasos espaciados.
- **Bloques fijos en los días libres:** un horario propio de jueves a domingo, cuando el hábito de arrancar esté asentado.

### ❌ Fuera del alcance (decidido)
Salud, finanzas y vida personal (amigos, familia, pareja).

---

## 4. Planificación

| Horizonte | Mecanismo |
|---|---|
| **Diaria** | Plan nocturno (1–3 cosas) → arranque por la mañana → bloques → cierre |
| **Semanal** | Revisión del domingo → 3 metas de la semana + objetivo de gimnasio |
| **Mensual / curso** | Hitos del TFG y entregas en el calendario |

---

## 5. Primer proyecto cargado: TFG

**Tema:** una aplicación web de seguimiento de anime, manga/manhwa/manhua y videojuegos, que reúne las mejores funciones de las plataformas que ya existen.

**Revisión del 2/2/2027:** no tiene un contenido definido. Proponemos llegar con algo tangible.

| Hito | Fecha | Entregable | Objetivos del TFG que cubre |
|---|---|---|---|
| H1 · Análisis de plataformas | 30 nov | Tabla comparativa de funcionalidades (p. ej. MyAnimeList, AniList, Backloggd…) + alcance del proyecto | 1 |
| H2 · Modelo de datos | 14 dic | Diagrama entidad-relación + esquema de la base de datos | 2 |
| H3 · Arquitectura | 21 dic | Stack elegido, diagrama de arquitectura, repositorio creado | 3 |
| — · Navidades | 22 dic – 6 ene | Margen y colchón (ritmo suave) | — |
| H4 · Esqueleto + importación | 18 ene | Esqueleto de extremo a extremo funcionando + importación desde 1 plataforma | 4, 6 (parcial) |
| H5 · Preparar revisión | 31 ene | Demo + documento de avance | 5 (parcial) |

_Hay que ajustarlo a tus exámenes de enero._

---

## 6. Automatizaciones
- Importación periódica de las entregas desde Blackboard (iCal).
- Bloqueo de distracciones al empezar un bloque y desbloqueo al terminar.
- Recordatorio del plan nocturno (hora configurable).
- Recordatorio de la revisión semanal (los domingos).
- Reinicio de los contadores de hábitos (diario y semanal).
- Avisos del asistente generados a partir de tus datos.

## 7. Integraciones
- **Blackboard:** calendario iCal (falta comprobar que tu universidad lo ofrece).
- **Android (Poco X7 Pro):** bloqueo de apps y, si se aprueba, lectura del tiempo de uso.
- **Linux:** bloqueo de Discord y de webs durante los bloques.

---

## 8. Riesgo estratégico que quiero señalar

Tu TFG también es una aplicación web de seguimiento. **Construir el Life OS podría convertirse en la forma perfecta de procrastinar el TFG**, porque parece productivo y te gusta. Mitigación:
- El alcance de la v1 se queda en lo de arriba, sin extras.
- Yo me encargo del grueso del desarrollo, para que no te robe horas de TFG.
- Primero se carga el TFG en el sistema y el sistema empieza a usarse lo antes posible, aunque sea básico.
