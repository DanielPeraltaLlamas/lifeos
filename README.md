# Life OS

Organizador personal para el día a día de un estudiante: tareas y proyectos, calendario, hábitos, notas y un **modo enfoque** que bloquea distracciones en el móvil, en el ordenador y en el navegador. Funciona sin conexión y se sincroniza entre Android y Linux.

> Las capturas y animaciones de este README usan **datos sintéticos** («Tarea 1», «Hábito 1», «Asignatura A»…). Se generan sin ventana a partir de una base de datos en memoria; ver [Regenerar las capturas](#regenerar-las-capturas).

<p align="center">
  <img src="docs/media/recorrido.gif" alt="Recorrido por las pantallas de escritorio" width="860">
</p>

---

## Índice

- [Funciones](#funciones)
- [Capturas](#capturas)
- [Arquitectura](#arquitectura)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Puesta en marcha](#puesta-en-marcha)
- [Pruebas](#pruebas)
- [Regenerar las capturas](#regenerar-las-capturas)

---

## Funciones

| Área | Qué hace |
|---|---|
| **Hoy** | «Lo primero» (la tarea que abre el día), plan de hoy, agenda, fechas que se acercan, hábitos y un asistente basado en reglas. |
| **Calendario** | Vistas de día, semana y mes; eventos semanales; reparto de eventos solapados; entregas, exámenes e hitos. |
| **Tareas y proyectos** | Proyectos, asignaturas, hitos con entregable, tareas estimadas en bloques de 50 min. |
| **Hábitos** | Diarios o semanales, con objetivo mínimo y máximo, y racha. |
| **Notas** | Ideas, notas y enlaces, organizados por proyecto. |
| **Bandeja** | Captura rápida desde cualquier sitio; después se pasa a tarea, a nota o se descarta. |
| **Evolución** | Tiempo de foco, tareas completadas y cumplimiento de hábitos. |
| **Rituales** | Cierre del día (elegir el plan de mañana) y revisión semanal (tres metas). |
| **Modo enfoque** | Bloques de 50 min que no se pueden terminar antes, con una pausa máxima de 10 min. Durante el bloque se bloquean apps y webs en todos los dispositivos. |
| **Bloqueo** | Android: límite diario para apps de redes sociales y bloqueo durante los bloques. Linux: un agente cierra apps de juegos y chat. Navegador: una WebExtension para Firefox y Zen. |
| **Widgets** | Android: tareas de hoy, hábitos y captura rápida. |
| **Tema dinámico** | Los colores salen del fondo de pantalla: Material You en Android y el esquema de [Caelestia](https://github.com/caelestia-dots) en Linux, con cambio en caliente. |
| **Sincronización** | *Local-first*: cada dispositivo tiene su SQLite y se sincroniza con Supabase (el último cambio gana, con borrado lógico). |

---

## Capturas

### Uso diario

Marcar hábitos, completar tareas del plan y apuntar algo en la bandeja; todo se actualiza al momento:

<p align="center">
  <img src="docs/media/interaccion.gif" alt="Interacción en la pantalla Hoy" width="760">
</p>

### Escritorio

| Hoy | Calendario |
|---|---|
| <img src="docs/media/desktop-today.png" alt="Hoy"> | <img src="docs/media/desktop-calendar.png" alt="Calendario"> |
| **Tareas y proyectos** | **Hábitos** |
| <img src="docs/media/desktop-tasks.png" alt="Tareas y proyectos"> | <img src="docs/media/desktop-habits.png" alt="Hábitos"> |
| **Notas** | **Bandeja** |
| <img src="docs/media/desktop-notes.png" alt="Notas"> | <img src="docs/media/desktop-inbox.png" alt="Bandeja"> |
| **Evolución** | **Ajustes** |
| <img src="docs/media/desktop-evolution.png" alt="Evolución"> | <img src="docs/media/desktop-settings.png" alt="Ajustes"> |
| **Cierre del día** | **Revisión semanal** |
| <img src="docs/media/desktop-ritual-close.png" alt="Cierre del día"> | <img src="docs/media/desktop-ritual-review.png" alt="Revisión semanal"> |

### Modo enfoque

<p align="center">
  <img src="docs/media/enfoque.gif" alt="Temporizador del modo enfoque" width="760">
</p>

### Android

| Hoy | Calendario | Tareas | Hábitos | Enfoque |
|---|---|---|---|---|
| <img src="docs/media/phone-today.png" alt="Hoy en Android" width="160"> | <img src="docs/media/phone-calendar.png" alt="Calendario en Android" width="160"> | <img src="docs/media/phone-tasks.png" alt="Tareas en Android" width="160"> | <img src="docs/media/phone-habits.png" alt="Hábitos en Android" width="160"> | <img src="docs/media/phone-focus.png" alt="Enfoque en Android" width="160"> |

---

## Arquitectura

```mermaid
flowchart LR
    subgraph shared["shared (Kotlin Multiplatform)"]
        UI["UI Compose + Material 3"]
        Repo["Repository"]
        DB[("SQLite · SQLDelight")]
        Sync["SyncEngine"]
        UI --> Repo --> DB
        Sync --> DB
    end

    Android["androidApp<br/>widgets · bloqueo"] --> shared
    Desktop["desktopApp<br/>tema Caelestia"] --> shared
    Agent["linuxAgent<br/>bloqueo · sincroniza"] --> DB
    Agent -- "estado en localhost" --> Ext["browserExtension<br/>Firefox · Zen"]
    Sync <--> Supabase[("Supabase<br/>Auth + PostgREST + RLS")]
    Agent <--> Supabase
```

- **Un solo lenguaje y una sola UI.** Casi todo vive en `shared/`: modelo, base de datos, sincronización, reglas del asistente y pantallas. Las apps de Android y Linux solo añaden lo propio de cada plataforma.
- **Local-first.** Todas las tablas tienen id UUID, `updated_at` y `deleted_at`; la app funciona sin red y sincroniza al abrirse o al volver a ella.
- **En el PC sincroniza solo el agente.** La app de escritorio y el agente comparten la misma base; la app detecta los cambios vigilando el archivo.
- **Bloqueo en tres capas.** AccessibilityService + UsageStats en Android, un agente de usuario en Linux y una WebExtension que consulta el estado del agente.

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.4 |
| UI | Compose Multiplatform 1.12 · Material 3 · Roboto Flex |
| Datos | SQLite · SQLDelight 2.4 |
| Red | Ktor 3.6 · kotlinx.serialization |
| Android | AGP 9.5 · Glance 1.2 (widgets) · Android 10+ (minSdk 29, compileSdk 37) |
| Nube | Supabase (Auth, PostgREST, Row Level Security) |
| Navegador | WebExtension (Manifest V3, `webRequest` bloqueante) |
| Build | Gradle 9.9 · JDK 21 |

Más detalle en [`ARQUITECTURA.md`](ARQUITECTURA.md) y en el registro de decisiones [`DECISIONES.md`](DECISIONES.md).

---

## Estructura del repositorio

```
shared/            Modelo, SQLDelight, sincronización, tema y UI comunes
androidApp/        App de Android: widgets, servicio de bloqueo, Material You
desktopApp/        App de escritorio (Linux): ventana, tema Caelestia
linuxAgent/        Agente en segundo plano: bloqueo de apps, sincronización, estado HTTP local
browserExtension/  Extensión de bloqueo para Firefox y Zen
supabase/          Migraciones SQL (tablas, RLS)
scripts/           Instalación de la app, del agente y firma de la extensión
docs/media/        Capturas y animaciones de este README
```

---

## Puesta en marcha

### Requisitos

- JDK 21 (Gradle y Kotlin aún no admiten versiones más nuevas; `gradle.properties` puede fijarlo con `org.gradle.java.home`).
- Android SDK con la plataforma 37, para la app de Android.
- Un proyecto de Supabase, para sincronizar (opcional: sin él, la app funciona solo en local).

### Configuración

Crea `local.properties` en la raíz (no se sube al repositorio):

```properties
sdk.dir=/ruta/a/Android/Sdk
supabase.url=https://TU-PROYECTO.supabase.co
supabase.key=TU_CLAVE_PUBLICABLE
```

Aplica las migraciones de `supabase/migrations/` en orden desde el SQL Editor de Supabase.

### Escritorio (Linux)

```bash
./gradlew :desktopApp:run          # desarrollo
./scripts/install-desktop.sh       # empaqueta e instala en el lanzador y el autostart
./scripts/install-agent.sh         # instala el agente como servicio de usuario
```

Los datos se guardan en `~/.local/share/lifeos/lifeos.db`.

### Android

```bash
./gradlew :androidApp:assembleDebug
# APK en androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Para el bloqueo hay que conceder **accesibilidad** y **acceso al uso**, y quitar la optimización de batería.

### Extensión del navegador

```bash
./scripts/sign-extension.sh        # firma con AMO para instalarla de forma permanente
```

---

## Pruebas

```bash
./gradlew :shared:desktopTest
```

Incluyen el repositorio, el calendario, el modo enfoque, las reglas de bloqueo, el asistente, flujos completos de UI y la sincronización contra un servidor falso que aplica las mismas reglas que el SQL de Supabase.

---

## Regenerar las capturas

Las imágenes de `docs/media/` salen de [`ReadmeMedia.kt`](shared/src/desktopTest/kotlin/dev/lifeos/ReadmeMedia.kt). Usa una base de datos **en memoria** con datos sintéticos, así que no toca los datos reales ni la nube:

```bash
README_MEDIA_DIR=/tmp/media ./gradlew :shared:desktopTest --tests '*ReadmeMedia*'

# GIF a partir de los fotogramas
cd /tmp/media
P="split[a][b];[a]palettegen=max_colors=96[p];[b][p]paletteuse=dither=bayer"
ffmpeg -framerate 10 -i frames-interaction/f-%04d.png -vf "scale=900:-1:flags=lanczos,$P" interaccion.gif
ffmpeg -framerate 2  -i frames-focus/f-%04d.png       -vf "scale=900:-1:flags=lanczos,$P" enfoque.gif
```

Los colores de las capturas de escritorio son los del esquema de Caelestia activo al generarlas.
