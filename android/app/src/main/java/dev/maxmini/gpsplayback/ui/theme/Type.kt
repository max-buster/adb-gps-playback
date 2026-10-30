package dev.maxmini.gpsplayback.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.maxmini.gpsplayback.R

/** Overpass: an open-source cut of the US highway signage face. Bundled in res/font. */
val Overpass = FontFamily(
    Font(R.font.overpass_regular, FontWeight.Normal),
    Font(R.font.overpass_semibold, FontWeight.SemiBold),
    Font(R.font.overpass_bold, FontWeight.Bold),
    Font(R.font.overpass_extrabold, FontWeight.ExtraBold),
)

/** For numbers that change while playing: tabular, so values don't jitter. */
val OverpassMono = FontFamily(
    Font(R.font.overpass_mono_regular, FontWeight.Normal),
    Font(R.font.overpass_mono_semibold, FontWeight.SemiBold),
)

private val base = Typography()

internal val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Overpass, fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Overpass, fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Overpass, fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Overpass, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = base.titleSmall.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Overpass, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Overpass),
    bodySmall = base.bodySmall.copy(fontFamily = Overpass, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = base.labelLarge.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Overpass, fontWeight = FontWeight.Bold),
)

/** Text styles outside Material's scale. */
object AppType {
    /** Section labels: small caps-style uppercase. Uppercase the string yourself. */
    val label = TextStyle(fontFamily = Overpass, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.9.sp)
    val data = TextStyle(fontFamily = OverpassMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
    val dataStrong = data.copy(fontWeight = FontWeight.SemiBold)
    val dataLarge = TextStyle(fontFamily = OverpassMono, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp, fontFeatureSettings = "tnum")
}
