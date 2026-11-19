package dev.lifeos

import app.cash.sqldelight.db.QueryResult
import dev.lifeos.data.Repository
import dev.lifeos.data.createDriver
import dev.lifeos.data.watchExternalChanges
import dev.lifeos.db.LifeOsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** La app y el agente abren la misma base: lo que escribe uno lo ve el otro. */
class ExternalChangesTest {
    @Test
    fun otherProcessWritesAreSeen() = runBlocking {
        val path = Files.createTempDirectory("lifeos-ext").resolve("lifeos.db")
        val agentDriver = createDriver(path)
        val agent = Repository(LifeOsDatabase(agentDriver))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        watchExternalChanges(agentDriver, scope, path, periodMs = 100)
        // Como el agente: un flujo que se queda escuchando (first() volvería a consultar y no probaría nada).
        val live = agent.activeFocus.stateIn(scope, SharingStarted.Eagerly, null)
        Thread.sleep(300)
        assertEquals(null, live.value)

        val dv = { agentDriver.executeQuery(null, "PRAGMA data_version", { c -> c.next(); QueryResult.Value(c.getLong(0)) }, 0).value }
        println("data_version antes: ${dv()}")

        // Otro proceso de verdad (como la app o una edición a mano).
        val now = System.currentTimeMillis()
        ProcessBuilder("sqlite3", path.toString(),
            "insert into focus_session(id,title,started_at,planned_ms,block_index,block_total,created_at,updated_at) values('x','Desde la app',$now,3000000,1,1,$now,$now)")
            .inheritIO().start().waitFor()
        Thread.sleep(500)
        println("data_version después: ${dv()}")

        val seen = withTimeout(5_000) { live.first { it != null } }
        assertEquals("Desde la app", seen!!.title)
        scope.cancel()
    }
}
