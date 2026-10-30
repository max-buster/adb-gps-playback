package dev.maxmini.gpsplayback.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.AppStore
import dev.maxmini.gpsplayback.R
import dev.maxmini.gpsplayback.core.edit.RouteEdit
import dev.maxmini.gpsplayback.core.geo.polylineLengthMeters
import dev.maxmini.gpsplayback.playback.PlaybackService
import dev.maxmini.gpsplayback.ui.map.RouteMap
import dev.maxmini.gpsplayback.ui.theme.AppTokens
import dev.maxmini.gpsplayback.ui.theme.AppType
import dev.maxmini.gpsplayback.ui.theme.rememberRouteStyle

@Composable
fun EditScreen(onNeedRoute: () -> Unit, onPlay: () -> Unit) {
    val context = LocalContext.current
    val routes by AppStore.routes.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf(AppStore.player.value.routeId) }
    val route = selectedId?.let { routes[it] } ?: routes.values.firstOrNull()

    if (route == null) {
        EmptyRoutes("No staged routes yet", "Load a GTFS feed and stage a few trips first.", onNeedRoute)
        return
    }

    val stopPoints = remember(route.stops) { route.stops.map { it.point } }
    val edited = route.waypoints != route.originalPoints
    val style = rememberRouteStyle(route.color, route.textColor)

    Box(Modifier.fillMaxSize()) {
        RouteMap(
            routeKey = route.id,
            waypoints = route.waypoints,
            stops = stopPoints,
            editable = true,
            routeColor = style.fill.toArgb(),
            ghost = if (edited) route.originalPoints else emptyList(),
            dark = AppTokens.colors.dark,
            onEdit = { AppStore.editRoute(route.id, it) },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoutePicker(route, routes.values, onPick = { selectedId = it })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MapChip("%.2f km · %d pts".format(polylineLengthMeters(route.waypoints) / 1000, route.waypoints.size))
                if (edited) EditedTag()
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 3.dp,
            ) {
                Text(
                    "Drag a point · tap the line to add · long-press to delete",
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            FlatCard {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(enabled = edited, onClick = { AppStore.editRoute(route.id, RouteEdit.Reset) }) { Text("Reset") }
                    OutlinedButton(
                        onClick = {
                            if (AppStore.player.value.routeId == route.id) PlaybackService.stop(context)
                            AppStore.removeRoute(route.id)
                            selectedId = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) { Text("Remove") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        AppStore.setPlayer { it.copy(routeId = route.id, progressMeters = 0.0, playing = false) }
                        onPlay()
                    }) {
                        Icon(painterResource(R.drawable.ic_play), contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Play")
                    }
                }
            }
        }
    }
}

/** Small floating label over the map. */
@Composable
internal fun MapChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = AppType.data)
    }
}

@Composable
private fun EditedTag() {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            "EDITED",
            Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = AppType.label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Shown on Edit and Play when nothing is staged. */
@Composable
internal fun EmptyRoutes(title: String, body: String, onLoad: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        FlatCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Muted(body)
                Button(onClick = onLoad) { Text("Load a feed →") }
            }
        }
    }
}
