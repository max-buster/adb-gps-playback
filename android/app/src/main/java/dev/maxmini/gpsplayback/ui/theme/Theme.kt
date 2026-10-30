package dev.maxmini.gpsplayback.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.core.style.RouteColors

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The app's fixed theme. Deliberately no dynamic (wallpaper) color: color
 * carries meaning here (live, route, error), and screenshots from any device
 * should look the same.
 */
@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Access to the app-specific tokens alongside MaterialTheme. */
object AppTokens {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

/** Fill and text color for a route's bullet and line. */
@Immutable
data class RouteStyle(val fill: Color, val text: Color)

private fun opaque(rgb: Int) = Color(0xFF000000.toInt() or rgb)

/**
 * Resolve a feed's `route_color` / `route_text_color` (hex `RRGGBB`, or null)
 * against the current theme. Dark feed colors are lightened on the dark theme
 * so the line stays visible, and the text color falls back to whichever of
 * ink or white reads better.
 */
@Composable
fun rememberRouteStyle(color: String?, textColor: String?): RouteStyle {
    val app = AppTokens.colors
    val surface = MaterialTheme.colorScheme.surface
    return remember(color, textColor, app) {
        val raw = color?.let(RouteColors::rgb) ?: (app.route.toArgb() and 0xFFFFFF)
        val fill = if (app.dark) RouteColors.readableOn(raw, surface.toArgb() and 0xFFFFFF) else raw
        val givenText = textColor?.let(RouteColors::rgb)
        val text = if (givenText != null && RouteColors.contrast(fill, givenText) >= 3.0) givenText else RouteColors.textOn(fill)
        RouteStyle(opaque(fill), opaque(text))
    }
}
