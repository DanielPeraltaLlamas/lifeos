package dev.lifeos.agent

import dev.lifeos.data.Dates
import dev.lifeos.data.desktopNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Apps que se cierran durante un bloque. */
enum class Distraction(val label: String) { DISCORD("Vesktop"), STEAM("Steam"), LUTRIS("Lutris") }

/** Decide si un proceso es una distracción (por ejecutable y argumentos). */
fun classify(command: String?, args: List<String>): Distraction? {
    val exe = command?.substringAfterLast('/')?.lowercase() ?: return null
    val all = (listOf(command) + args).joinToString(" ").lowercase()
    return when {
        exe == "vesktop" || exe == "discord" || exe == "discordcanary" || exe == "discordptb" ||
            "dev.vencord.vesktop" in all || "com.discordapp.discord" in all -> Distraction.DISCORD
        exe == "steam" || exe == "steamwebhelper" || "com.valvesoftware.steam" in all -> Distraction.STEAM
        exe == "lutris" || (exe.startsWith("python") && args.any { it.endsWith("/lutris") }) || "net.lutris.lutris" in all -> Distraction.LUTRIS
        else -> null
    }
}

/**
 * Mientras haya un bloque (endsAt != null), cada 2 s busca las apps de la lista
 * entre los procesos del usuario y las cierra. Avisa una vez por app y bloque.
 */
class Enforcer(private val focusEndsAt: () -> Long?) {
    private val me = System.getProperty("user.name")
    private val warned = mutableSetOf<Distraction>()

    fun start(scope: CoroutineScope) {
        scope.launch {
            var lastEnd: Long? = null
            while (true) {
                val end = focusEndsAt()
                if (end != lastEnd) { warned.clear(); lastEnd = end }
                if (end != null) sweep(end)
                delay(2_000)
            }
        }
    }

    private fun sweep(endsAt: Long) {
        val found = mutableMapOf<Distraction, MutableList<ProcessHandle>>()
        ProcessHandle.allProcesses().forEach { p ->
            val info = p.info()
            if (info.user().orElse(null) != me) return@forEach
            val d = classify(info.command().orElse(null), info.arguments().map { it.toList() }.orElse(emptyList())) ?: return@forEach
            found.getOrPut(d) { mutableListOf() } += p
        }
        for ((d, procs) in found) {
            procs.forEach { it.destroy() }
            Thread.sleep(1_500)
            procs.filter { it.isAlive }.forEach { it.destroyForcibly() }
            if (warned.add(d)) {
                desktopNotify("${d.label} cerrado", "Estás en un bloque de enfoque hasta las ${Dates.time(endsAt)}.")
            }
        }
    }
}
