package dev.lifeos.sync

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Sesión de Supabase Auth. expiresAt en milisegundos. */
data class Session(val accessToken: String, val refreshToken: String, val expiresAt: Long, val userId: String, val email: String)

/** Error que devuelve el servidor (no de red). */
class ApiException(val status: Int, val code: String?, message: String) : Exception(message)

/** Lo que la sincronización necesita del servidor. En las pruebas se sustituye por uno en memoria. */
interface Remote {
    suspend fun signIn(email: String, password: String): Session
    /** Devuelve null si hay que confirmar el correo antes de entrar. */
    suspend fun signUp(email: String, password: String): Session?
    suspend fun refresh(refreshToken: String): Session
    suspend fun upsert(token: String, table: String, rows: JsonArray)
    /** Filas con synced_at > since, de menor a mayor. */
    suspend fun changedSince(token: String, table: String, since: Long, limit: Int): JsonArray
    suspend fun hasRows(token: String, table: String): Boolean
}

class SupabaseApi(private val url: String, private val key: String) : Remote {
    private val http = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun HttpResponse.ok(): String {
        val text = bodyAsText()
        if (status.isSuccess()) return text
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        val code = obj?.get("error_code")?.jsonPrimitive?.content ?: obj?.get("code")?.jsonPrimitive?.content
        val msg = listOf("msg", "message", "error_description", "error")
            .firstNotNullOfOrNull { obj?.get(it)?.jsonPrimitive?.content } ?: text.take(200)
        throw ApiException(status.value, code, msg)
    }

    private fun session(text: String): Session {
        val o = json.parseToJsonElement(text).jsonObject
        val user = o["user"]!!.jsonObject
        return Session(
            accessToken = o["access_token"]!!.jsonPrimitive.content,
            refreshToken = o["refresh_token"]!!.jsonPrimitive.content,
            expiresAt = Clock.System.now().toEpochMilliseconds() + o["expires_in"]!!.jsonPrimitive.long * 1000,
            userId = user["id"]!!.jsonPrimitive.content,
            email = user["email"]?.jsonPrimitive?.content ?: "",
        )
    }

    private suspend fun auth(path: String, body: JsonObject): String =
        http.post("$url/auth/v1/$path") {
            header("apikey", key)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.ok()

    override suspend fun signIn(email: String, password: String): Session =
        session(auth("token?grant_type=password", buildJsonObject { put("email", email); put("password", password) }))

    override suspend fun signUp(email: String, password: String): Session? {
        val text = auth("signup", buildJsonObject { put("email", email); put("password", password) })
        val o = json.parseToJsonElement(text).jsonObject
        return if (o["access_token"] != null) session(text) else null
    }

    override suspend fun refresh(refreshToken: String): Session =
        session(auth("token?grant_type=refresh_token", buildJsonObject { put("refresh_token", refreshToken) }))

    override suspend fun upsert(token: String, table: String, rows: JsonArray) {
        if (rows.isEmpty()) return
        http.post("$url/rest/v1/$table") {
            header("apikey", key)
            header("Authorization", "Bearer $token")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            parameter("on_conflict", "user_id,id")
            contentType(ContentType.Application.Json)
            setBody(rows.toString())
        }.ok()
    }

    override suspend fun changedSince(token: String, table: String, since: Long, limit: Int): JsonArray {
        val text = http.get("$url/rest/v1/$table") {
            header("apikey", key)
            header("Authorization", "Bearer $token")
            parameter("select", "*")
            parameter("synced_at", "gt.$since")
            parameter("order", "synced_at.asc")
            parameter("limit", limit)
        }.ok()
        return json.parseToJsonElement(text).jsonArray
    }

    override suspend fun hasRows(token: String, table: String): Boolean {
        val text = http.get("$url/rest/v1/$table") {
            header("apikey", key)
            header("Authorization", "Bearer $token")
            parameter("select", "id")
            parameter("limit", 1)
        }.ok()
        return json.parseToJsonElement(text).jsonArray.isNotEmpty()
    }
}

/** Mensaje en castellano para un error de Supabase Auth. */
fun authMessage(e: Throwable): String = when {
    e !is ApiException -> "No hay conexión con el servidor. Prueba otra vez en un momento."
    e.code == "invalid_credentials" -> "Correo o contraseña incorrectos."
    e.code == "email_not_confirmed" -> "Falta confirmar el correo: abre el enlace que te ha llegado."
    e.code == "user_already_exists" -> "Ya existe una cuenta con ese correo. Usa «Entrar»."
    e.code == "weak_password" -> "La contraseña es demasiado corta (mínimo 6 caracteres)."
    e.code == "validation_failed" || e.code == "email_address_invalid" -> "El correo no es válido."
    e.code == "over_email_send_rate_limit" || e.status == 429 -> "Demasiados intentos. Espera un minuto."
    else -> "Error del servidor (${e.status}): ${e.message}"
}
