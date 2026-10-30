package dev.maxmini.gpsplayback.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * Design tokens (see docs/design-system.md). Names match the web app's CSS
 * variables. Amber ("live") is reserved for "this device's location is being
 * mocked right now"; nothing else uses it.
 */

internal object Light {
    val Bg = Color(0xFFF2F4F5)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFE8ECEE)
    val Ink = Color(0xFF111A21)
    val Ink2 = Color(0xFF55636D)
    val Line = Color(0xFFD3DADF)
    val Accent = Color(0xFF2B3BD1)
    val OnAccent = Color(0xFFFFFFFF)
    val AccentSoft = Color(0xFFE3E6FB)
    val Live = Color(0xFFF2A10C)
    val OnLive = Color(0xFF2A1C00)
    val LiveSoft = Color(0xFFFDF0D2)
    val Route = Color(0xFFC0267E)
    val Ok = Color(0xFF1F8A4C)
    val Err = Color(0xFFC8322B)
}

internal object Dark {
    val Bg = Color(0xFF0D1317)
    val Surface = Color(0xFF151D23)
    val Surface2 = Color(0xFF1E282F)
    val Ink = Color(0xFFE7EDF0)
    val Ink2 = Color(0xFF93A1AA)
    val Line = Color(0xFF2A363E)
    val Accent = Color(0xFF7C88FF)
    val OnAccent = Color(0xFF0D1317)
    val AccentSoft = Color(0xFF232A5C)
    val Live = Color(0xFFFFB52E)
    val OnLive = Color(0xFF2A1C00)
    val LiveSoft = Color(0xFF3A2A08)
    val Route = Color(0xFFF05BAE)
    val Ok = Color(0xFF4CC27E)
    val Err = Color(0xFFF0716A)
}

/** Colors with app-specific meaning that Material's ColorScheme has no slot for. */
@Immutable
data class AppColors(
    val live: Color,
    val onLive: Color,
    val liveSoft: Color,
    /** Route line and bullet when the feed has no `route_color`. */
    val route: Color,
    val ok: Color,
    val dark: Boolean,
)

internal val LightAppColors = AppColors(Light.Live, Light.OnLive, Light.LiveSoft, Light.Route, Light.Ok, dark = false)
internal val DarkAppColors = AppColors(Dark.Live, Dark.OnLive, Dark.LiveSoft, Dark.Route, Dark.Ok, dark = true)

internal val LightScheme = lightColorScheme(
    primary = Light.Accent,
    onPrimary = Light.OnAccent,
    primaryContainer = Light.AccentSoft,
    onPrimaryContainer = Light.Accent,
    secondary = Light.Ink2,
    onSecondary = Light.Surface,
    // NavigationBar's selected indicator and tonal buttons use secondaryContainer.
    secondaryContainer = Light.AccentSoft,
    onSecondaryContainer = Light.Accent,
    background = Light.Bg,
    onBackground = Light.Ink,
    surface = Light.Surface,
    onSurface = Light.Ink,
    surfaceVariant = Light.Surface2,
    onSurfaceVariant = Light.Ink2,
    surfaceContainerLowest = Light.Surface,
    surfaceContainerLow = Light.Surface,
    surfaceContainer = Light.Surface,
    surfaceContainerHigh = Light.Surface,
    surfaceContainerHighest = Light.Surface2,
    inverseSurface = Light.Ink,
    inverseOnSurface = Light.Bg,
    outline = Light.Ink2,
    outlineVariant = Light.Line,
    error = Light.Err,
    onError = Color.White,
)

internal val DarkScheme = darkColorScheme(
    primary = Dark.Accent,
    onPrimary = Dark.OnAccent,
    primaryContainer = Dark.AccentSoft,
    onPrimaryContainer = Dark.Accent,
    secondary = Dark.Ink2,
    onSecondary = Dark.Surface,
    secondaryContainer = Dark.AccentSoft,
    onSecondaryContainer = Dark.Accent,
    background = Dark.Bg,
    onBackground = Dark.Ink,
    surface = Dark.Surface,
    onSurface = Dark.Ink,
    surfaceVariant = Dark.Surface2,
    onSurfaceVariant = Dark.Ink2,
    surfaceContainerLowest = Dark.Bg,
    surfaceContainerLow = Dark.Surface,
    surfaceContainer = Dark.Surface,
    surfaceContainerHigh = Dark.Surface,
    surfaceContainerHighest = Dark.Surface2,
    inverseSurface = Dark.Ink,
    inverseOnSurface = Dark.Bg,
    outline = Dark.Ink2,
    outlineVariant = Dark.Line,
    error = Dark.Err,
    onError = Dark.Bg,
)
