package dev.lifeos.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.lifeos.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Life OS") {
        App()
    }
}
