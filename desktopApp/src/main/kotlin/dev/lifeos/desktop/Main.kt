package dev.lifeos.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lifeos.App
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.createDatabase

fun main() {
    val repository = Repository(createDatabase())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Life OS",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            App(repository, Source.DESKTOP)
        }
    }
}
