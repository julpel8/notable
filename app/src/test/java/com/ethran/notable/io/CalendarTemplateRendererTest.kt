package com.ethran.notable.io

import android.graphics.Bitmap
import android.graphics.Color
import com.ethran.notable.data.CalendarRepository.CalendarEvent
import com.ethran.notable.data.model.TodayTask
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
}
