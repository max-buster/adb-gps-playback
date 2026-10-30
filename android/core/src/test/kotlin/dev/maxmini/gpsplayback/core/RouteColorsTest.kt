package dev.maxmini.gpsplayback.core

import dev.maxmini.gpsplayback.core.style.RouteColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteColorsTest {
    @Test fun normalizesHex() {
        assertEquals("005B95", RouteColors.normalizeHex("005b95"))
        assertEquals("005B95", RouteColors.normalizeHex(" #005B95 "))
        assertNull(RouteColors.normalizeHex(""))
        assertNull(RouteColors.normalizeHex(null))
        assertNull(RouteColors.normalizeHex("FFF"))
        assertNull(RouteColors.normalizeHex("GG0000"))
    }

    @Test fun contrastMatchesWcag() {
        assertEquals(21.0, RouteColors.contrast(0x000000, 0xFFFFFF), 0.01)
        assertEquals(1.0, RouteColors.contrast(0x777777, 0x777777), 0.001)
    }

    @Test fun picksReadableText() {
        assertEquals(0xFFFFFF, RouteColors.textOn(0x005B95))
        assertEquals(0x111A21, RouteColors.textOn(0xF2C200))
    }

    @Test fun liftsDarkColorsOnDarkBackground() {
        val bg = 0x151D23
        val navy = 0x002050
        val lifted = RouteColors.readableOn(navy, bg)
        assertTrue(RouteColors.contrast(lifted, bg) >= 3.0)
        // Already readable colors are left alone.
        assertEquals(0xF2C200, RouteColors.readableOn(0xF2C200, bg))
    }
}
