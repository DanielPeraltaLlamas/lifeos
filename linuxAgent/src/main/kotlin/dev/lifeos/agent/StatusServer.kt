package dev.lifeos.agent

import com.sun.net.httpserver.HttpServer
import dev.lifeos.data.AGENT_PORT
import dev.lifeos.data.FocusMath
import dev.lifeos.db.Focus_session
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Servidor HTTP mínimo en 127.0.0.1 (no se ve desde fuera del PC):
 *   GET /ping   -> 200
 *   GET /status -> {"focus": true, "endsAt": 1795..., "title": "..."}
 * Lo usan la app (para no avisar dos veces) y la extensión del navegador.
 */
class StatusServer(private val current: () -> Focus_session?) {
    fun start() {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), AGENT_PORT), 0)
        server.createContext("/ping") { ex -> ex.sendResponseHeaders(200, -1); ex.close() }
        server.createContext("/status") { ex ->
            val now = System.currentTimeMillis()
            val s = current()?.takeIf { FocusMath.remaining(it, now) > 0 }
            val body = buildJsonObject {
                put("focus", s != null)
                if (s != null) {
                    put("endsAt", FocusMath.endsAt(s, now))
                    put("title", s.title)
                }
            }.toString().toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.responseHeaders.add("Access-Control-Allow-Origin", "*")
            ex.responseHeaders.add("Cache-Control", "no-store")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        server.start()
    }
}
