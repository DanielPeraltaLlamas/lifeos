package dev.lifeos.android.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import dev.lifeos.android.LifeOsApp
import dev.lifeos.data.BlockRules
import dev.lifeos.data.FocusMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Mira qué app está delante. Si es una de las bloqueadas y toca bloquearla,
 * vuelve al inicio y enseña la pantalla de bloqueo. Mientras una app con límite
 * sigue abierta, lo revisa cada 15 s por si se acaban los minutos.
 */
class BlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private var current: String? = null
    private val focus by lazy {
        (application as LifeOsApp).repository.activeFocus.stateIn(scope, SharingStarted.Eagerly, null)
    }

    /** Los teclados abren ventanas propias; no cuentan como cambiar de app. */
    private val keyboards by lazy {
        getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }.toSet()
    }

    private val recheck = object : Runnable {
        override fun run() {
            current?.let { if (evaluate(it)) return }
            if (current != null && BlockRules.appName(current!!) != null) handler.postDelayed(this, 15_000)
        }
    }

    override fun onServiceConnected() {
        focus // empieza a escuchar la base de datos
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui" || pkg in keyboards) return
        if (pkg == current) return
        current = pkg
        handler.removeCallbacks(recheck)
        if (BlockRules.appName(pkg) == null) return
        if (!evaluate(pkg)) handler.postDelayed(recheck, 15_000)
    }

    /** Devuelve true si ha bloqueado. */
    private fun evaluate(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        val endsAt = focus.value?.takeIf { FocusMath.remaining(it, now) > 0 }?.let { FocusMath.endsAt(it, now) }
        val used = if (pkg in BlockRules.LIMITED) Usage.limitedTodayMs(this) else 0
        val reason = BlockRules.check(pkg, endsAt, used) ?: return false
        current = null
        handler.removeCallbacks(recheck)
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(BlockActivity.EXTRA_APP, BlockRules.appName(pkg))
                .putExtra(BlockActivity.EXTRA_FOCUS_END, (reason as? BlockRules.Reason.Focus)?.endsAt ?: 0L),
        )
        return true
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(recheck)
        scope.cancel()
        super.onDestroy()
    }
}
