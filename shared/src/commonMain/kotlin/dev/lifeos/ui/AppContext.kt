package dev.lifeos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.lifeos.data.Dates
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

val LocalRepository = compositionLocalOf<Repository> { error("Sin repositorio") }
val LocalSource = compositionLocalOf { Source.DESKTOP }
val LocalToday = compositionLocalOf<LocalDate> { error("Sin fecha") }
val LocalPrefs = compositionLocalOf { dev.lifeos.data.Prefs() }

/** Lanza una escritura en la base de datos sin bloquear la interfaz. */
class Actions(private val scope: CoroutineScope, val repo: Repository) {
    operator fun invoke(block: suspend Repository.() -> Unit) {
        scope.launch { repo.block() }
    }
}

@Composable
fun rememberActions(): Actions {
    val scope = rememberCoroutineScope()
    val repo = LocalRepository.current
    return remember(scope, repo) { Actions(scope, repo) }
}

@Composable
fun rememberToday(): State<LocalDate> {
    val flow = remember { Dates.todayFlow() }
    return flow.collectAsState(Dates.today())
}
