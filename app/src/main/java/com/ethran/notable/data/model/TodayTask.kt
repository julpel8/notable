package com.ethran.notable.data.model

import io.shipbook.shipbooksdk.ShipBook
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val log = ShipBook.getLogger("TodayTask")

/**
 * One task printed in the "Tasks" zone of the daily template.
 *
 * FROZEN FORMAT — this is the contract with the (future, server-side) glue
 * script that drops a `today-tasks.json` file into the Syncthing folder:
 * a JSON array of objects `{ "title": ..., "due": ..., "project": ... }`,
 * `due` and `project` optional. Do not change without versioning the file.
 */
@Serializable
data class TodayTask(
    val title: String,
    val due: String? = null,
    val project: String? = null,
)

private val todayTasksJson = Json { ignoreUnknownKeys = true }

/**
 * Tolerant parser: malformed or empty input yields an empty list — the
 * template silently falls back to blank checkboxes.
 */
fun parseTodayTasks(jsonText: String): List<TodayTask> {
    if (jsonText.isBlank()) return emptyList()
    return try {
        todayTasksJson.decodeFromString<List<TodayTask>>(jsonText)
    } catch (e: Exception) {
        log.w("Ignoring malformed today-tasks.json: ${e.message}")
        emptyList()
    }
}
