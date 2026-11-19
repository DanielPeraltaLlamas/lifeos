# Personal Life OS — Arquitectura técnica (propuesta)

_Fase 3 · Paso 1 · Pendiente de aprobación · 17/11/2026_

---

## 0. Requisitos que mandan sobre la arquitectura

| Requisito | Qué obliga a hacer |
|---|---|
| Widgets en Android (tareas, hábitos, captura) | App **nativa** de Android; una web/PWA no puede crearlos |
| Bloqueo de TikTok/Instagram (límite) y de Discord en bloques | Servicio nativo en Android (AccessibilityService + UsageStats) |
| Notificación fija hasta completar el plan | Servicio en primer plano en Android + notificaciones D-Bus en Linux |
| Colores del fondo vía Caelestia | App local de escritorio que lea `~/.local/state/caelestia/scheme.json` (comprobado: roles de Material 3 en hex) y detecte sus cambios |
| Bloquear Discord, Steam y Lutris en Linux | Agente local con acceso a los procesos |
| Mismos datos en móvil y ordenador, también sin conexión | Base de datos local en cada dispositivo + sincronización |
| Un solo usuario, mantenible por una persona | Evitar microservicios, servidores propios y complejidad innecesaria |

Conclusión: hacen falta **dos apps nativas** (Android y Linux) que compartan todo lo posible, más una **nube sencilla** para sincronizar.

---

## 1. Stack recomendado

### Lenguaje y UI: **Kotlin Multiplatform + Compose Multiplatform**
- **Un solo lenguaje (Kotlin)** para Android y escritorio Linux.
- Se comparte entre las dos apps: modelo de datos, base de datos, sincronización, reglas del asistente y **la mayor parte de la interfaz** (Compose + Material 3).
- Las partes más difíciles (widgets con *Glance*, servicio de accesibilidad para el bloqueo, notificación fija) son de Android y se escriben en Kotlin, sin puentes entre lenguajes.
- **Material 3 nativo**: los roles de color de Caelestia (`primary`, `surfaceContainer`…) y los de Material You encajan directamente en el `ColorScheme`, sin traducir nada.

| Alternativa | Por qué no |
|---|---|
| Flutter | También es válido, pero widgets, bloqueo y notificaciones obligan a escribir Kotlin igualmente y a mantener puentes Dart ↔ Kotlin. Serían dos lenguajes en lugar de uno. |
| Web (React) + Capacitor/Tauri | Tres "envoltorios" distintos y, aun así, plugins nativos para todo lo importante. Es lo que más complejidad añade. |
| Kotlin solo para Android + otra app de escritorio | Duplicaría toda la lógica. |

### Base de datos local: **SQLite con SQLDelight**
- SQL tipado y compartido entre Android y escritorio.
- La app funciona sin conexión y los widgets leen al instante.

### Sincronización: **Supabase** (PostgreSQL gestionado, plan gratuito)
- Postgres + autenticación + API, sin servidor que mantener.
- Modelo **local-first**: escribes en local y se sincroniza en segundo plano (al abrir la app, tras cada cambio y periódicamente).
- Conflictos: **gana el último cambio por fila** (`updated_at`), con borrado lógico (`deleted_at`). Para un único usuario es suficiente.
- Seguridad: *Row Level Security*; solo tu usuario puede leer o escribir tus filas. Las claves no se guardan en el repositorio.
- Aviso: en el plan gratuito, un proyecto se pausa tras unos 7 días sin uso. Con uso diario no pasa. Elegiremos región de la UE.

| Alternativa | Por qué no |
|---|---|
| PocketBase en un VPS propio | Otro servidor que mantener y pagar. |
| Solo en local, sincronizando con Syncthing | SQLite no se lleva bien con la copia de archivos entre dos dispositivos: hay riesgo de corromper la base de datos. |

---

## 2. Componentes

```
lifeos/
├── shared/            Kotlin común: modelo, SQLDelight, sincronización, asistente (reglas), UI Compose compartida
├── androidApp/        App Android: widgets (Glance), bloqueo (Accessibility + UsageStats),
│                      notificación fija (foreground service), Material You
├── desktopApp/        App Linux (Compose Desktop): tema desde Caelestia, notificaciones D-Bus,
│                      agente de bloqueo (procesos) + servidor local para la extensión
├── browser-extension/ Extensión WebExtension (Zen/Firefox y Chromium): bloquea webs durante los bloques
└── supabase/          Migraciones SQL y políticas RLS
```

### Bloqueo
- **Android:** un `AccessibilityService` detecta qué app está en primer plano. Si es TikTok o Instagram y se ha agotado el límite diario, o es Discord durante un bloque, muestra una pantalla de bloqueo y te devuelve al inicio. `UsageStatsManager` mide los minutos. En HyperOS habrá que dar permisos de autoarranque y quitar la optimización de batería.
- **Linux:** durante un bloque, el agente cierra Discord, Steam y Lutris si se abren. La extensión del navegador bloquea las webs de la lista y consulta al agente en `localhost` si hay un bloque activo.
- **Con sinceridad:** en tu propio ordenador eres administrador, así que siempre podrás saltarte el bloqueo si te empeñas. El objetivo es añadir fricción, no ser una cárcel. En Android es bastante más firme.

### Notificación fija
- **Android:** notificación en curso ligada a un servicio en primer plano. Si se desliza (Android 14+ lo permite), se vuelve a publicar al momento. Desaparece al completar el plan.
- **Linux:** notificación D-Bus con urgencia crítica, que el demonio de notificaciones de Caelestia mantiene hasta que se descarta. Si la quitas con tareas pendientes, se reenvía.

### Colores
- **Escritorio:** se lee `scheme.json` de Caelestia y se vigila el archivo. Cuando cambia, el tema se actualiza en caliente (incluido el modo claro u oscuro).
- **Android:** `dynamicDarkColorScheme` / `dynamicLightColorScheme` (Material You).

---

## 3. Modelo de datos (primera versión)

`projects` (TFG, personales, asignaturas) · `milestones` (hitos) · `tasks` (fecha, proyecto, hito, estimación, estado) · `daily_plans` (plan del día, "lo primero") · `focus_sessions` (bloques de enfoque: inicio, fin, tarea) · `habits` + `habit_logs` (frecuencia diaria o semanal, número de registros) · `deadlines` (entregas y exámenes) · `events` (horario y bloques del calendario) · `notes` (por proyecto: idea, nota o enlace) · `inbox_items` · `block_rules` + `usage_logs` (minutos de scroll) · `settings`.

Todas las tablas llevan `id` (UUID), `updated_at` y `deleted_at` para poder sincronizar.

---

## 4. Despliegue
- **Android:** APK instalado directamente en el Poco X7 Pro (sin Play Store, porque Google restringe el uso de accesibilidad para bloquear apps).
- **Linux:** paquete para Arch (PKGBUILD) con la app, un archivo `.desktop` y un **servicio de usuario de systemd** para el agente, que arranca con la sesión.
- **Supabase:** proyecto gratuito; las migraciones se aplican desde `supabase/`.
- **Repositorio:** git en esta carpeta.

## 5. Entorno de desarrollo necesario
- Ya instalado: JDK 27, adb, node y bun.
- Falta:
  - **JDK 21**: Gradle y Kotlin aún no van fiables con JDK 27, así que se instala en paralelo.
  - **Android SDK** (command-line tools; Android Studio es opcional).
  - Cuenta de **Supabase**.

## 6. Plan de construcción por entregas
Cada entrega se puede usar de verdad antes de pasar a la siguiente:

1. **Base:** esqueleto del proyecto, modelo y base de datos local, tema dinámico. Pantallas Hoy, Tareas y proyectos, Hábitos y Bandeja en escritorio y Android. ✅
2. **Calendario** (adelantado el 19/11): día, semana y mes, eventos semanales y tarjeta Agenda en Hoy. ✅
3. **Sincronización** con Supabase y acceso con tu cuenta. ✅
4. **Enfoque y bloqueo:** temporizador, bloqueo en Android, agente de Linux y extensión del navegador. ✅
5. **Widgets y notificación fija.**
6. **El resto:** Notas, Cierre del día, Revisión semanal, Evolución, asistente por reglas y Ajustes.
7. **Calidad:** pruebas de flujos, persistencia, sincronización, móvil y escritorio, y que no haya botones sin función.
