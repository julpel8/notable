package com.ethran.notable.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTapZoneTest {

    private val zone = DailyTapZone(10f, 20f, 50f, 60f, "task:Pay rent")

    @Test
    fun `contains includes edges and excludes outside points`() {
        assertTrue(zone.contains(10f, 20f))
        assertTrue(zone.contains(50f, 60f))
        assertTrue(zone.contains(30f, 40f))
        assertFalse(zone.contains(9.9f, 40f))
        assertFalse(zone.contains(30f, 60.1f))
    }

    @Test
    fun `zoneAt returns the first hit or null`() {
        val other = DailyTapZone(100f, 100f, 140f, 140f, "task:Call mom")
        val zones = listOf(zone, other)

        assertEquals("task:Pay rent", zones.zoneAt(30f, 40f)?.valueKey)
        assertEquals("task:Call mom", zones.zoneAt(120f, 120f)?.valueKey)
        assertNull(zones.zoneAt(0f, 0f))
        assertNull(emptyList<DailyTapZone>().zoneAt(30f, 40f))
    }
}
