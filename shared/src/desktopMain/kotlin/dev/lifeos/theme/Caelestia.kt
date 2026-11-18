package dev.lifeos.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

/**
 * Lee el esquema de Caelestia (~/.local/state/caelestia/scheme.json): roles de
 * Material 3 en hex sin '#', más "mode" (dark | light). Se vigila el archivo
 * para cambiar de tema en caliente cuando cambia el fondo de pantalla.
 */
object Caelestia {
    val path: Path = Path.of(System.getProperty("user.home"), ".local", "state", "caelestia", "scheme.json")

    fun read(): ColorScheme? = runCatching { parse(Files.readString(path)) }.getOrNull()

    fun parse(json: String): ColorScheme {
        val root = Json.parseToJsonElement(json).jsonObject
        val light = root["mode"]?.jsonPrimitive?.content == "light"
        val c = root.getValue("colours").jsonObject
        val base = if (light) lightColorScheme() else darkColorScheme()
        fun col(key: String, fallback: Color): Color =
            c[key]?.jsonPrimitive?.content?.removePrefix("#")?.toLongOrNull(16)
                ?.let { Color(0xFF000000 or it) } ?: fallback

        return base.copy(
            primary = col("primary", base.primary),
            onPrimary = col("onPrimary", base.onPrimary),
            primaryContainer = col("primaryContainer", base.primaryContainer),
            onPrimaryContainer = col("onPrimaryContainer", base.onPrimaryContainer),
            inversePrimary = col("inversePrimary", base.inversePrimary),
            secondary = col("secondary", base.secondary),
            onSecondary = col("onSecondary", base.onSecondary),
            secondaryContainer = col("secondaryContainer", base.secondaryContainer),
            onSecondaryContainer = col("onSecondaryContainer", base.onSecondaryContainer),
            tertiary = col("tertiary", base.tertiary),
            onTertiary = col("onTertiary", base.onTertiary),
            tertiaryContainer = col("tertiaryContainer", base.tertiaryContainer),
            onTertiaryContainer = col("onTertiaryContainer", base.onTertiaryContainer),
            background = col("background", base.background),
            onBackground = col("onBackground", base.onBackground),
            surface = col("surface", base.surface),
            onSurface = col("onSurface", base.onSurface),
            surfaceVariant = col("surfaceVariant", base.surfaceVariant),
            onSurfaceVariant = col("onSurfaceVariant", base.onSurfaceVariant),
            surfaceTint = col("surfaceTint", base.surfaceTint),
            inverseSurface = col("inverseSurface", base.inverseSurface),
            inverseOnSurface = col("inverseOnSurface", base.inverseOnSurface),
            error = col("error", base.error),
            onError = col("onError", base.onError),
            errorContainer = col("errorContainer", base.errorContainer),
            onErrorContainer = col("onErrorContainer", base.onErrorContainer),
            outline = col("outline", base.outline),
            outlineVariant = col("outlineVariant", base.outlineVariant),
            scrim = col("scrim", base.scrim),
            surfaceBright = col("surfaceBright", base.surfaceBright),
            surfaceDim = col("surfaceDim", base.surfaceDim),
            surfaceContainer = col("surfaceContainer", base.surfaceContainer),
            surfaceContainerHigh = col("surfaceContainerHigh", base.surfaceContainerHigh),
            surfaceContainerHighest = col("surfaceContainerHighest", base.surfaceContainerHighest),
            surfaceContainerLow = col("surfaceContainerLow", base.surfaceContainerLow),
            surfaceContainerLowest = col("surfaceContainerLowest", base.surfaceContainerLowest),
        )
    }

    /** Emite el esquema actual y uno nuevo cada vez que Caelestia reescribe el archivo. */
    fun changes(): Flow<ColorScheme?> = callbackFlow {
        send(read())
        val dir = path.parent
        if (!Files.isDirectory(dir)) {
            awaitClose { }
            return@callbackFlow
        }
        val watcher = FileSystems.getDefault().newWatchService()
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
        launch {
            try {
                while (isActive) {
                    val key = runInterruptible { watcher.take() }
                    val relevant = key.pollEvents().any { (it.context() as? Path)?.fileName?.toString() == path.fileName.toString() }
                    key.reset()
                    if (relevant) {
                        delay(150) // Caelestia escribe el archivo en varios pasos
                        read()?.let { send(it) }
                    }
                }
            } catch (_: ClosedWatchServiceException) {
            }
        }
        awaitClose { watcher.close() }
    }.flowOn(Dispatchers.IO)
}
