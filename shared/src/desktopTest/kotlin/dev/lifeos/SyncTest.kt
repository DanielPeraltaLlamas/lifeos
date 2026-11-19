package dev.lifeos

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import dev.lifeos.data.EventKind
import dev.lifeos.data.Repository
import dev.lifeos.data.createDriver
import dev.lifeos.data.seedIfNeeded
import dev.lifeos.db.LifeOsDatabase
import dev.lifeos.sync.Remote
import dev.lifeos.sync.SYNC_TABLES
import dev.lifeos.sync.Session
import dev.lifeos.sync.SyncEngine
import dev.lifeos.sync.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Servidor en memoria con las mismas reglas que supabase/migrations/0001_sync.sql. */
private class FakeRemote : Remote {
    val tables = mutableMapOf<String, MutableMap<String, JsonObject>>()
    private var serverClock = 1_000L
    var offline = false

    override suspend fun signIn(email: String, password: String) = Session("tok", "ref", Long.MAX_VALUE, "u1", email)
    override suspend fun signUp(email: String, password: String) = signIn(email, password)
    override suspend fun refresh(refreshToken: String) = signIn("x", "y")

    override suspend fun upsert(token: String, table: String, rows: JsonArray) {
        if (offline) throw java.io.IOException("offline")
        val t = tables.getOrPut(table) { mutableMapOf() }
        rows.forEach { r ->
            r as JsonObject
            val id = r["id"]!!.jsonPrimitive.content
            val old = t[id]
            if (old != null && r["updated_at"]!!.jsonPrimitive.long <= old["updated_at"]!!.jsonPrimitive.long) return@forEach
            t[id] = JsonObject(r + ("synced_at" to JsonPrimitive(++serverClock)) + ("user_id" to JsonPrimitive("u1")))
        }
    }

    override suspend fun changedSince(token: String, table: String, since: Long, limit: Int): JsonArray {
        if (offline) throw java.io.IOException("offline")
        return JsonArray(
            tables[table].orEmpty().values.filter { it["synced_at"]!!.jsonPrimitive.long > since }
                .sortedBy { it["synced_at"]!!.jsonPrimitive.long }.take(limit),
        )
    }

    override suspend fun hasRows(token: String, table: String) = tables[table].orEmpty().isNotEmpty()
}

/** Reloj que avanza 1 ms en cada lectura, compartido por los dos "dispositivos". */
private class TickClock : Clock {
    private var t = 1_800_000_000_000L
    override fun now(): Instant = Instant.fromEpochMilliseconds(++t)
}

private class Device(remote: Remote, clock: Clock) {
    val driver: SqlDriver = createDriver(path = null)
    val repo = Repository(LifeOsDatabase(driver), clock)
    var backups = 0
    val sync = SyncEngine(driver, repo, remote, backup = { backups++ }, clock = clock)

    fun count(table: String, where: String = "1"): Long = driver.executeQuery(
        null, "SELECT count(*) FROM $table WHERE $where", { c -> c.next(); QueryResult.Value(c.getLong(0)!!) }, 0,
    ).value
}

class SyncTest {
    private val mon = LocalDate(2026, 11, 16)

    @Test
    fun specMatchesLocalSchema() {
        val d = createDriver(path = null)
        SYNC_TABLES.forEach { t ->
            val cols = d.executeQuery(null, "PRAGMA table_info(${t.name})", { c ->
                QueryResult.Value(buildList { while (c.next().value) add(c.getString(1)!!) })
            }, 0).value
            assertEquals(cols, t.names, "Columnas de ${t.name}")
        }
        val sql = java.io.File("../supabase/migrations/0001_sync.sql").readText()
        SYNC_TABLES.forEach { t ->
            assertTrue("create table if not exists public.${t.name} (" in sql, "Falta ${t.name} en el SQL")
            t.names.forEach { c -> assertTrue(Regex("\\n  $c (text|bigint)").containsMatchIn(sql.substringAfter("public.${t.name} (")), "Falta ${t.name}.$c en el SQL") }
        }
    }

    @Test
    fun pcUploadsThenPhoneReplacesItsDataAndBothStayInSync() = runTest {
        val remote = FakeRemote()
        val clock = TickClock()
        val pc = Device(remote, clock)
        val phone = Device(remote, clock)
        pc.repo.seedIfNeeded(); phone.repo.seedIfNeeded()
        pc.repo.addEvent("Paradigmas", EventKind.CLASS, mon, 15 * 60, 17 * 60, weekly = true, location = "MD204")
        phone.repo.addTask("Tarea de prueba del móvil")

        // El PC entra primero: la nube está vacía, así que sube todo.
        assertEquals(null, pc.sync.signIn("yo@ejemplo.es", "secreta"))
        pc.sync.syncOnce()
        assertEquals(SyncState.Status.IDLE, pc.sync.state.value.status)
        assertEquals(0, pc.backups)
        assertEquals(1, remote.tables["project"]!!.size)
        assertEquals(3, remote.tables["habit"]!!.size)

        // El móvil entra después: copia de seguridad, descarta lo suyo y baja lo del PC.
        phone.sync.signIn("yo@ejemplo.es", "secreta")
        phone.sync.syncOnce()
        assertEquals(1, phone.backups)
        assertEquals(1L, phone.count("project"))
        assertEquals(3L, phone.count("habit"))
        assertEquals(0L, phone.count("task", "title = 'Tarea de prueba del móvil'"))
        assertEquals("Paradigmas", phone.repo.events.first().single().title)
        assertEquals(pc.repo.projects.first().single().id, phone.repo.projects.first().single().id)

        // Un cambio en el móvil llega al PC.
        val gym = phone.repo.habits.first().first { it.name == "Gimnasio" }
        phone.repo.logHabit(gym.id, mon)
        phone.sync.syncOnce(); pc.sync.syncOnce()
        assertEquals(1L, pc.count("habit_log"))

        // Borrar en el PC llega al móvil (borrado lógico).
        val ev = pc.repo.events.first().single()
        pc.repo.deleteEvent(ev.id)
        pc.sync.syncOnce(); phone.sync.syncOnce()
        assertTrue(phone.repo.events.first().isEmpty())

        // Conflicto: los dos cambian la misma tarea; gana el cambio más reciente.
        val id = pc.repo.addTask("Original")
        pc.sync.syncOnce(); phone.sync.syncOnce()
        phone.repo.updateTask(id, "Cambio del móvil", null, null, null, null, null)
        pc.repo.updateTask(id, "Cambio del PC (más tarde)", null, null, null, null, null)
        phone.sync.syncOnce(); pc.sync.syncOnce(); phone.sync.syncOnce()
        assertEquals("Cambio del PC (más tarde)", pc.repo.tasks.first().first { it.id == id }.title)
        assertEquals("Cambio del PC (más tarde)", phone.repo.tasks.first().first { it.id == id }.title)

        // Volver a sincronizar sin cambios no hace nada raro.
        val before = remote.tables.mapValues { it.value.size }
        pc.sync.syncOnce(); phone.sync.syncOnce()
        assertEquals(before, remote.tables.mapValues { it.value.size })
    }

    @Test
    fun offlineKeepsLocalChangesAndUploadsLater() = runTest {
        val remote = FakeRemote()
        val pc = Device(remote, TickClock())
        pc.sync.signIn("a@b.es", "123456")
        pc.sync.syncOnce()
        remote.offline = true
        pc.repo.capture("Idea sin conexión", dev.lifeos.data.Source.DESKTOP)
        pc.sync.syncOnce()
        assertEquals(SyncState.Status.OFFLINE, pc.sync.state.value.status)
        remote.offline = false
        pc.sync.syncOnce()
        assertEquals(SyncState.Status.IDLE, pc.sync.state.value.status)
        assertEquals("Idea sin conexión", remote.tables["inbox_item"]!!.values.single()["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun signOutStopsSyncing() = runTest {
        val remote = FakeRemote()
        val pc = Device(remote, TickClock())
        pc.sync.signIn("a@b.es", "123456")
        pc.sync.signOut()
        pc.repo.addTask("Después de salir")
        pc.sync.syncOnce()
        assertEquals(SyncState.Status.SIGNED_OUT, pc.sync.state.value.status)
        assertTrue(remote.tables["task"].isNullOrEmpty())
    }
}
