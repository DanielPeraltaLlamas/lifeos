# Cómo retomar el proyecto

_Última sesión: 19/11/2026_

## Dónde estamos
- **Fase 1 (descubrimiento):** terminada. Ver `PROPUESTA.md` (está desactualizada en algunos puntos: manda `DECISIONES.md`).
- **Fase 2 (diseño):** **aprobada.** Prototipo navegable: https://claude.ai/artifact/YPhWZsZa7GHJU3ashSwJ9x. Copia local de sus archivos en `prototipo/project/`.
- **Fase 3 (desarrollo):** arquitectura aprobada (`ARQUITECTURA.md`). **Entrega 1 (Base) hecha el 18/11 y probada por el usuario el 19/11: funciona en el PC y en el móvil.** **Entregas 2 (Calendario) y 3 (Sincronización con Supabase) hechas y probadas el 19/11.**
- **Git: todo en local.** Nunca añadir remoto ni subir a GitHub (lo pidió el usuario).

## Documentos (léelos en este orden)
1. `instruciones.txt`: reglas del proyecto (tres fases; no avanzar sin aprobación).
2. `DECISIONES.md`: registro de **todas** las decisiones y del feedback. Es la fuente de verdad.
3. `ARQUITECTURA.md`: stack, componentes, modelo de datos y plan de entregas.
4. `PROPUESTA.md`: propuesta original de producto.
5. `prototipo/project/*.dc.html`: diseño de cada pantalla.

## Resumen de decisiones clave
- **Pantallas:** Hoy (tarjetas; "Lo primero" a ancho completo y grande), Calendario, Tareas y proyectos (TFG con hitos), Notas (carpetas por proyecto; los proyectos personales empiezan vacíos), Hábitos, Bandeja, Evolución, Cierre del día, Revisión semanal, Ajustes y modo Enfoque.
- **Fuera:** juegos (eliminado por ahora), salud, finanzas y vida personal.
- **Color:** dinámico desde el fondo de pantalla. En el ordenador, se lee `~/.local/state/caelestia/scheme.json` (roles de Material 3 en hex sin `#`, más `mode` dark o light). En el móvil, Material You. No hay paleta fija. Tipografía: Roboto Flex.
- **Móvil:** widgets de tareas de hoy, hábitos y captura rápida (+Nota / +Tarea), más una notificación fija que no se quita hasta completar el plan.
- **Bloqueo:** TikTok e Instagram con límite diario. Discord, Steam, Lutris y webs, solo durante los bloques de enfoque. Navegadores: Zen (personal) y Firefox (uni), con una sola WebExtension para los dos.
- **Stack:** Kotlin Multiplatform + Compose Multiplatform (Android + Linux), SQLite/SQLDelight, sincronización con Supabase (aceptado; alternativa futura: servidor en el PC + Tailscale).

## Cómo se ejecuta
- Escritorio: `./gradlew :desktopApp:run`. Datos en `~/.local/share/lifeos/lifeos.db`.
- Android: `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. adb no ve el móvil por USB, así que se manda con KDE Connect: `kdeconnect-cli -n "POCO X7 Pro" --share <apk>` y se instala a mano.
- Pruebas: `./gradlew :shared:desktopTest`.
- Capturas sin ventana (escritorio y móvil): `SCREENSHOTS_DIR=/ruta ./gradlew :shared:desktopTest --tests '*Screenshots*'`.
- `gradle.properties` fija `org.gradle.java.home` al JDK 21, así que funciona aunque el JDK del sistema sea el 27.

## Qué incluye la Entrega 1
- Módulos `shared/` (modelo, SQLDelight, tema y UI Compose), `androidApp/` y `desktopApp/`.
- Tablas: project, milestone, task, daily_plan, habit, habit_log, deadline, inbox_item, note y setting. Todas tienen id UUID, `updated_at` y `deleted_at` (borrado lógico) para la sincronización futura.
- Precarga solo de datos reales: el TFG (entrega el 2/2/2027), los hitos H1–H5 de PROPUESTA §5 y los hábitos de dientes, ducha y gimnasio.
- Tema: Caelestia en el escritorio (vigila `scheme.json` y cambia en caliente) y Material You en Android. Tipografía Roboto Flex.
- Pantallas con CRUD real:
  - **Hoy:** Lo primero, Plan de hoy, Se acerca y Hábitos.
  - **Tareas y proyectos:** proyectos, asignaturas, hitos, tareas, y entregas y exámenes.
  - **Hábitos:** diarios y semanales, con racha.
  - **Bandeja:** apuntar y luego pasar a tarea, a notas o descartar.
- Diferencias con el prototipo (llegan en entregas posteriores):
  - En Hoy aún no están las tarjetas Agenda y Asistente (Entrega 5).
  - El botón «Empezar · 50 min» de Lo primero llega con Enfoque (Entrega 3); por ahora se usan «Hecha» y «Cambiar».
  - Las notas creadas desde la Bandeja se guardan, pero la pantalla Notas es de la Entrega 5.

## Qué incluye la Entrega 2 (Calendario)
- Tabla `event` (migración `1.sqm`: la base de datos pasa a la versión 2; probada con una copia de la base real).
- `data/Calendar.kt`: repetición semanal, reparto de eventos solapados y elementos de «todo el día». Pruebas en `CalendarTest.kt`.
- `ui/screens/CalendarScreen.kt` y `ui/EventDialog.kt`. Tarjeta Agenda en Hoy.
- En Hoy, la cuadrícula queda como en el prototipo: Agenda + Se acerca, y debajo Plan + Hábitos.

## Qué incluye la Entrega 3 (Sincronización)
- `sync/SupabaseApi.kt` (Auth + PostgREST con Ktor), `sync/SyncEngine.kt` (subir, bajar, conflictos y primera vez), `ui/SyncUi.kt` (estado en el menú lateral del PC y nube en Hoy en el móvil).
- Pruebas en `SyncTest.kt` con un servidor falso que aplica las mismas reglas que el SQL.
- Copias antes de reemplazar datos: en el PC, `~/.local/share/lifeos/lifeos.db.copia-*`; en Android, `files/copias/`.

## Entorno ya preparado (17/11)
- `git init` hecho en esta carpeta (rama `main`, `.gitignore` creado). Aún sin commits.
- Android SDK en `~/Android/Sdk`: cmdline-tools, platform-tools, platforms;android-36 y build-tools;36.0.0, con licencias aceptadas.
- JDK 21 (Temurin 21.0.13) en `~/.jdks/jdk-21.0.13+9`. El JDK del sistema es el 27, demasiado nuevo para Gradle y Kotlin: hay que usar el 21 (`JAVA_HOME=~/.jdks/jdk-21.0.13+9`).
- Hecho el 18/11: wrapper de Gradle 9.9.1, `local.properties` y la plataforma `android-37.0` (Compose 1.12 obliga a compileSdk 37).
- Supabase: proyecto «LifeOs» (https://cvxdryjkglastqukzbhg.supabase.co). URL y clave publishable en `local.properties` (`supabase.url`, `supabase.key`); al compilar se genera `SupabaseConfig`. Tablas: `supabase/migrations/0001_sync.sql` (se pega en el SQL Editor). Si se añade una tabla o columna local, hay que añadirla también en `sync/SyncTables.kt` y en un SQL nuevo (la prueba `SyncTest.specMatchesLocalSchema` lo vigila).
- Versiones estables encontradas el 17/11/2026, para revisar al retomar:

| Componente | Versión |
|---|---|
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2 (Material 3 de CMP: 1.9.0) |
| AGP | 9.5.1 |
| SQLDelight | 2.4.1 |
| Glance | 1.2.0 |
| activity-compose | 1.13.0 |
| kotlinx-coroutines | 1.11.0 |
| kotlinx-datetime | 0.8.0 |
| kotlinx-serialization | 1.13.0 |
| Gradle | 9.9.1 |

  Ojo: AGP 9 exige el plugin `com.android.kotlin.multiplatform.library` para el módulo compartido y trae Kotlin integrado (no aplicar `kotlin-android`).

## Siguiente paso
1. Propuesta pendiente de respuesta: sincronizar al volver a la app (onResume en Android, foco de ventana en el PC).
2. Entrega 4: Enfoque y bloqueo (ver ARQUITECTURA.md §6).

**Pedir confirmación al usuario antes de empezar cada entrega.**
