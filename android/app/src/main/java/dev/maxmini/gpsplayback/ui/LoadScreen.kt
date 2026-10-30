package dev.maxmini.gpsplayback.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.AppStore
import dev.maxmini.gpsplayback.core.geo.polylineLengthMeters
import dev.maxmini.gpsplayback.core.gtfs.GtfsParser
import dev.maxmini.gpsplayback.core.model.EditableRoute
import dev.maxmini.gpsplayback.core.model.GtfsData
import dev.maxmini.gpsplayback.core.model.GtfsRoute
import dev.maxmini.gpsplayback.core.model.GtfsTrip
import dev.maxmini.gpsplayback.ui.components.RouteBullet
import dev.maxmini.gpsplayback.ui.theme.AppTokens
import dev.maxmini.gpsplayback.ui.theme.AppType
import dev.maxmini.gpsplayback.ui.theme.rememberRouteStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }

/** One row per route: its trips that have a usable shape. */
private data class RouteRow(val route: GtfsRoute, val title: String, val trips: List<GtfsTrip>)

/** The long name next to the bullet; falls back to the short name or id when there isn't one. */
private fun routeTitle(r: GtfsRoute): String = r.longName ?: r.shortName ?: r.routeId

private fun hasShape(gtfs: GtfsData, t: GtfsTrip) = t.shapeId?.let { gtfs.shapes[it] } != null

private fun rows(gtfs: GtfsData): List<RouteRow> {
    val byRoute = gtfs.trips.filter { hasShape(gtfs, it) }.groupBy { it.routeId }
    return gtfs.routes.mapNotNull { r ->
        val trips = byRoute[r.routeId] ?: return@mapNotNull null
        // Many trips share one shape; list each distinct geometry once.
        RouteRow(r, routeTitle(r), trips.distinctBy { it.shapeId })
    }
}

@Composable
fun LoadScreen(onStaged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gtfs by AppStore.gtfs.collectAsState()
    val staged by AppStore.routes.collectAsState()
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    var staging by remember { mutableStateOf(setOf<String>()) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Keep read access so staging can re-read the zip later.
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val open: () -> InputStream = {
            context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
        }
        loading = true
        message = null
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { GtfsParser.parse(open, feedName = displayName(context, uri)) }
                AppStore.setGtfs(data, open)
            } catch (e: Exception) {
                message = "Couldn't read feed: ${e.message ?: e}"
            } finally {
                loading = false
            }
        }
    }
    val openPicker = {
        pick.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    }

    val allRows = remember(gtfs) { gtfs?.let(::rows) ?: emptyList() }
    val shown = remember(allRows, filter) {
        if (filter.isBlank()) allRows
        else allRows.filter { row ->
            row.title.contains(filter, ignoreCase = true) || row.route.shortName?.contains(filter, ignoreCase = true) == true
        }
    }

    val feed = gtfs
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (feed == null) {
                    EmptyFeed(loading, onOpen = openPicker)
                } else {
                    FlatCard {
                        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(feed.feedName ?: "Feed", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${feed.routes.size} routes · ${feed.trips.size} trips",
                                    style = AppType.data,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (loading) CircularProgressIndicator(Modifier.size(24.dp))
                            else TextButton(onClick = openPicker) { Text("Replace") }
                        }
                    }
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = { Text("Filter routes") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                message?.let { Muted(it) }
            }
            if (feed != null) {
                if (shown.isEmpty()) Muted("No routes match.", Modifier.padding(horizontal = 16.dp))
                HorizontalDivider()
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(shown, key = { it.route.routeId }) { row ->
                        val open = expanded == row.route.routeId
                        val stagedHere = row.trips.count { it.tripId in staged }
                        RouteRowItem(row, open, stagedHere) { expanded = if (open) null else row.route.routeId }
                        if (open) {
                            row.trips.forEach { trip ->
                                TripRow(
                                    feed = feed,
                                    trip = trip,
                                    isStaged = trip.tripId in staged,
                                    isStaging = trip.tripId in staging,
                                ) {
                                    staging = staging + trip.tripId
                                    scope.launch {
                                        message = AppStore.stageTrip(trip.tripId)
                                        staging = staging - trip.tripId
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        if (staged.isNotEmpty()) {
            StagedBar(staged.values, onStaged, Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
    }
}

@Composable
private fun EmptyFeed(loading: Boolean, onOpen: () -> Unit) {
    FlatCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Open a GTFS feed", style = MaterialTheme.typography.titleMedium)
            Muted("Pick a GTFS .zip, then stage the trips you want to replay. Routes without shapes.txt get a line built from their stops.")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !loading, onClick = onOpen) { Text("Open GTFS zip") }
                if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun RouteRowItem(row: RouteRow, open: Boolean, stagedCount: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (open) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterStart) {
            RouteBullet(row.route.shortName ?: row.route.routeId, rememberRouteStyle(row.route.color, row.route.textColor))
        }
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val shapes = if (row.trips.size == 1) "1 shape" else "${row.trips.size} shapes"
            Muted(if (stagedCount > 0) "$shapes · $stagedCount staged" else shapes)
        }
        Text(if (open) "⌃" else "›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TripRow(feed: GtfsData, trip: GtfsTrip, isStaged: Boolean, isStaging: Boolean, onStage: () -> Unit) {
    val shapeId = trip.shapeId
    val km = remember(feed, shapeId) {
        shapeId?.let { feed.shapes[it] }?.let { polylineLengthMeters(it.points) / 1000 }
    }
    val fromStops = shapeId != null && GtfsParser.isSynthesized(shapeId)
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 72.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(trip.headsign ?: trip.tripId, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(km?.let { "%.2f km".format(it) }, "from stops".takeIf { fromStops }).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = AppType.data, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            isStaged -> Text("✓ Staged", style = MaterialTheme.typography.labelLarge, color = AppTokens.colors.ok)
            isStaging -> CircularProgressIndicator(Modifier.size(24.dp))
            else -> FilledTonalButton(onClick = onStage, contentPadding = ButtonDefaults.TextButtonContentPadding) { Text("Stage") }
        }
    }
}

/** Dark bar pinned to the bottom once something is staged, leading on to Edit. */
@Composable
private fun StagedBar(staged: Collection<EditableRoute>, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (staged.size == 1) "1 route staged" else "${staged.size} routes staged",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    contentColor = MaterialTheme.colorScheme.inverseSurface,
                ),
            ) { Text("Edit →") }
        }
    }
}
