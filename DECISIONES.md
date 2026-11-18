# Personal Life OS — Registro de decisiones

**Fase actual:** 3 · Desarrollo. **Arquitectura APROBADA (17/11)**: ver ARQUITECTURA.md. Sin código todavía; el usuario pidió parar. Ver CONTINUAR.md para retomar.
**Fase 3, respuestas del usuario (17/11):** navegadores Zen (personal) y Firefox (uni). Supabase aceptado (alternativas explicadas: servidor en el PC + Tailscale, PocketBase en un VPS, Firebase). Autoriza instalar JDK 21 y Android SDK y crear el repositorio git (hecho).
**Fase 2 aprobada:** 17/11/2026 (prototipo + widgets + colores dinámicos).
**Inicio:** 2026-11-17

## Hoja de ruta del descubrimiento
1. Panorama: situación actual y fricciones principales
2. Rumbo: objetivos y ambiciones
3. Áreas de vida en profundidad (solo las relevantes)
4. Hábitos de uso: cuándo, dónde y cómo usarías el sistema
5. Síntesis y propuesta de producto → aprobación

## Hallazgos
- Estudiante universitario; clases de lunes a miércoles (3 días/semana).
- No usa ninguna herramienta de organización: todo lo lleva en la cabeza.
- Problema detonante: últimamente le cuesta mucho organizarse y concentrarse.
- Estudia Ingeniería de Software, 4º curso (último año).
- Jueves a domingo sin clase, pero con proyectos y estudio pendientes → días sin estructura externa.
- Síntomas (los cuatro): olvida cosas, procrastina, se distrae, se satura sin saber por dónde empezar.
- Hipótesis: el problema central no es la falta de tiempo, sino la falta de estructura en los días libres + no tener un "cerebro externo".
- Frentes de este curso: TFG (tema ya elegido), búsqueda de prácticas. Después: oposiciones.
- Un jueves típico: despierta ~11:00 → Discord hasta comer → móvil hasta 17:00 → 1 h de repaso → móvil/series/juegos. ≈1 h productiva de ~13 h despierto.
- Hipótesis reforzada: el cuello de botella es **arrancar** (sin plan ni disparador al empezar el día); el móvil/Discord ocupa el vacío.
- Oposiciones: después de la carrera → no entra en la v1; módulo futuro.
- Hábitos deseados: lavarse los dientes (3–4/día), ducha (1/día), gimnasio (3–4/semana, flexible).
- Implicación: el sistema de hábitos necesita frecuencias variables (varias veces/día, X veces/semana), no solo "sí/no diario".
- Carga del curso: 4 asignaturas cuatrimestrales + 3 anuales (una es el TFG; otra, prácticas).
- Entregas: se entera por Blackboard y no se le pasan → la captura de fechas no es un problema; no hace falta priorizarla.
- TFG: primera entrega el 2 de febrero de 2027 (~11 semanas desde el 17/11/2026). Fase de investigación inicial. Sin hitos del tutor ("voy a mi aire") → riesgo alto de procrastinación.
- Entrega TFG 2/2: sin contenido definido, es una "primera revisión de cómo va todo" → hay que definir los hitos nosotros.
- Dispositivos: móvil Android (Poco X7 Pro, HyperOS) + ordenador Linux.
- ⚠️ Riesgo detectado: pasar de "nada" a un sistema complejo tiene alta probabilidad de abandono → el sistema debe empezar mínimo y crecer.

## Decisiones tomadas
- **Fuera del alcance:** salud y finanzas (el usuario no las quiere).
- **Dentro:** hábitos. Los de higiene (dientes, ducha) se mantienen: al usuario sí le cuestan.
- **Núcleo del sistema (aprobado):** planificar la noche anterior (1–3 cosas) + "arranque del día": al abrir la app, ver lo primero que toca y empezar un bloque de concentración con un clic.
- **Distracciones (aprobado):** el sistema interviene con el móvil/Discord (medir, recordar, contrastar plan vs. realidad). Nivel de intervención por definir.
- **Distracciones, nivel FIRME:** bloqueo de Discord y redes durante los bloques de concentración. Requiere apoyarse en herramientas del SO o del navegador (se definirá en la fase 3).
- **Entregas y exámenes:** sí se registran y están siempre visibles. No se le olvidan, pero quiere tenerlos presentes en todo momento.
- **TFG:** estructura con hitos propios cada ~2 semanas, contando hacia atrás desde el 2/2/2027 (opción B). Sin objetivo de horas semanales.
- **Captura de entregas:** opción C (mixta). Importación automática desde el calendario iCal de Blackboard (pendiente de verificar que existe) + alta manual para lo demás.
- **Ocio como recompensa:** aprobado. El ocio aparece como "desbloqueado" y sin culpa al completar el plan del día.
- **Vida personal (amigos, familia, pareja):** fuera del alcance de la v1.
- **Bandeja rápida de notas:** sí.
- **Revisión semanal (domingo, 10–15 min):** sí, el usuario se compromete.
- **Hora de despertar:** sin decidir → propuesta: observar 2 semanas antes de fijar un objetivo.
- **TFG:** app web de seguimiento de anime, manga/manhwa/manhua y videojuegos, que fusiona funciones de otras plataformas. Hitos propuestos en PROPUESTA.md §5.
- **Propuesta de producto v1:** redactada en PROPUESTA.md (17/11/2026), pendiente de aprobación.
- **Calendario unificado:** pasa a imprescindible. Reúne clases, entregas, exámenes, hitos, bloques, gimnasio y plan en una sola vista (día, semana y mes).
- **Blackboard iCal:** el usuario no encuentra la opción. Pendiente de una segunda comprobación; si no aparece, alta manual.
- **Ocio organizado (nuevo requisito, 17/11):** el sistema no es solo para la vida académica. También organiza el ocio para aprovechar mejor el tiempo. Prioridad: bloquear TikTok y apps similares de scroll infinito. Detalles pendientes de confirmar antes de la fase 2.
- **Bloqueo tipo B (bloqueado por defecto + límite diario):** TikTok, Instagram y apps similares de scroll infinito.
- **Bloqueo tipo A (solo durante los bloques de concentración):** Discord, Steam, Lutris y webs de distracción en el navegador (lista configurable: YouTube, Reddit, etc.).
- **Ocio = biblioteca de juegos (solo juegos):** lista de pendientes que incluye la biblioteca de Steam (importable con la API de Steam, horas incluidas) y los juegos de Nintendo (alta manual). Cada juego admite **metas autoimpuestas**: horas jugadas o un objetivo concreto (p. ej. "terminar la historia"). Es deliberadamente distinto del TFG: no es un tracker social, es planificación personal del ocio.
- **Ocio en el calendario:** también se pueden planificar bloques de juego.
- **Fase 2: APROBADA (17/11/2026).**
- **Prototipo v1 publicado:** https://claude.ai/artifact/YPhWZsZa7GHJU3ashSwJ9x. 12 pantallas: Hoy, Hoy móvil, Enfoque, Calendario, Tareas, Hábitos, Juegos, Bandeja, Evolución, Cierre, Revisión y Ajustes. Tema oscuro sobrio, acento lavanda (#7B88FF) para trabajo y ámbar para ocio. Navegación: barra lateral en escritorio y pestañas en móvil; Enfoque, Cierre y Revisión son modos a pantalla completa sin menú. Pendiente del feedback del usuario.
- **Feedback 1 (17/11):** los colores se dejan para el final; prioridad a la funcionalidad y la maquetación. Prefiere una estética **de tarjetas** (bento/grid de widgets con cabecera e icono, mini-gráficas y mini-calendario), según una referencia de dashboard de contenidos. → Hoy rediseñado con tarjetas; el resto de pantallas se adaptarán cuando lo valide.
- **Feedback 2 (17/11), Hoy:** se quitan las tarjetas de Foco, Hito TFG y Scroll, y el bloque de foco del menú lateral. "Lo primero" pasa a ocupar toda la primera fila. Agenda y Se acerca suben a la segunda fila (junto al Asistente); Plan, Hábitos y Ocio bajan a la tercera. El resumen "Plan de hoy" se integra como barra de progreso en la tarjeta del plan, para no duplicarlo. Se quita el mini-calendario. Resultado: de 13 a 7 cajas. Después, "Lo primero" se hace más grande.
- **Feedback 3 (17/11):** el calendario está aprobado. En Hábitos se quitan los "sugeridos por el asistente". **Nueva sección Notas:** carpetas por proyecto (TFG + proyectos personales) con notas de tipo idea, nota o enlace. Desde la Bandeja se puede mandar cualquier elemento a las notas de cualquier proyecto. Un proyecto personal puede convertirse en proyecto con hitos. Por ahora no tiene proyectos personales: la sección queda disponible y vacía (sin ejemplos inventados).
- **Feedback 4 (17/11):** estilo de tarjetas aplicado a Tareas, Hábitos, Juegos, Bandeja, Evolución, Ajustes, Cierre y Revisión. El Calendario se queda como está, por decisión del usuario. Enfoque sigue a pantalla completa sin tarjetas. Se quita "Empezar bloque" del menú lateral en todas las pantallas con tarjetas, como ya se hizo en Hoy.
- **Feedback 5 (17/11): fuera todo lo de juegos (de momento).** Se elimina la pantalla Juegos, la tarjeta de ocio de Hoy, los eventos de juego del calendario, las integraciones de Steam y Nintendo, el paso de juegos de la revisión y la comparativa ocio/scroll de Evolución, que pasa a ser solo scroll. El bloqueo de Steam y Lutris durante los bloques se mantiene, porque es control de distracciones. La pestaña móvil "Juegos" pasa a "Notas".
- **Colores:** comienza la pasada de color. Se presentan 5 paletas (Grafito y lavanda, Carbón y coral, Negro y lima, Azul noche y cian, Claro), pendientes de que el usuario elija.
- **Feedback 6 (17/11): rechaza el "oscuro neón" porque parece hecho por IA.** Segunda ronda de paletas mates y poco saturadas: Papel (recomendada), Claro neutro, Oscuro mate y Salvia. Se proponen también tipografías con más carácter (serifa en los títulos o IBM Plex Sans).
- ~~**Paleta elegida: Carbón y coral (17/11).**~~ (descartada tras verla aplicada)
- **Feedback 7 (17/11):** la segunda ronda (crema, serifa, terroso) "parece muy Claude": se descarta. Se proponen tres direcciones: A) nativa del sistema, B) suiza/editorial, C) gris medio con acento. Pendiente de elección o de una referencia del usuario.
- **Nuevo requisito MUY IMPORTANTE: widgets en el móvil (Android).** Widgets de hábitos, tareas de hoy y captura rápida de notas y tareas. Implicación técnica: una web/PWA no puede crear widgets de Android → el móvil necesita **app nativa o multiplataforma** (esto también encaja con el bloqueo de apps, que ya requería componente nativo). Se decidirá en la fase 3.
- **Widgets aprobados (17/11):** tareas de hoy, hábitos y captura rápida (+ Nota / + Tarea). El widget "Lo primero" no se incluye.
- **Notificación fija:** una notificación persistente del plan del día que no se puede quitar hasta completar las tareas. Incluye acciones "Hecha" y "Empezar bloque". Nota técnica: en Android 14+ el sistema permite deslizar incluso las notificaciones fijas, así que la app la vuelve a publicar al instante; en Linux se usará urgencia crítica y se reenviará igual.
- **COLOR = DINÁMICO DESDE EL FONDO DE PANTALLA (decisión final):** en el ordenador, la app lee el esquema que genera **Caelestia** (que usa los roles de color de Material) y cambia cuando cambia el fondo, incluido el modo claro u oscuro. En el móvil, Material You. Se acabaron las paletas fijas. La interfaz se construye con roles de color de Material 3 (primary, primaryContainer, surface, surfaceContainer, tertiary, error…). Significado fijo: principal = trabajo/TFG, terciario = hábitos, error = entregas urgentes. Tipografía: Roboto Flex (Material).
  - Implicación técnica para la fase 3: leer el archivo de esquema de Caelestia requiere una app de escritorio local o el agente local de Linux (que ya hacía falta para el bloqueo).
- **Estilo distinto en móvil y ordenador:** permitido. Propuesta: móvil con Material You (colores del fondo de pantalla, convenciones de Android); ordenador con su propio estilo. Misma estructura, iconos y colores de categoría en ambos para que se reconozca. Se aplica a todas las pantallas:
  - Fondo #0E0E10, tarjetas #18181B, bordes #27272A, texto #FAFAFA, texto secundario #A1A1AA.
  - Acento coral #F2665B (texto sobre el acento: #1A0605). También se usa para TFG y foco.
  - Entregas y exámenes en ámbar #F5B544, para no confundirlas con el acento.
  - Gimnasio en teal #4FC1A6.
  - Ocio y scroll en violeta #B79CF5.
  - Asignaturas en gris.
- **Prácticas:** no aparecen en el sistema hasta que las consiga. Entonces se reorganiza el horario.
- **Dispositivos:** móvil y ordenador por igual → requisito: sincronización entre ambos y diseño responsive real (no "versión móvil recortada").

## Ideas aparcadas / a validar
_(ninguna todavía)_
