package com.ethran.notable.io

import android.graphics.Bitmap
import android.graphics.Color
import com.ethran.notable.data.CalendarRepository.CalendarEvent
import com.ethran.notable.data.model.TodayTask
import com.ethran.notable.data.model.taskValueKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke tests for the daily template drawing (Robolectric native graphics
 * provides a real Canvas). Layout aesthetics are validated on-device; here we
 * assert the geometry contract and that something is actually drawn in every
 * data scenario, including the degraded ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CalendarTemplateRendererTest {

    private val renderer = CalendarTemplateRenderer()

    private fun sampleEvents() = listOf(
        CalendarEvent(
            title = "Team breakfast with a fairly long title that needs ellipsizing",
            beginMillis = 1_780_000_000_000,
            endMillis = 1_780_003_600_000,
            allDay = false,
            calendarName = "Work",
        ),
        CalendarEvent(
            title = "Holiday",
            beginMillis = 1_780_000_000_000,
            endMillis = 1_780_086_400_000,
            allDay = true,
            calendarName = "Perso",
        ),
    )

    private fun hasInk(bitmap: Bitmap): Boolean {
        // Sample a coarse grid; the banner underline alone is a full-width
        // solid black bar, so any drawing at all is caught here.
        for (y in 0 until bitmap.height step 4) {
            for (x in 0 until bitmap.width step 4) {
                if (bitmap.getPixel(x, y) != Color.WHITE) return true
            }
        }
        return false
    }

    @Test
    fun `renders at the requested page size`() {
        val bitmap = renderer.render(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = sampleEvents()),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertEquals(600, bitmap.width)
        assertEquals(800, bitmap.height)
        assertTrue("expected the template to draw something", hasInk(bitmap))
    }

    @Test
    fun `render scale is applied and capped at 2x`() {
        val scaled = renderer.render(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList()),
            widthPx = 300, heightPx = 400, scale = 1.5f,
        )
        assertEquals(450, scaled.width)
        assertEquals(600, scaled.height)

        val capped = renderer.render(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList()),
            widthPx = 300, heightPx = 400, scale = 5f,
        )
        assertEquals(600, capped.width)
        assertEquals(800, capped.height)
    }

    @Test
    fun `missing calendar permission still renders a usable template`() {
        val bitmap = renderer.render(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = null),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertTrue(hasInk(bitmap))
    }

    @Test
    fun `tasks from today-tasks json are printed without crashing`() {
        val bitmap = renderer.render(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(
                events = emptyList(),
                tasks = listOf(
                    TodayTask("Pay rent", due = "2026-06-10", project = "Home"),
                    TodayTask("A very long task title that will not fit in the narrow column"),
                ),
            ),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertTrue(hasInk(bitmap))
    }

    @Test
    fun `invalid date falls back to the raw string banner`() {
        val bitmap = renderer.render(
            "not-a-date",
            CalendarTemplateRenderer.TemplateData(events = emptyList()),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertTrue(hasInk(bitmap))
    }

    // ---- interactive zones ----

    private val twoTasks = listOf(
        TodayTask("Pay rent", due = "2026-06-10", project = "Home"),
        TodayTask("Call mom"),
    )

    @Test
    fun `one tap zone per printed task, keyed by title, none for blank rows`() {
        val result = renderer.renderWithZones(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList(), tasks = twoTasks),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertEquals(
            listOf(taskValueKey("Pay rent"), taskValueKey("Call mom")),
            result.tapZones.map { it.valueKey },
        )
        // Zones (page coordinates) stay inside the page, slop included
        for (zone in result.tapZones) {
            assertTrue(zone.left >= 0f && zone.top >= 0f)
            assertTrue(zone.right <= 600f && zone.bottom <= 800f)
            assertTrue(zone.right > zone.left && zone.bottom > zone.top)
        }
    }

    @Test
    fun `no tasks means no tap zones`() {
        val result = renderer.renderWithZones(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList()),
            widthPx = 600, heightPx = 800, scale = 1f,
        )
        assertTrue(result.tapZones.isEmpty())
    }

    @Test
    fun `checked task gets an X inside its checkbox, unchecked stays empty`() {
        fun centerPixel(zoneIndex: Int, values: Map<String, Float>): Int {
            val result = renderer.renderWithZones(
                "2026-06-10",
                CalendarTemplateRenderer.TemplateData(
                    events = emptyList(), tasks = twoTasks, values = values
                ),
                widthPx = 600, heightPx = 800, scale = 1f,
            )
            // Zone = checkbox rect + symmetric slop, so its center is the
            // box center, which the X mark crosses.
            val zone = result.tapZones[zoneIndex]
            val cx = ((zone.left + zone.right) / 2f).toInt()
            val cy = ((zone.top + zone.bottom) / 2f).toInt()
            return result.bitmap.getPixel(cx, cy)
        }

        val checked = mapOf(taskValueKey("Pay rent") to 1f)
        assertEquals(Color.BLACK, centerPixel(0, checked))
        assertEquals(Color.WHITE, centerPixel(1, checked))
        assertEquals(Color.WHITE, centerPixel(0, emptyMap()))
    }

    @Test
    fun `tap zones follow the toolbar insets`() {
        fun zonesWithInsets(left: Float, top: Float) = renderer.renderWithZones(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList(), tasks = twoTasks),
            widthPx = 600, heightPx = 800, scale = 1f,
            leftInsetPx = left, topInsetPx = top,
        ).tapZones

        val plain = zonesWithInsets(0f, 0f)
        val inset = zonesWithInsets(40f, 50f)
        assertEquals(plain[0].left + 40f, inset[0].left, 0.01f)
        assertEquals(plain[0].top + 50f, inset[0].top, 0.01f)
    }

    @Test
    fun `zones are in page units regardless of render scale`() {
        fun zones(scale: Float) = renderer.renderWithZones(
            "2026-06-10",
            CalendarTemplateRenderer.TemplateData(events = emptyList(), tasks = twoTasks),
            widthPx = 600, heightPx = 800, scale = scale,
        ).tapZones

        assertEquals(zones(1f), zones(2f))
    }
}
