package dev.lifeos.data

import java.net.HttpURLConnection
import java.net.URI

/** Puerto local del agente de Linux (también lo usa la extensión del navegador). */
const val AGENT_PORT = 47613

/** true si el agente está en marcha (responde en localhost). */
fun agentRunning(): Boolean = runCatching {
    val c = URI("http://127.0.0.1:$AGENT_PORT/ping").toURL().openConnection() as HttpURLConnection
    c.connectTimeout = 300; c.readTimeout = 300
    c.responseCode == 200
}.getOrDefault(false)

/** Notificación del escritorio (la muestra el demonio de Caelestia). */
fun desktopNotify(title: String, body: String, critical: Boolean = false) {
    runCatching {
        ProcessBuilder(
            listOf("notify-send", "-a", "Life OS", "-u", if (critical) "critical" else "normal", "-i", "alarm-symbolic", title, body),
        ).start()
    }
}

/** Pide al agente que sincronice ya. */
fun agentSyncNow() {
    runCatching {
        val c = URI("http://127.0.0.1:$AGENT_PORT/sync").toURL().openConnection() as HttpURLConnection
        c.connectTimeout = 500; c.readTimeout = 500
        c.responseCode
    }
}
