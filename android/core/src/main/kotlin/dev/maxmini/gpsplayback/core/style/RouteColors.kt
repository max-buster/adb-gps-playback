package dev.maxmini.gpsplayback.core.style

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Color math for GTFS route colors, kept free of Android so it's unit tested.
 * Colors are plain `0xRRGGBB` ints (no alpha).
 */
object RouteColors {
    /** Normalize a GTFS color field (`RRGGBB`, optionally with `#`) to upper-case `RRGGBB`, or null. */
    fun normalizeHex(raw: String?): String? {
        val s = raw?.trim()?.removePrefix("#") ?: return null
        if (s.length != 6 || !s.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return s.uppercase()
    }

    fun rgb(hex: String): Int = hex.toInt(16)

    /** WCAG relative luminance, 0 (black) to 1 (white). */
    fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((rgb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** WCAG contrast ratio between two colors, 1 to 21. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Black-ish or white text, whichever reads better on [background]. */
    fun textOn(background: Int, dark: Int = 0x111A21, light: Int = 0xFFFFFF): Int =
        if (contrast(background, dark) >= contrast(background, light)) dark else light

    /** Mix [rgb] toward [target] by [t] (0 = unchanged, 1 = target). */
    fun mix(rgb: Int, target: Int, t: Double): Int {
        fun ch(shift: Int): Int {
            val a = (rgb shr shift) and 0xFF
            val b = (target shr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /**
     * Lighten [rgb] toward white until it has at least [minContrast] against
     * [background], so dark feed colors stay visible on the dark theme and the
     * dimmed dark map. Colors that already pass are returned unchanged.
     */
    fun readableOn(rgb: Int, background: Int, minContrast: Double = 3.0): Int {
        var t = 0.0
        var c = rgb
        while (contrast(c, background) < minContrast && t < 1.0) {
            t += 0.05
            c = mix(rgb, 0xFFFFFF, t)
        }
        return c
    }
}
