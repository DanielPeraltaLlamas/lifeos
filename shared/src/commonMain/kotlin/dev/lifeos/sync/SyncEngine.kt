package dev.lifeos.sync

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import dev.lifeos.data.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

data class SyncState(
    /** false si la app se compiló sin las claves de Supabase. */
    val configured: Boolean,
    val email: String? = null,
    val status: Status = Status.SIGNED_OUT,
    val lastSync: Long? = null,
    val message: String? = null,
) {
    enum class Status { SIGNED_OUT, SYNCING, IDLE, OFFLINE, ERROR }
}

/**
 * Sincronización local-first con Supabase.
 *
 * - Subir: filas locales con updated_at mayor que lo último subido (por tabla).
 * - Bajar: filas remotas con synced_at mayor que lo último bajado (por tabla).
 * - Conflictos: gana la fila con updated_at mayor, tanto aquí como en el servidor.
 * - Primera vez en un dispositivo: si la nube ya tiene datos, se guarda una copia
 *   de la base local, se vacía y se descarga todo; si está vacía, se sube todo.
 *
 * Las marcas y la sesión se guardan en la tabla `setting`, que no se sincroniza.
 */
@OptIn(FlowPreview::class)
class SyncEngine(
    private val driver: SqlDriver,
    private val repo: Repository,
    private val remote: Remote?,
    private val backup: () -> Unit,
    private val clock: Clock = Clock.System,
    private val tables: List<SyncTable> = SYNC_TABLES,
) {
    private val io = Dispatchers.IO
    private val mutex = Mutex()
    private val kick = Channel<Unit>(Channel.CONFLATED)
    private var session: Session? = null
    private val tx = object : TransacterImpl(driver) {}

    private val _state = MutableStateFlow(SyncState(configured = remote != null))
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private fun now() = clock.now().toEpochMilliseconds()

    /** Arranca: recupera la sesión y sincroniza al abrir, tras cada cambio y cada 2 minutos. */
    fun start(scope: CoroutineScope) {
        if (remote == null) return
        scope.launch {
            restoreSession()
            launch { repo.localChanges.debounce(2_000).collect { kick.trySend(Unit) } }
            launch { while (true) { kick.trySend(Unit); delay(120_000) } }
            for (k in kick) syncOnce()
        }
    }

    fun syncNow() { kick.trySend(Unit) }

    /** Al abrir la app o volver a ella. Si se sincronizó hace menos de 20 s, no hace nada. */
    fun onAppVisible() {
        val last = _state.value.lastSync ?: 0L
        if (now() - last > 20_000) kick.trySend(Unit)
    }

    // ------------------------------------------------------------------ sesión

    private suspend fun restoreSession() = withContext(io) {
        val refresh = getSetting(KEY_REFRESH) ?: return@withContext
        val email = getSetting(KEY_EMAIL)
        val last = getSetting(KEY_LAST)?.toLongOrNull()
        session = Session("", refresh, 0, getSetting(KEY_USER) ?: "", email ?: "")
        _state.update { it.copy(email = email, status = SyncState.Status.IDLE, lastSync = last) }
    }

    private fun saveSession(s: Session) {
        session = s
        putSetting(KEY_REFRESH, s.refreshToken)
        putSetting(KEY_EMAIL, s.email)
        putSetting(KEY_USER, s.userId)
    }

    /** Devuelve null si ha ido bien, o el mensaje de error. */
    suspend fun signIn(email: String, password: String): String? {
        val r = remote ?: return "La sincronización no está configurada."
        return try {
            val s = r.signIn(email.trim(), password)
            withContext(io) { switchUser(s.userId); saveSession(s) }
            _state.update { it.copy(email = s.email, status = SyncState.Status.IDLE, message = null) }
            kick.trySend(Unit)
            null
        } catch (e: CancellationException) { throw e } catch (e: Throwable) { authMessage(e) }
    }

    /** Crea la cuenta. Devuelve un mensaje para mostrar (error o "confirma el correo"), o null si ya ha entrado. */
    suspend fun signUp(email: String, password: String): String? {
        val r = remote ?: return "La sincronización no está configurada."
        return try {
            val s = r.signUp(email.trim(), password)
                ?: return "Cuenta creada. Abre el enlace que te ha llegado al correo y después pulsa «Entrar»."
            withContext(io) { switchUser(s.userId); saveSession(s) }
            _state.update { it.copy(email = s.email, status = SyncState.Status.IDLE, message = null) }
            kick.trySend(Unit)
            null
        } catch (e: CancellationException) { throw e } catch (e: Throwable) { authMessage(e) }
    }

    suspend fun signOut() = mutex.withLock {
        withContext(io) {
            session = null
            listOf(KEY_REFRESH, KEY_EMAIL, KEY_LAST).forEach(::deleteSetting)
        }
        _state.value = SyncState(configured = remote != null)
    }

    /** Con otra cuenta distinta de la última, este dispositivo vuelve a empezar como si fuera nuevo. */
    private fun switchUser(userId: String) {
        val previous = getSetting(KEY_USER)
        if (previous != null && previous != userId) resetMarks()
    }

    private fun resetMarks() {
        deleteSetting(KEY_INITIALIZED)
        tables.forEach { deleteSetting(pushKey(it)); deleteSetting(pullKey(it)) }
    }

    private suspend fun token(): String {
        val s = session ?: error("sin sesión")
        if (s.accessToken.isNotEmpty() && s.expiresAt - 60_000 > now()) return s.accessToken
        val fresh = try {
            remote!!.refresh(s.refreshToken)
        } catch (e: ApiException) {
            if (e.status in 400..401) throw SessionExpired() else throw e
        }
        withContext(io) { saveSession(fresh) }
        return fresh.accessToken
    }

    // ------------------------------------------------------------------ sincronizar

    suspend fun syncOnce() = mutex.withLock {
        val r = remote ?: return@withLock
        if (session == null) return@withLock
        _state.update { it.copy(status = SyncState.Status.SYNCING, message = null) }
        try {
            val t = token()
            if (withContext(io) { getSetting(KEY_INITIALIZED) } == null) firstSync(r, t)
            push(r, t)
            pull(r, t)
            val at = now()
            withContext(io) { putSetting(KEY_LAST, at.toString()) }
            _state.update { it.copy(status = SyncState.Status.IDLE, lastSync = at, message = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionExpired) {
            // La sesión ya no vale: hay que volver a entrar.
            withContext(io) { session = null; listOf(KEY_REFRESH, KEY_LAST).forEach(::deleteSetting) }
            _state.update { it.copy(status = SyncState.Status.SIGNED_OUT, message = "La sesión ha caducado. Vuelve a entrar.") }
        } catch (e: ApiException) {
            if (e.status == 401) session = session?.copy(accessToken = "")
            _state.update { it.copy(status = SyncState.Status.ERROR, message = "Error del servidor (${e.status}): ${e.message}") }
        } catch (e: Throwable) {
            _state.update { it.copy(status = SyncState.Status.OFFLINE, message = "Sin conexión. Se sincronizará cuando vuelva.") }
        }
    }

    private suspend fun firstSync(r: Remote, token: String) {
        val cloudHasData = tables.any { r.hasRows(token, it.name) }
        withContext(io) {
            if (cloudHasData) {
                // Manda lo que hay en la nube: copia de seguridad y se vacía lo local.
                backup()
                tx.transaction { tables.forEach { driver.execute(null, "DELETE FROM ${it.name}", 0) } }
                driver.notifyListeners(*tables.map { it.name }.toTypedArray())
            }
            resetMarks()
        }
        if (cloudHasData) {
            pull(r, token)
            // Lo recién bajado no hace falta volver a subirlo.
            withContext(io) { tables.forEach { putSetting(pushKey(it), maxUpdatedAt(it).toString()) } }
        }
        withContext(io) { putSetting(KEY_INITIALIZED, "1") }
    }

    private suspend fun push(r: Remote, token: String) {
        for (t in tables) {
            var mark = withContext(io) { getSetting(pushKey(t))?.toLongOrNull() ?: -1L }
            while (true) {
                val rows = withContext(io) { localChanged(t, mark, BATCH) }
                if (rows.isEmpty()) break
                r.upsert(token, t.name, JsonArray(rows))
                mark = rows.maxOf { it["updated_at"]!!.jsonPrimitive.longOrNull ?: 0L }
                withContext(io) { putSetting(pushKey(t), mark.toString()) }
                if (rows.size < BATCH) break
            }
        }
    }

    private suspend fun pull(r: Remote, token: String) {
        val touched = mutableSetOf<String>()
        for (t in tables) {
            val saved = withContext(io) { getSetting(pullKey(t))?.toLongOrNull() ?: -1L }
            // Pequeño margen por si dos escrituras del servidor se cruzaron; aplicar dos veces no hace daño.
            var since = if (saved < 0) -1L else saved - MARGIN
            var mark = saved
            while (true) {
                val rows = r.changedSince(token, t.name, since, BATCH)
                if (rows.isEmpty()) break
                val changed = withContext(io) { applyRemote(t, rows.map { it as JsonObject }) }
                if (changed) touched += t.name
                since = rows.maxOf { (it as JsonObject)["synced_at"]!!.jsonPrimitive.longOrNull ?: 0L }
                mark = maxOf(mark, since)
                if (rows.size < BATCH) break
            }
            if (mark != saved) withContext(io) { putSetting(pullKey(t), mark.toString()) }
        }
        if (touched.isNotEmpty()) driver.notifyListeners(*touched.toTypedArray())
    }

    // ------------------------------------------------------------------ base local

    private fun localChanged(t: SyncTable, after: Long, limit: Int): List<JsonObject> =
        driver.executeQuery(
            null,
            "SELECT ${t.names.joinToString()} FROM ${t.name} WHERE updated_at > ? ORDER BY updated_at LIMIT $limit",
            { c -> QueryResult.Value(buildList { while (c.next().value) add(rowToJson(t, c)) }) },
            1,
        ) { bindLong(0, after) }.value

    private fun rowToJson(t: SyncTable, c: SqlCursor): JsonObject = JsonObject(
        t.columns.mapIndexed { i, (name, type) ->
            name to when (type) {
                Col.TEXT -> c.getString(i)?.let(::JsonPrimitive) ?: JsonNull
                Col.INT -> c.getLong(i)?.let(::JsonPrimitive) ?: JsonNull
            }
        }.toMap(),
    )

    /** Aplica filas del servidor si son más nuevas que las locales. Devuelve si cambió algo. */
    private fun applyRemote(t: SyncTable, rows: List<JsonObject>): Boolean {
        var changed = false
        val placeholders = t.names.joinToString { "?" }
        tx.transaction {
            for (row in rows) {
                val id = row["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val incoming = row["updated_at"]?.jsonPrimitive?.longOrNull ?: 0L
                val local = driver.executeQuery(
                    null, "SELECT updated_at FROM ${t.name} WHERE id = ?",
                    { c -> QueryResult.Value(if (c.next().value) c.getLong(0) else null) }, 1,
                ) { bindString(0, id) }.value
                if (local != null && local >= incoming) continue
                driver.execute(null, "INSERT OR REPLACE INTO ${t.name}(${t.names.joinToString()}) VALUES ($placeholders)", t.columns.size) {
                    t.columns.forEachIndexed { i, (name, type) ->
                        val v: JsonElement? = row[name]
                        val p = (v as? JsonPrimitive)?.takeIf { v !is JsonNull }
                        when (type) {
                            Col.TEXT -> bindString(i, p?.contentOrNull)
                            Col.INT -> bindLong(i, p?.longOrNull)
                        }
                    }
                }
                changed = true
            }
        }
        return changed
    }

    private fun maxUpdatedAt(t: SyncTable): Long = driver.executeQuery(
        null, "SELECT coalesce(max(updated_at), -1) FROM ${t.name}",
        { c -> QueryResult.Value(if (c.next().value) c.getLong(0) ?: -1L else -1L) }, 0,
    ).value

    // Ajustes propios de la sincronización (no pasan por el repositorio para no provocar otra sincronización).
    private fun getSetting(key: String): String? = driver.executeQuery(
        null, "SELECT value FROM setting WHERE key = ?",
        { c -> QueryResult.Value(if (c.next().value) c.getString(0) else null) }, 1,
    ) { bindString(0, key) }.value

    private fun putSetting(key: String, value: String) {
        driver.execute(null, "INSERT OR REPLACE INTO setting(key, value, updated_at) VALUES (?, ?, ?)", 3) {
            bindString(0, key); bindString(1, value); bindLong(2, now())
        }
    }

    private fun deleteSetting(key: String) {
        driver.execute(null, "DELETE FROM setting WHERE key = ?", 1) { bindString(0, key) }
    }

    private fun pushKey(t: SyncTable) = "sync.push.${t.name}"
    private fun pullKey(t: SyncTable) = "sync.pull.${t.name}"

    private companion object {
        const val BATCH = 500
        const val MARGIN = 5_000L
        const val KEY_REFRESH = "sync.refresh_token"
        const val KEY_EMAIL = "sync.email"
        const val KEY_USER = "sync.user_id"
        const val KEY_LAST = "sync.last"
        const val KEY_INITIALIZED = "sync.initialized"
    }
}

private class SessionExpired : Exception()

/** Crea el motor con la configuración de local.properties (o desactivado si no hay claves). */
fun createSyncEngine(driver: SqlDriver, repo: Repository, backup: () -> Unit): SyncEngine {
    val remote = SupabaseConfig.URL.takeIf { it.isNotBlank() && SupabaseConfig.KEY.isNotBlank() }
        ?.let { SupabaseApi(it, SupabaseConfig.KEY) }
    return SyncEngine(driver, repo, remote, backup)
}
