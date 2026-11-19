# Personal Life OS — Registro de decisiones

**Fase actual:** 3 · Desarrollo. Arquitectura aprobada (17/11). **Entrega 1 (Base) construida el 18/11**, pendiente de prueba del usuario. Ver CONTINUAR.md para retomar.
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

- **Git solo en local (18/11):** el usuario no quiere subir el proyecto a su cuenta de GitHub. Nada de remotos ni de push.
- **Entrega 1 (18/11):** las tarjetas Agenda y Asistente de Hoy y el botón «Empezar» de Lo primero se dejan para las entregas en las que existen el Calendario, el asistente y el modo Enfoque, para no tener botones sin función.
- **Datos iniciales (18/11):** solo se precargan datos reales: el TFG, sus hitos H1–H5 y los tres hábitos. Las asignaturas del prototipo no se precargan; las crea el usuario.
- **Prueba de la Entrega 1 (19/11):** funciona bien en el PC y en el móvil (Poco X7 Pro). Lo único que echa en falta es el Calendario. El APK se instala por KDE Connect, porque el móvil no aparece por USB (probablemente el cable solo carga).
- **Orden de entregas (19/11):** el Calendario se adelanta a la Entrega 2. La sincronización con Supabase pasa a la 3 (y se corre el resto).
- **Entrega 2, Calendario (19/11):** vistas de día, semana y mes (semana por defecto en el ordenador; día en el móvil). Eventos con hora de cuatro tipos: clase, estudio, gimnasio y otro, opcionalmente ligados a una asignatura o proyecto, con lugar y repetición semanal (con fecha de fin opcional). Un evento semanal se puede borrar solo un día, desde un día en adelante o entero; las ediciones se aplican a toda la serie. En la fila «todo el día» salen entregas, exámenes, hitos y tareas con fecha límite (y, en la vista de día, las tareas del plan). Al pulsar cualquiera se edita. Panel lateral: mes en miniatura y próximas fechas. En Hoy aparece la tarjeta Agenda (eventos de hoy, con «Ahora») y el subtítulo dice cuántas clases hay. Colores desde el tema dinámico; estudio con borde discontinuo (planificado). Las entregas ya se pueden editar.
- **Pendiente del calendario:** importar el iCal de Blackboard (sin verificar que exista), arrastrar tareas a huecos libres y los bloques de enfoque reales (Entrega de Enfoque).
- **Horario real cargado (19/11), solo en el PC:** asignaturas Paradigmas, Visión por Computador, Proyectos 3, Dirección de Proyectos, Aprendizaje Automático y Proyectos 4, con sus 7 clases semanales (lunes, martes y miércoles). El usuario confirma que se ven bien. En el móvil no están hasta que haya sincronización.
- **Entrega 3, Sincronización (19/11):** Supabase (proyecto «LifeOs», región UE-Irlanda). Local-first: cada dispositivo escribe en su SQLite y sincroniza solo (2 s después de cada cambio, cada 2 minutos y al abrir). Gana el cambio más reciente por fila (`updated_at`), aplicado también en el servidor con un trigger. Seguridad por filas: cada usuario solo ve lo suyo. Acceso con correo y contraseña (usuario creado en el panel con «Auto Confirm»). La URL y la clave publishable están en `local.properties` (fuera de git). Primera vez en un dispositivo: si la nube tiene datos, copia de seguridad + se vacía lo local + se descarga; si está vacía, se sube todo. El PC subió primero y el móvil descargó sus datos. **El usuario confirma que funciona en los dos.** Los ajustes (`setting`) no se sincronizan.
- **Respuestas al plan de la Entrega 4 (19/11):** límite de 1 h 30 al día entre TikTok e Instagram juntos; **no se puede salir ni terminar antes un bloque** (opción c), solo pausar (máx. 10 min por bloque, el bloqueo sigue en pausa); durante un bloque TikTok, Instagram y Discord se bloquean del todo; webs bloqueadas: YouTube, Reddit, Twitch, Discord, TikTok e Instagram (estas dos, siempre en el PC: propuesta pendiente de confirmar); hasta 1 minuto de retraso en el móvil está bien; la cuenta de Mozilla se verá en la 4d.
- **Entrega 4a, Modo enfoque (19/11):** hecha y probada. Bloques de 50 min sincronizados entre dispositivos, descanso de 10 min, avisos en PC (notify-send) y Android (notificación con cuenta atrás + alarma exacta). Bloques hechos en el calendario como «Foco».
- **Entrega 4b, Bloqueo en Android (19/11):** hecha y probada (el usuario confirma que le saca de TikTok). Servicio de accesibilidad + acceso al uso. En HyperOS, la accesibilidad de apps instaladas fuera de la tienda da «Se ha denegado»: se resolvió con depuración inalámbrica (`adb pair` + `appops set dev.lifeos ACCESS_RESTRICTED_SETTINGS allow` y `GET_USAGE_STATS allow`) y el usuario la activó a mano después. `settings put secure` no funciona en Xiaomi sin «Depuración USB (ajustes de seguridad)». Life OS añadido a la lista blanca de batería (`dumpsys deviceidle whitelist +dev.lifeos`). Falta que el usuario active Autoinicio a mano.
- **Entrega 4c, Agente de Linux (19/11):** hecha y probada (el usuario confirma que funciona). Servicio de usuario `lifeos-agent` (systemd, `scripts/install-agent.sh`, `--remove` para quitarlo). Durante un bloque cierra Vesktop (Discord), Steam y Lutris y avisa una vez por app. Sincroniza con la misma sesión que la app (comparten la base de datos; `PRAGMA data_version` para ver los cambios del otro proceso). Estado en `http://127.0.0.1:47613/status` para la extensión. Ocupa ~165 MB.
- **Terminar un bloque a mano:** solo a petición del usuario (p. ej. bloques de prueba), poniendo `ended_at` en la base del PC; no hay botón en la app (regla «no se puede salir»).
- **Entrega 4d, Extensión del navegador (19/11):** hecha y probada en Zen y Firefox. Firmada por Mozilla como «unlisted» (`scripts/sign-extension.sh`; claves `amo.issuer`/`amo.secret` en `local.properties`). Durante un bloque bloquea YouTube, Reddit, Twitch y Discord web (también pestañas ya abiertas y navegación interna de YouTube); TikTok e Instagram, siempre en el PC. Consulta el agente cada 15 s. Si se cambia la extensión, hay que subir `version` en el manifest y volver a firmar.
- **Arreglo (19/11):** `PRAGMA data_version` no servía para notar los cambios del otro proceso (app ↔ agente); ahora se vigila la fecha y el tamaño del archivo de la base. Prueba: `ExternalChangesTest`.
- **Entrega 4 (Enfoque y bloqueo): COMPLETA (19/11).**
- **Notificación fija del plan: ELIMINADA (19/11),** por decisión del usuario. La Entrega 5 queda solo con los widgets de Android (tareas de hoy, hábitos y captura rápida).
- **Entrega 5, Widgets (19/11): COMPLETA y probada.** Tres widgets Glance con Material You: «Hoy» (lo primero con Empezar · 50 min, plan con casillas), «Hábitos» (botón + por hábito) y «Apuntar» (+ Nota a la Bandeja, + Tarea con «Para hoy»). Lección: en Glance los datos se escuchan dentro de `provideContent` (la sesión sigue viva y `updateAll` no vuelve a llamar a `provideGlance`).
- **Entrega 6 (19/11): hecha.** Cierre del día (aviso en Hoy desde las 21:00; elige hasta 3 para mañana y lo primero; lo pendiente no elegido sale del plan), Notas (carpetas Universidad / Proyectos personales / Sin proyecto; idea, nota o enlace; con título), Evolución (foco y scroll por semana, primer bloque, plan cumplido, TFG), Revisión semanal (5 pasos; aviso el domingo), Asistente por reglas (máx. 3 avisos, botón Planificar) y Ajustes sincronizados (tabla `pref`: límite de scroll, webs, duración de bloque y descanso, hora del cierre, día de la revisión). Móvil: barra inferior Hoy · Calendario · Tareas · Hábitos · Más. El móvil sube los minutos de scroll por día (`usage_log`). Base de datos v4; SQL `0003_entrega6.sql`. Extensión 1.1.0 (recibe las listas de webs del agente).
- **Sesión perdida (19/11) y arreglo:** la app del PC y el agente renovaron a la vez el mismo token y Supabase cerró la sesión. Ahora, si el agente está en marcha, la app del PC no sincroniza (modo pasivo: lee el estado de la base y pide «sincronizar ahora» al agente por `localhost:47613/sync`). El usuario volvió a entrar y funciona.

## Ideas aparcadas / a validar
_(ninguna todavía)_
