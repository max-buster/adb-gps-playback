package dev.maxmini.gpsplayback.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.PlaybackStatus
import dev.maxmini.gpsplayback.core.model.PlayerState
import dev.maxmini.gpsplayback.ui.theme.AppTokens
import dev.maxmini.gpsplayback.ui.theme.AppType

/** What the device's location is doing right now, from the user's point of view. */
enum class LiveState {
    /** No mocking: the device uses its real location. */
    Idle,
    /** Pushing moving fixes. */
    Mocking,
    /** Paused, but still pushing the same fix so the device stays pinned. */
    Holding,
    /** The last attempt failed (see PlaybackStatus.lastError). */
    Error,
}

fun liveStateOf(status: PlaybackStatus, player: PlayerState): LiveState = when {
    status.lastError != null -> LiveState.Error
    status.serviceRunning && player.playing -> LiveState.Mocking
    status.serviceRunning -> LiveState.Holding
    else -> LiveState.Idle
}

/**
 * The live status pill shown in the top bar of every tab. Amber means the
 * location is fake right now; it's the only place besides the map marker and
 * the notification that uses that color.
 */
@Composable
fun LiveStatusPill(state: LiveState, modifier: Modifier = Modifier) {
    val app = AppTokens.colors
    val scheme = MaterialTheme.colorScheme
    val (bg, fg, dot, label) = when (state) {
        LiveState.Mocking -> Quad(app.live, app.onLive, app.onLive, "Mocking")
        LiveState.Holding -> Quad(app.liveSoft, scheme.onSurface, app.live, "Holding")
        LiveState.Error -> Quad(scheme.error, scheme.onError, scheme.onError, "Error")
        LiveState.Idle -> Quad(scheme.surfaceVariant, scheme.onSurfaceVariant, scheme.onSurfaceVariant, "Real location")
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = if (state == LiveState.Holding) BorderStroke(1.5.dp, app.live) else null,
    ) {
        Row(
            Modifier.padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val pulse = if (state == LiveState.Mocking) {
                val t = rememberInfiniteTransition(label = "live")
                val a by t.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
                    label = "pulse",
                )
                a
            } else {
                1f
            }
            Box(Modifier.size(8.dp).alpha(pulse).background(dot, CircleShape))
            Text(label.uppercase(), style = AppType.label)
        }
    }
}

private data class Quad(val bg: Color, val fg: Color, val dot: Color, val label: String)
