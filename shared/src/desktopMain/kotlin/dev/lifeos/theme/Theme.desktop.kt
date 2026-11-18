package dev.lifeos.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
actual fun platformColorScheme(): ColorScheme {
    val dark = isSystemInDarkTheme()
    val flow = remember { Caelestia.changes() }
    val initial = remember { Caelestia.read() }
    val scheme by flow.collectAsState(initial)
    return scheme ?: if (dark) darkColorScheme() else lightColorScheme()
}
