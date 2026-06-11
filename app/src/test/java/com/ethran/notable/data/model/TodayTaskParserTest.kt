package com.ethran.notable.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the FROZEN today-tasks.json contract (the V2 glue script's output):
 * a JSON array of {title, due?, project?}. Robolectric because the parser's
 * failure path logs through ShipBook/android.util.Log.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TodayTaskParserTest {

    @Test
    fun `parses the nominal format`() {
        val json = """
            [
              {"title": "Pay rent", "due": "2026-06-10", "project": "Home"},
              {"title": "Call plumber", "due": "2026-06-09", "project": "Home"}
            ]
        """.trimIndent()

        val tasks = parseTodayTasks(json)
        assertEquals(2, tasks.size)
        assertEquals(TodayTask("Pay rent", "2026-06-10", "Home"), tasks[0])
        assertEquals("Call plumber", tasks[1].title)
    }

    @Test
    fun `optional fields may be absent`() {
        val tasks = parseTodayTasks("""[{"title": "Just a title"}]""")
        assertEquals(1, tasks.size)
        assertEquals("Just a title", tasks[0].title)
        assertNull(tasks[0].due)
        assertNull(tasks[0].project)
    }

    @Test
    fun `unknown keys are ignored for forward compatibility`() {
        val tasks = parseTodayTasks(
            """[{"title": "T", "priority": 3, "labels": ["a", "b"]}]"""
        )
        assertEquals(listOf(TodayTask("T")), tasks)
    }

    @Test
    fun `malformed input degrades to an empty list`() {
        assertTrue(parseTodayTasks("not json at all").isEmpty())
        assertTrue(parseTodayTasks("""{"title": "an object, not an array"}""").isEmpty())
        assertTrue(parseTodayTasks("""[{"due": "missing required title"}]""").isEmpty())
        assertTrue(parseTodayTasks("").isEmpty())
        assertTrue(parseTodayTasks("   ").isEmpty())
    }

    @Test
    fun `empty array is a valid no-tasks file`() {
        assertTrue(parseTodayTasks("[]").isEmpty())
    }
}
