package dev.maxmini.gpsplayback.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.ui.theme.AppTokens
import dev.maxmini.gpsplayback.ui.theme.AppType
import kotlin.math.abs

/**
 * The route as a line diagram: a rail with a tick for each stop and the
 * current position as a puck. Drag anywhere to scrub; tap near a stop to jump
 * to it. Without stops it shows quarter-distance ticks instead.
 *
 * [onSeek] is called continuously while dragging, [onSeekFinished] once at the end.
 */
@Composable
fun StripScrubber(
    progressMeters: Double,
    totalMeters: Double,
    stopDistances: DoubleArray,
    routeColor: Color,
    live: Boolean,
    onSeek: (Double) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seek by rememberUpdatedState(onSeek)
    val finished by rememberUpdatedState(onSeekFinished)
    val stops by rememberUpdatedState(stopDistances)
    val total by rememberUpdatedState(totalMeters)

    val scheme = MaterialTheme.colorScheme
    val app = AppTokens.colors
    val inset = with(LocalDensity.current) { 14.dp.toPx() }
    val snap = with(LocalDensity.current) { 18.dp.toPx() }
    val ticks = if (stopDistances.isNotEmpty() || totalMeters <= 0) stopDistances
    else doubleArrayOf(0.25, 0.5, 0.75).map { it * totalMeters }.toDoubleArray()

    fun metersAt(x: Float, width: Int): Double {
        val usable = (width - 2 * inset).coerceAtLeast(1f)
        return ((x - inset) / usable).coerceIn(0f, 1f) * total
    }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val usable = (size.width - 2 * inset).coerceAtLeast(1f)
                        val t = total
                        // Jump to the nearest stop if the tap is close to one.
                        val nearest = stops.minByOrNull { abs(inset + (it / t).toFloat() * usable - pos.x) }
                        val target = if (nearest != null && t > 0 &&
                            abs(inset + (nearest / t).toFloat() * usable - pos.x) <= snap
                        ) nearest else metersAt(pos.x, size.width)
                        seek(target)
                        finished()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { finished() },
                        onDragCancel = { finished() },
                    ) { change, _ ->
                        change.consume()
                        seek(metersAt(change.position.x, size.width))
                    }
                },
        ) {
            val y = size.height / 2
            val usable = size.width - 2 * inset
            fun xOf(m: Double) = inset + if (totalMeters > 0) (m / totalMeters).toFloat().coerceIn(0f, 1f) * usable else 0f
            val px = xOf(progressMeters)
            val rail = 6.dp.toPx()

            drawLine(scheme.outlineVariant, Offset(inset, y), Offset(inset + usable, y), rail, StrokeCap.Round)
            drawLine(routeColor, Offset(inset, y), Offset(px, y), rail, StrokeCap.Round)

            if (stopDistances.isNotEmpty()) {
                for (d in ticks) {
                    val x = xOf(d)
                    val passed = d <= progressMeters
                    drawCircle(scheme.surface, 6.dp.toPx(), Offset(x, y))
                    drawCircle(if (passed) routeColor else scheme.onSurface, 6.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
                }
            } else {
                for (d in ticks) {
                    val x = xOf(d)
                    drawLine(
                        if (d <= progressMeters) scheme.surface else scheme.onSurfaceVariant,
                        Offset(x, y - 5.dp.toPx()), Offset(x, y + 5.dp.toPx()), 2.dp.toPx(),
                    )
                }
            }
            // Terminals: filled dots.
            drawCircle(routeColor, 7.dp.toPx(), Offset(inset, y))
            drawCircle(if (progressMeters >= totalMeters && totalMeters > 0) routeColor else scheme.onSurface, 7.dp.toPx(), Offset(inset + usable, y))

            // Puck: ink with a surface ring, and an amber ring while mocking.
            if (live) drawCircle(app.live, 15.dp.toPx(), Offset(px, y))
            drawCircle(scheme.surface, 12.dp.toPx(), Offset(px, y))
            drawCircle(scheme.onSurface, 10.dp.toPx(), Offset(px, y))
        }
        Row(Modifier.fillMaxWidth()) {
            Text("0 km", style = AppType.data, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("%.2f km".format(totalMeters / 1000), style = AppType.data, color = scheme.onSurfaceVariant)
        }
    }
}
