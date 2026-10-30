package dev.maxmini.gpsplayback.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.maxmini.gpsplayback.ui.theme.Overpass
import dev.maxmini.gpsplayback.ui.theme.RouteStyle

/**
 * Transit-style route bullet: `route_short_name` on the route's color.
 * Names longer than 4 characters are cut, so the bullet stays a pill.
 */
@Composable
fun RouteBullet(shortName: String?, style: RouteStyle, modifier: Modifier = Modifier, small: Boolean = false) {
    val size = if (small) 22.dp else 32.dp
    Box(
        modifier
            .heightIn(min = size)
            .widthIn(min = size)
            .clip(CircleShape)
            .background(style.fill)
            .padding(horizontal = if (small) 5.dp else 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            shortName?.take(4).orEmpty(),
            color = style.text,
            maxLines = 1,
            style = TextStyle(
                fontFamily = Overpass,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (small) 11.sp else 14.sp,
                lineHeight = if (small) 12.sp else 16.sp,
            ),
        )
    }
}
