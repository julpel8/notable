package com.ethran.notable.data.model

import io.shipbook.shipbooksdk.ShipBook
import kotlinx.serialization.json.Json

private val log = ShipBook.getLogger("DailyValues")

/**
 * Interactive state of a daily template, persisted as a JSON object of
 * `key -> Float` on the DailyPage row (mirrors toolsboox's calendarValues).
 * Checkboxes use 0/1; future counters can store arbitrary floats.
 *
 * Task checkboxes are keyed by "task:<title>" so the state survives a
 * regeneration of today-tasks.json as long as titles are stable.
 */
const val DAILY_VALUE_CHECKED = 1f

fun taskValueKey(title: String): String = "task:$title"

fun isCheckedValue(value: Float?): Boolean = (value ?: 0f) >= DAILY_VALUE_CHECKED

private val dailyValuesJson = Json { ignoreUnknownKeys = true }

/** Tolerant parser: malformed input yields an empty map. */
fun parseDailyValues(jsonText: String): Map<String, Float> {
    if (jsonText.isBlank()) return emptyMap()
    return try {
        dailyValuesJson.decodeFromString<Map<String, Float>>(jsonText)
    } catch (e: Exception) {
        log.w("Ignoring malformed daily values: ${e.message}")
        emptyMap()
    }
}

fun encodeDailyValues(values: Map<String, Float>): String =
    dailyValuesJson.encodeToString(values)

/** Checkbox semantics: a checked key is removed, an unchecked one is set. */
fun toggleDailyValue(values: Map<String, Float>, key: String): Map<String, Float> =
    if (isCheckedValue(values[key])) values - key
    else values + (key to DAILY_VALUE_CHECKED)
