package dev.lifeos.data

import dev.lifeos.db.Focus_session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Vigila el bloque en marcha: avisa de cada cambio (para notificaciones del sistema)
 * y lo cierra al cumplir su tiempo aunque la pantalla de enfoque no esté abierta.
 */
class FocusWatcher(
    private val repo: Repository,
    private val onChange: (Focus_session?) -> Unit = {},
    private val onFinished: (Focus_session) -> Unit = {},
    private val clock: Clock = Clock.System,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            repo.activeFocus.collectLatest { s ->
                onChange(s)
                if (s == null) return@collectLatest
                while (true) {
                    val now = clock.now().toEpochMilliseconds()
                    val left = FocusMath.remaining(s, now)
                    if (left <= 0) {
                        if (repo.finishFocusIfDue(s.id)) onFinished(s)
                        break
                    }
                    // Si está en pausa, la pausa puede acabarse sola: se revisa a menudo.
                    delay(minOf(left, 5_000))
                    onChange(s)
                }
            }
        }
    }
}
