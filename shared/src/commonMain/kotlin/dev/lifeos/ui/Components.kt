package dev.lifeos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tarjeta con cabecera (icono + título + acción a la derecha). */
@Composable
fun Card(
    title: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    padding: Dp = 18.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = color) {
        Column(Modifier.padding(padding)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
                    if (icon != null) {
                        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    action()
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Surface(color = color, shape = RoundedCornerShape(7.dp), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 6.dp),
    )
}

class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** Botón "⋮" con un menú de acciones. */
@Composable
fun OverflowMenu(actions: List<MenuAction>, contentDescription: String = "Más acciones") {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { a ->
                DropdownMenuItem(
                    text = {
                        Text(a.label, color = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    },
                    onClick = { open = false; a.onClick() },
                )
            }
        }
    }
}

/** Fila de tarea: casilla + título + detalle + menú. */
@Composable
fun TaskRow(
    title: String,
    meta: String?,
    done: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    badge: String? = null,
    menu: List<MenuAction> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(end = 4.dp),
        ) {
            Checkbox(checked = done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!meta.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (accent != null) {
                            Dot(accent, 7.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (badge != null) {
                Chip(badge, color = MaterialTheme.colorScheme.primaryContainer)
                Spacer(Modifier.width(4.dp))
            }
            if (menu.isNotEmpty()) OverflowMenu(menu)
        }
    }
}
