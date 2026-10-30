package dev.maxmini.gpsplayback.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.core.model.EditableRoute
import dev.maxmini.gpsplayback.ui.components.RouteBullet
import dev.maxmini.gpsplayback.ui.theme.AppType
import dev.maxmini.gpsplayback.ui.theme.rememberRouteStyle

/** Flat card with a 1dp outline: the default container for grouped content. */
@Composable
internal fun FlatCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border,
    ) { content() }
}

@Composable
internal fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlatCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(title)
            content()
        }
    }
}

/** Uppercase group label, e.g. "SPEED". */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = AppType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

/** Label / monospaced value row, e.g. "Base speed      15.0 m/s". */
@Composable
internal fun ValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = AppType.dataStrong)
    }
}

/** Floating pill showing the current route's bullet and label; tap to pick another staged route. */
@Composable
internal fun RoutePicker(
    current: EditableRoute,
    routes: Collection<EditableRoute>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 3.dp,
        ) {
            Row(
                Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp).widthIn(max = 320.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RouteBullet(current.shortName, rememberRouteStyle(current.color, current.textColor), small = true)
                Text(
                    routeTitle(current),
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (routes.size > 1) Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            routes.forEach { r ->
                DropdownMenuItem(
                    leadingIcon = { RouteBullet(r.shortName, rememberRouteStyle(r.color, r.textColor), small = true) },
                    text = { Text(routeTitle(r), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { open = false; onPick(r.id) },
                )
            }
        }
    }
}

/** The route's label without the short name, which the bullet already shows. */
internal fun routeTitle(route: EditableRoute): String {
    val short = route.shortName ?: return route.label
    return route.label.removePrefix("$short — ").ifEmpty { route.label }
}
