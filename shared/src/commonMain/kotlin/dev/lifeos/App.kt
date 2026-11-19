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
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
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
import dev.lifeos.ui.screens.CloseDayScreen
import dev.lifeos.ui.screens.EvolutionScreen
import dev.lifeos.ui.screens.NotesScreen
import dev.lifeos.ui.screens.ReviewScreen
import dev.lifeos.ui.screens.SettingsScreen
import dev.lifeos.ui.screens.FocusHost
import dev.lifeos.ui.screens.HabitsScreen
import dev.lifeos.ui.screens.InboxScreen
import dev.lifeos.ui.screens.TasksScreen
import dev.lifeos.ui.screens.TodayScreen

enum class Screen(val title: String, val short: String, val icon: ImageVector) {
    TODAY("Hoy", "Hoy", Icons.Outlined.WbSunny),
    CALENDAR("Calendario", "Calendario", Icons.Outlined.CalendarMonth),
    TASKS("Tareas y proyectos", "Tareas", Icons.Outlined.TaskAlt),
    NOTES("Notas", "Notas", Icons.AutoMirrored.Outlined.StickyNote2),
    HABITS("Hábitos", "Hábitos", Icons.Outlined.Loop),
    INBOX("Bandeja", "Bandeja", Icons.Outlined.Inbox),
    EVOLUTION("Evolución", "Evolución", Icons.Outlined.Insights),
    SETTINGS("Ajustes", "Ajustes", Icons.Outlined.Settings),
    /** Solo en el móvil: lista con lo que no cabe en la barra de abajo. */
    MORE("Más", "Más", Icons.Outlined.Menu),
}

/** Rituales: pantallas completas, sin menú. */
enum class Ritual(val title: String, val icon: ImageVector) {
    CLOSE("Cierre del día", Icons.Outlined.Bedtime),
    REVIEW("Revisión semanal", Icons.Outlined.EventRepeat),
}

private val SIDEBAR = listOf(Screen.TODAY, Screen.CALENDAR, Screen.TASKS, Screen.NOTES, Screen.HABITS, Screen.INBOX, Screen.EVOLUTION)
private val BOTTOM_BAR = listOf(Screen.TODAY, Screen.CALENDAR, Screen.TASKS, Screen.HABITS, Screen.MORE)

/** Ancho a partir del cual se usa el diseño de escritorio (menú lateral y tarjetas en fila). */
private val WIDE = 840.dp

@Composable
fun App(
    repository: Repository,
    source: Source,
    initialScreen: Screen = Screen.TODAY,
    sync: SyncEngine? = null,
    initialRitual: Ritual? = null,
    /** Tarjetas propias de la plataforma bajo «Lo primero» (en Android, la del bloqueo). */
    todayExtras: (@Composable () -> Unit)? = null,
) {
    LaunchedEffect(repository) { repository.seedIfNeeded() }
    val today by rememberToday()
    val prefs by repository.prefs.collectAsState(dev.lifeos.data.Prefs())

    CompositionLocalProvider(
        LocalRepository provides repository,
        LocalSource provides source,
        LocalToday provides today,
        LocalSync provides sync,
        dev.lifeos.ui.LocalPrefs provides prefs,
    ) {
        LifeOsTheme {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                FocusHost { Shell(initialScreen, todayExtras, initialRitual) }
            }
        }
    }
}

@Composable
private fun Shell(initialScreen: Screen, todayExtras: (@Composable () -> Unit)?, initialRitual: Ritual? = null) {
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var ritual by rememberSaveable { mutableStateOf(initialRitual) }
    var tasksSelection by rememberSaveable { mutableStateOf<String?>(null) }
    val inboxCount by LocalRepository.current.inbox.collectAsState(emptyList())
    val badge = inboxCount.size

    when (ritual) {
        Ritual.CLOSE -> { CloseDayScreen(onClose = { ritual = null }); return }
        Ritual.REVIEW -> { ReviewScreen(onClose = { ritual = null }); return }
        null -> {}
    }

    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val wide = maxWidth >= WIDE
        // MORE solo existe en el móvil; en el ordenador se vuelve a Hoy.
        if (wide && screen == Screen.MORE) screen = Screen.TODAY

        @Composable
        fun content(modifier: Modifier) {
            Box(modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = if (wide) 24.dp else 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = 1200.dp)) {
                        when (screen) {
                            Screen.TODAY -> TodayScreen(
                                wide,
                                onOpenHabits = { screen = Screen.HABITS },
                                onOpenCalendar = { screen = Screen.CALENDAR },
                                onOpenRitual = { ritual = it },
                                extras = todayExtras,
                            )
                            Screen.CALENDAR -> CalendarScreen(wide)
                            Screen.TASKS -> TasksScreen(wide, tasksSelection) { tasksSelection = it }
                            Screen.NOTES -> NotesScreen(wide)
                            Screen.HABITS -> HabitsScreen()
                            Screen.INBOX -> InboxScreen()
                            Screen.EVOLUTION -> EvolutionScreen(wide)
                            Screen.SETTINGS -> SettingsScreen(wide)
                            Screen.MORE -> MoreScreen(badge, onScreen = { screen = it }, onRitual = { ritual = it })
                        }
                    }
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Sidebar(screen, badge, onSelect = { screen = it }, onRitual = { ritual = it })
                content(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                NavigationBar {
                    BOTTOM_BAR.forEach { s ->
                        val selected = s == screen || (s == Screen.MORE && screen !in BOTTOM_BAR)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { screen = s },
                            icon = {
                                if (s == Screen.MORE && badge > 0) BadgedBox(badge = { Badge { Text("$badge") } }) { Icon(s.icon, null) }
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

/** Móvil: lo que no cabe en la barra de abajo. */
@Composable
private fun MoreScreen(inboxCount: Int, onScreen: (Screen) -> Unit, onRitual: (Ritual) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Más", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 10.dp))
        @Composable
        fun item(title: String, icon: ImageVector, badge: Int = 0, onClick: () -> Unit) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(14.dp))
                    Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (badge > 0) Badge { Text("$badge") }
                }
            }
        }
        item(Screen.INBOX.title, Screen.INBOX.icon, inboxCount) { onScreen(Screen.INBOX) }
        item(Screen.NOTES.title, Screen.NOTES.icon) { onScreen(Screen.NOTES) }
        item(Screen.EVOLUTION.title, Screen.EVOLUTION.icon) { onScreen(Screen.EVOLUTION) }
        Text("Rituales", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Ritual.entries.forEach { r -> item(r.title, r.icon) { onRitual(r) } }
        Spacer(Modifier.height(12.dp))
        item(Screen.SETTINGS.title, Screen.SETTINGS.icon) { onScreen(Screen.SETTINGS) }
    }
}

@Composable
private fun Sidebar(current: Screen, inboxCount: Int, onSelect: (Screen) -> Unit, onRitual: (Ritual) -> Unit) {
    val today = LocalToday.current
    val itemColors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(256.dp).fillMaxHeight(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp).verticalScroll(rememberScrollState())) {
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
            SIDEBAR.forEach { s ->
                NavigationDrawerItem(
                    label = { Text(s.title, maxLines = 1) },
                    icon = { Icon(s.icon, null, Modifier.size(20.dp)) },
                    selected = s == current,
                    onClick = { onSelect(s) },
                    badge = if (s == Screen.INBOX && inboxCount > 0) ({ Text("$inboxCount", style = MaterialTheme.typography.labelMedium) }) else null,
                    colors = itemColors,
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                "RITUALES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
            )
            Ritual.entries.forEach { r ->
                NavigationDrawerItem(
                    label = { Text(r.title, maxLines = 1) },
                    icon = { Icon(r.icon, null, Modifier.size(20.dp)) },
                    selected = false,
                    onClick = { onRitual(r) },
                    colors = itemColors,
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(16.dp))
            NavigationDrawerItem(
                label = { Text(Screen.SETTINGS.title) },
                icon = { Icon(Screen.SETTINGS.icon, null, Modifier.size(20.dp)) },
                selected = current == Screen.SETTINGS,
                onClick = { onSelect(Screen.SETTINGS) },
                colors = itemColors,
                modifier = Modifier.height(44.dp),
            )
            SyncStatusRow()
        }
    }
}
