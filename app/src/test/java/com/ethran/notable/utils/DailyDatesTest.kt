package com.ethran.notable.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Pure-JVM tests for the daily journal date helpers. All "today" logic is
 * pinned through an injected Clock so results are deterministic.
 */
class DailyDatesTest {

    // 2026-06-10T12:00 in Paris
    private val parisClock: Clock = Clock.fixed(
        Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Europe/Paris")
    )

    @Test
    fun `todayIso uses the clock's zone and day`() {
        assertEquals("2026-06-10", todayIso(parisClock))

        // 23:30 in Paris on the 10th is already the 11th in Tokyo
        val lateEvening = Clock.fixed(
            Instant.parse("2026-06-10T21:30:00Z"), ZoneId.of("Europe/Paris")
        )
        val tokyo = Clock.fixed(
            Instant.parse("2026-06-10T21:30:00Z"), ZoneId.of("Asia/Tokyo")
        )
        assertEquals("2026-06-10", todayIso(lateEvening))
        assertEquals("2026-06-11", todayIso(tokyo))
    }

    @Test
    fun `shiftDay crosses month and year boundaries`() {
        assertEquals("2026-03-01", shiftDay("2026-02-28", 1))
        assertEquals("2026-01-01", shiftDay("2025-12-31", 1))
        assertEquals("2025-12-31", shiftDay("2026-01-01", -1))
        // 2024 is a leap year
        assertEquals("2024-02-29", shiftDay("2024-02-28", 1))
        assertEquals("2026-06-10", shiftDay("2026-06-10", 0))
    }

    @Test
    fun `isToday matches only the clock's current day`() {
        assertTrue(isToday("2026-06-10", parisClock))
        assertFalse(isToday("2026-06-09", parisClock))
        assertFalse(isToday("2026-06-11", parisClock))
    }

    @Test
    fun `formatBannerDate renders weekday and date in the given locale`() {
        // 2026-06-10 is a Wednesday
        assertEquals("mercredi 10 juin 2026", formatBannerDate("2026-06-10", Locale.FRENCH))
        assertEquals("Wednesday 10 June 2026", formatBannerDate("2026-06-10", Locale.ENGLISH))
    }
}
