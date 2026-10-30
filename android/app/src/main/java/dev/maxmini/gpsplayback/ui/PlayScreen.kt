package dev.maxmini.gpsplayback.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.AppStore
import dev.maxmini.gpsplayback.PlaybackStatus
import dev.maxmini.gpsplayback.R
import dev.maxmini.gpsplayback.core.geo.bearingAtDistance
import dev.maxmini.gpsplayback.core.geo.cumulativeDistances
import dev.maxmini.gpsplayback.core.geo.pointAtDistance
import dev.maxmini.gpsplayback.core.model.EditableRoute
import dev.maxmini.gpsplayback.core.model.LatLon
import dev.maxmini.gpsplayback.core.model.PlayerState
import dev.maxmini.gpsplayback.core.playback.PlaybackEngine
import dev.maxmini.gpsplayback.playback.PlaybackService
import dev.maxmini.gpsplayback.ui.components.LiveState
import dev.maxmini.gpsplayback.ui.components.RouteBullet
import dev.maxmini.gpsplayback.ui.components.SegmentedChoice
import dev.maxmini.gpsplayback.ui.components.StripScrubber
import dev.maxmini.gpsplayback.ui.components.TransportBar
import dev.maxmini.gpsplayback.ui.components.liveStateOf
import dev.maxmini.gpsplayback.ui.theme.AppTokens
import dev.maxmini.gpsplayback.ui.theme.AppType
import dev.maxmini.gpsplayback.ui.theme.RouteStyle
import dev.maxmini.gpsplayback.ui.theme.rememberRouteStyle
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private val MULTIPLIERS = listOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0)

/** Height of the collapsed sheet: route header, progress, scrubber, next stop and transport. */
private val SHEET_PEEK = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(onNeedRoute: () -> Unit) {
    val routes by AppStore.routes.collectAsState()
    val player by AppStore.player.collectAsState()
    val status by AppStore.status.collectAsState()

    val route = player.routeId?.let { routes[it] } ?: routes.values.firstOrNull()
    if (route == null) {
        EmptyRoutes("No route staged", "Load a GTFS feed and stage a trip to start streaming mock fixes.", onNeedRoute)
        return
    }

    val cum = remember(route.waypoints) { cumulativeDistances(route.waypoints) }
    val total = cum.last()
    val position = pointAtDistance(route.waypoints, cum, player.progressMeters)
    val bearing = bearingAtDistance(route.waypoints, cum, player.progressMeters)
    val stopPoints = remember(route.stops) { route.stops.map { it.point } }
    val stopDistances = remember(route.waypoints, route.stops) {
        PlaybackEngine(route.waypoints, route.stops).stopDistances
    }
    val live = liveStateOf(status, player)
    val style = rememberRouteStyle(route.color, route.textColor)

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(),
        sheetPeekHeight = SHEET_PEEK,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 8.dp,
        sheetContent = {
            PlaySheet(route, routes.values, style, player, status, live, total, position, stopDistances)
        },
    ) {
        // The map stops just under the sheet's rounded top, so fitting the route and
        // auto-pan center on the visible part.
        Box(Modifier.fillMaxSize().padding(bottom = SHEET_PEEK - 20.dp)) {
            RouteMap(
                routeKey = route.id,
                waypoints = route.waypoints,
                stops = stopPoints,
                position = position,
                bearing = bearing,
                autoPan = player.autoPan,
                routeColor = style.fill.toArgb(),
                live = live == LiveState.Mocking || live == LiveState.Holding,
                dark = AppTokens.colors.dark,
                modifier = Modifier.fillMaxSize(),
            )
            SmallFloatingActionButton(
                onClick = { AppStore.setPlayer { it.copy(autoPan = !it.autoPan) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                containerColor = if (player.autoPan) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (player.autoPan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(painterResource(R.drawable.ic_follow), contentDescription = if (player.autoPan) "Stop following" else "Follow position")
            }
        }
    }

}

@Composable
private fun PlaySheet(
    route: EditableRoute,
    routes: Collection<EditableRoute>,
    style: RouteStyle,
    player: PlayerState,
    status: PlaybackStatus,
    live: LiveState,
    total: Double,
    position: LatLon,
    stopDistances: DoubleArray,
) {
    val context = LocalContext.current
    fun seek(meters: Double, persist: Boolean = true) =
        AppStore.setPlayer(persist) { it.copy(progressMeters = meters.coerceIn(0.0, total)) }
    val hasStops = stopDistances.isNotEmpty()
    val prev = PlaybackEngine.previousStopBefore(stopDistances, player.progressMeters)
    val next = PlaybackEngine.nextStopAfter(stopDistances, player.progressMeters)
    val nextIndex = PlaybackEngine.nextStopIndex(stopDistances, player.progressMeters)
    val fraction = if (total > 0) (player.progressMeters / total).coerceIn(0.0, 1.0) else 0.0
    val kmh = PlaybackEngine.effectiveSpeed(player) * 3.6

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: which route, how fast.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (routes.size > 1) {
                RoutePicker(route, routes, onPick = { id ->
                    if (id != route.id) {
                        PlaybackService.stop(context)
                        AppStore.setPlayer { it.copy(routeId = id, progressMeters = 0.0, playing = false) }
                    }
                }, Modifier.weight(1f))
            } else {
                RouteBullet(route.shortName, style)
                Column(Modifier.weight(1f)) {
                    Text(routeTitle(route), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Muted(speedSummary(kmh, player, hasStops))
                }
            }
        }

        // Where along the route.
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("%.2f".format(player.progressMeters / 1000), style = AppType.dataLarge)
            Text("of %.2f km".format(total / 1000), Modifier.padding(bottom = 4.dp), style = AppType.data, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f))
            Text("%.0f%%".format(fraction * 100), Modifier.padding(bottom = 4.dp), style = AppType.data, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StripScrubber(
            progressMeters = player.progressMeters,
            totalMeters = total,
            stopDistances = stopDistances,
            routeColor = style.fill,
            live = live == LiveState.Mocking || live == LiveState.Holding,
            onSeek = { seek(it, persist = false) },
            onSeekFinished = { AppStore.save() },
        )
        if (nextIndex != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Next")
                Text(
                    route.stops.getOrNull(nextIndex)?.name ?: "Stop ${nextIndex + 1}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(distanceLabel(stopDistances[nextIndex] - player.progressMeters), style = AppType.data, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TransportBar(
            playing = player.playing,
            hasStops = hasStops,
            canPrev = prev != null,
            canNext = next != null,
            mocking = status.serviceRunning,
            onPrev = { prev?.let { seek(it) } },
            onToggle = { if (player.playing) PlaybackService.pause(context) else PlaybackService.play(context) },
            onNext = { next?.let { seek(it) } },
            onStop = { PlaybackService.stop(context) },
        )
        StatusLine(status)

        // Expanded part of the sheet.
        HorizontalDivider()
        SpeedGroup(player, kmh)
        HorizontalDivider()
        StopsGroup(player, hasStops)
        HorizontalDivider()
        JitterGroup(player)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Position", Modifier.weight(1f))
                Muted("Follow  ")
                Switch(checked = player.autoPan, onCheckedChange = { v -> AppStore.setPlayer { it.copy(autoPan = v) } })
            }
            Text(
                "%.6f, %.6f · %d pts".format(position.lat, position.lon, route.waypoints.size),
                style = AppType.data,
            )
        }
    }
}

private fun speedSummary(kmh: Double, player: PlayerState, hasStops: Boolean): String {
    val parts = mutableListOf("%.0f km/h".format(kmh), fmtMultiplier(player.speedMultiplier))
    if (hasStops && player.stopAtStops) parts += "stopping ${player.dwellSec} s"
    return parts.joinToString(" · ")
}

private fun distanceLabel(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters.coerceAtLeast(0.0))

@Composable
private fun SpeedGroup(player: PlayerState, kmh: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Speed")
        ValueRow("Base speed", "%.1f m/s · %.0f km/h".format(player.baseSpeedMps, kmh))
        Slider(
            value = player.baseSpeedMps.toFloat(),
            onValueChange = { v -> AppStore.setPlayer(persist = false) { it.copy(baseSpeedMps = v.toDouble()) } },
            onValueChangeFinished = { AppStore.save() },
            valueRange = 1f..40f,
        )
        SegmentedChoice(
            options = MULTIPLIERS,
            selected = player.speedMultiplier,
            label = ::fmtMultiplier,
            onSelect = { m -> AppStore.setPlayer { it.copy(speedMultiplier = m) } },
        )
    }
}

@Composable
private fun StopsGroup(player: PlayerState, hasStops: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Stop at each stop", Modifier.weight(1f))
            Switch(
                enabled = hasStops,
                checked = hasStops && player.stopAtStops,
                onCheckedChange = { v -> AppStore.setPlayer { it.copy(stopAtStops = v) } },
            )
        }
        if (!hasStops) {
            Muted("No stops for this route. Re-stage the trip from Load to stop along the way.")
        } else if (player.stopAtStops) {
            ValueRow("Time at each stop", "${player.dwellSec} s")
            Slider(
                value = player.dwellSec.toFloat(),
                onValueChange = { v -> AppStore.setPlayer(persist = false) { it.copy(dwellSec = (v / 5).roundToInt() * 5) } },
                onValueChangeFinished = { AppStore.save() },
                valueRange = 0f..120f,
                steps = 120 / 5 - 1,
            )
            Muted("Scaled by the speed multiplier.")
        }
    }
}

@Composable
private fun JitterGroup(player: PlayerState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("GPS jitter")
                Muted("Slowly drifting receiver error")
            }
            Switch(
                checked = player.jitter.enabled,
                onCheckedChange = { v -> AppStore.setPlayer { it.copy(jitter = it.jitter.copy(enabled = v)) } },
            )
        }
        if (player.jitter.enabled) {
            ValueRow("σ per axis", "%.0f m".format(player.jitter.sigmaMeters))
            Slider(
                value = player.jitter.sigmaMeters.toFloat(),
                onValueChange = { v ->
                    AppStore.setPlayer(persist = false) { it.copy(jitter = it.jitter.copy(sigmaMeters = v.toDouble())) }
                },
                onValueChangeFinished = { AppStore.save() },
                valueRange = 1f..30f,
            )
        }
    }
}

private fun fmtMultiplier(m: Double) = if (m == 0.5) "½×" else if (m % 1.0 == 0.0) "${m.toInt()}×" else "$m×"

@Composable
private fun StatusLine(status: PlaybackStatus) {
    val error = status.lastError
    val lastSentAt = status.lastSentAt
    when {
        error != null -> ErrorText(error)
        status.serviceRunning && lastSentAt != null ->
            Muted("Last fix ${DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(lastSentAt))}")
        status.serviceRunning -> Muted("Starting…")
        else -> Muted("Not mocking. The device is using its real location.")
    }
}
