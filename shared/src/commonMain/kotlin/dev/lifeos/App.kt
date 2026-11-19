package dev.lifeos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.lifeos.data.Dates
import dev.lifeos.data.Repository
import dev.lifeos.data.Source
import dev.lifeos.data.seedIfNeeded
import dev.lifeos.theme.LifeOsTheme
import dev.lifeos.sync.SyncEngine
import dev.lifeos.ui.LocalRepository
import dev.lifeos.ui.LocalSync
import dev.lifeos.ui.SyncStatusRow
import dev.lifeos.ui.LocalSource
import dev.lifeos.ui.LocalToday
import dev.lifeos.ui.rememberToday
import dev.lifeos.ui.screens.CalendarScreen
import dev.lifeos.ui.screens.HabitsScreen
import dev.lifeos.ui.screens.InboxScreen
import dev.lifeos.ui.screens.TasksScreen
import dev.lifeos.ui.screens.TodayScreen

enum class Screen(val title: String, val short: String, val icon: ImageVector) {
    TODAY("Hoy", "Hoy", Icons.Outlined.WbSunny),
    CALENDAR("Calendario", "Calendario", Icons.Outlined.CalendarMonth),
    TASKS("Tareas y proyectos", "Tareas", Icons.Outlined.TaskAlt),
    HABITS("Hábitos", "Hábitos", Icons.Outlined.Loop),
    INBOX("Bandeja", "Bandeja", Icons.Outlined.Inbox),
}

/** Ancho a partir del cual se usa el diseño de escritorio (menú lateral y tarjetas en fila). */
private val WIDE = 840.dp

@Composable
fun App(repository: Repository, source: Source, initialScreen: Screen = Screen.TODAY, sync: SyncEngine? = null) {
    LaunchedEffect(repository) { repository.seedIfNeeded() }
    val today by rememberToday()

    CompositionLocalProvider(
        LocalRepository provides repository,
        LocalSource provides source,
        LocalToday provides today,
        LocalSync provides sync,
    ) {
        LifeOsTheme {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                Shell(initialScreen)
            }
        }
    }
}

@Composable
private fun Shell(initialScreen: Screen) {
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var tasksSelection by rememberSaveable { mutableStateOf<String?>(null) }
    val inboxCount by LocalRepository.current.inbox.collectAsState(emptyList())
    val badge = inboxCount.size

    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val wide = maxWidth >= WIDE

        @Composable
        fun content(modifier: Modifier) {
            Box(modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = if (wide) 24.dp else 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = 1200.dp)) {
                        when (screen) {
                            Screen.TODAY -> TodayScreen(wide, onOpenHabits = { screen = Screen.HABITS }, onOpenCalendar = { screen = Screen.CALENDAR })
                            Screen.CALENDAR -> CalendarScreen(wide)
                            Screen.TASKS -> TasksScreen(wide, tasksSelection) { tasksSelection = it }
                            Screen.HABITS -> HabitsScreen()
                            Screen.INBOX -> InboxScreen()
                        }
                    }
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Sidebar(screen, badge) { screen = it }
                content(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                NavigationBar {
                    Screen.entries.forEach { s ->
                        NavigationBarItem(
                            selected = s == screen,
                            onClick = { screen = s },
                            icon = {
                                if (s == Screen.INBOX && badge > 0) BadgedBox(badge = { Badge { Text("$badge") } }) { Icon(s.icon, null) }
                                else Icon(s.icon, null)
                            },
                            label = { Text(s.short) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(current: Screen, inboxCount: Int, onSelect: (Screen) -> Unit) {
    val today = LocalToday.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(256.dp).fillMaxHeight(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 18.dp)) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(30.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("L", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Life OS", style = MaterialTheme.typography.titleSmall)
                    Text(Dates.short(today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Screen.entries.forEach { s ->
                NavigationDrawerItem(
                    label = { Text(s.title, maxLines = 1) },
                    icon = { Icon(s.icon, null, Modifier.size(20.dp)) },
                    selected = s == current,
                    onClick = { onSelect(s) },
                    badge = if (s == Screen.INBOX && inboxCount > 0) ({ Text("$inboxCount", style = MaterialTheme.typography.labelMedium) }) else null,
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.weight(1f))
            SyncStatusRow()
        }
    }
}
