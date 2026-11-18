# Cómo retomar el proyecto

_Última sesión: 17/11/2026_

## Dónde estamos
- **Fase 1 (descubrimiento):** terminada. Ver `PROPUESTA.md` (está desactualizada en algunos puntos: manda `DECISIONES.md`).
- **Fase 2 (diseño):** **aprobada.** Prototipo navegable: https://claude.ai/artifact/YPhWZsZa7GHJU3ashSwJ9x. Copia local de sus archivos en `prototipo/project/`.
- **Fase 3 (desarrollo):** **arquitectura aprobada** (`ARQUITECTURA.md`). **Todavía NO hay código.** El usuario pidió parar antes de empezar a programar.

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

## Entorno ya preparado (17/11)
- `git init` hecho en esta carpeta (rama `main`, `.gitignore` creado). Aún sin commits.
- Android SDK en `~/Android/Sdk`: cmdline-tools, platform-tools, platforms;android-36 y build-tools;36.0.0, con licencias aceptadas.
- JDK 21 (Temurin 21.0.13) en `~/.jdks/jdk-21.0.13+9`. El JDK del sistema es el 27, demasiado nuevo para Gradle y Kotlin: hay que usar el 21 (`JAVA_HOME=~/.jdks/jdk-21.0.13+9`).
- **Pendiente:**
  - Gradle: descargar 9.9.1 y generar el wrapper.
  - Escribir `local.properties` con `sdk.dir=/home/luqui2/Android/Sdk`.
  - Crear la cuenta de Supabase (la crea el usuario).
- Versiones estables encontradas el 17/11/2026, para revisar al retomar:

| Componente | Versión |
|---|---|
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
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
Entrega 1 de `ARQUITECTURA.md` (Base):
- esqueleto KMP (`shared/`, `androidApp/`, `desktopApp/`);
- modelo de datos y base de datos local;
- tema dinámico desde Caelestia y Material You;
- pantallas Hoy, Tareas y proyectos, Hábitos y Bandeja con CRUD real.

**Pedir confirmación al usuario antes de empezar a escribir código.**
