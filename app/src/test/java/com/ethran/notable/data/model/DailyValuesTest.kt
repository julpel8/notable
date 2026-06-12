package com.ethran.notable.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the interactive daily template state: JSON round-trip,
 * tolerance to garbage, and checkbox toggle semantics.
 */
class DailyValuesTest {

    @Test
    fun `parse and encode round-trip`() {
        val values = mapOf(taskValueKey("Pay rent") to 1f, "counter:water" to 3f)
        assertEquals(values, parseDailyValues(encodeDailyValues(values)))
    }

    @Test
    fun `parse tolerates blank and malformed input`() {
        assertEquals(emptyMap<String, Float>(), parseDailyValues(""))
        assertEquals(emptyMap<String, Float>(), parseDailyValues("   "))
        assertEquals(emptyMap<String, Float>(), parseDailyValues("not json"))
        assertEquals(emptyMap<String, Float>(), parseDailyValues("[1, 2]"))
        assertEquals(emptyMap<String, Float>(), parseDailyValues("""{"key": "text"}"""))
    }

    @Test
    fun `empty object is the default state`() {
        assertEquals(emptyMap<String, Float>(), parseDailyValues("{}"))
    }

    @Test
    fun `toggle checks an unchecked key and unchecks a checked one`() {
        val key = taskValueKey("Pay rent")

        val checked = toggleDailyValue(emptyMap(), key)
        assertTrue(isCheckedValue(checked[key]))

        val unchecked = toggleDailyValue(checked, key)
        assertFalse(isCheckedValue(unchecked[key]))
        assertFalse(unchecked.containsKey(key))
    }

    @Test
    fun `toggle leaves other keys untouched`() {
        val other = "counter:water" to 3f
        val toggled = toggleDailyValue(mapOf(other), taskValueKey("Call mom"))
        assertEquals(3f, toggled["counter:water"])
        assertTrue(isCheckedValue(toggled[taskValueKey("Call mom")]))
    }

    @Test
    fun `isCheckedValue treats null and zero as unchecked`() {
        assertFalse(isCheckedValue(null))
        assertFalse(isCheckedValue(0f))
        assertTrue(isCheckedValue(1f))
        assertTrue(isCheckedValue(2f))
    }
}
