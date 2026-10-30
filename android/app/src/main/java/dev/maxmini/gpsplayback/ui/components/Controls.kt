package dev.maxmini.gpsplayback.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.R
import dev.maxmini.gpsplayback.ui.theme.AppType

/** A row of mutually exclusive options in a sunken track, e.g. the speed multiplier. */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = scheme.surfaceVariant) {
        Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { o ->
                val on = o == selected
                Surface(
                    onClick = { onSelect(o) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (on) scheme.surface else scheme.surfaceVariant,
                    contentColor = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                    border = if (on) BorderStroke(1.dp, scheme.outlineVariant) else null,
                    shadowElevation = if (on) 1.dp else 0.dp,
                ) {
                    Text(label(o), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = AppType.dataStrong)
                }
            }
        }
    }
}

/**
 * Previous stop, Play/Pause, next stop, and Stop mocking set apart in red:
 * Stop is the only control that hands the device back to its real location.
 */
@Composable
fun TransportBar(
    playing: Boolean,
    hasStops: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    mocking: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (hasStops) {
            FilledTonalIconButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_skip_prev), contentDescription = "Previous stop", Modifier.size(20.dp))
            }
        }
        FilledIconButton(
            onClick = onToggle,
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (playing) "Pause" else "Play",
                Modifier.size(28.dp),
            )
        }
        if (hasStops) {
            FilledTonalIconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next stop", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onStop,
            enabled = mocking,
            modifier = Modifier.height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Icon(painterResource(R.drawable.ic_stop), contentDescription = null, Modifier.size(14.dp))
            Spacer(Modifier.size(6.dp))
            Text("Stop mocking", maxLines = 1)
        }
    }
}
