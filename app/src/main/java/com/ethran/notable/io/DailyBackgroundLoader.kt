package com.ethran.notable.io

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.ethran.notable.data.CalendarRepository
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.data.model.TodayTask
import com.ethran.notable.data.model.parseTodayTasks
import com.ethran.notable.utils.AppResult
import com.ethran.notable.utils.DomainError
import com.ethran.notable.utils.ensureNotMainThread
import io.shipbook.shipbooksdk.ShipBook
import java.io.File
import java.time.LocalDate

private val log = ShipBook.getLogger("DailyBackgroundLoader")

const val TODAY_TASKS_FILE = "today-tasks.json"

/**
 * The journal sync folder (shared with Syncthing): where today-tasks.json is
 * read from and where the Markdown export (later phase) will be written.
 * Blank setting falls back to Documents/notable.
 */
fun resolveJournalSyncDir(settings: AppSettings = GlobalAppSettings.current): File {
    val configured = settings.journalSyncFolder.trim()
    if (configured.isNotEmpty()) {
        return if (configured.startsWith("/")) File(configured)
        else File(Environment.getExternalStorageDirectory(), configured)
    }
    return File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "notable"
    )
}

/**
 * Impure orchestrator for [BackgroundType.Daily] backgrounds: gathers the
 * day's calendar events and the optional today-tasks.json, then delegates to
 * the pure [CalendarTemplateRenderer]. Never throws — degraded inputs render
 * a degraded (but valid) template.
 */
class DailyBackgroundLoader(private val context: Context) {

    fun load(dateIso: String, widthPx: Int, heightPx: Int, scale: Float): Bitmap {
        ensureNotMainThread("DailyBackgroundLoader")

        val events = queryEvents(dateIso)
        val tasks = if (runCatching { LocalDate.parse(dateIso) }.getOrNull() == LocalDate.now()) {
            readTodayTasks()
        } else {
            // today-tasks.json only describes *today*; older/future pages get blanks
            emptyList()
        }

        // Reserve a margin on whichever edge a toolbar is docked, so it does
        // not sit on top of the banner (Top) or the events column (Left). When
        // the toolbar is split both halves are considered.
        val barPx = BUTTON_SIZE * context.resources.displayMetrics.density + 6f
        val settings = GlobalAppSettings.current
        val positions = if (settings.splitToolbar)
            setOf(settings.toolbarPosition, settings.actionToolbarPosition)
        else setOf(settings.toolbarPosition)
        val leftInset = if (AppSettings.Position.Left in positions) barPx else 0f
        val topInset = if (AppSettings.Position.Top in positions) barPx else 0f

        return CalendarTemplateRenderer().render(
            dateIso,
            CalendarTemplateRenderer.TemplateData(events = events, tasks = tasks),
            widthPx,
            heightPx,
            scale,
            leftInsetPx = leftInset,
            topInsetPx = topInset,
        )
    }

    private fun queryEvents(dateIso: String): List<CalendarRepository.CalendarEvent>? {
        val date = runCatching { LocalDate.parse(dateIso) }.getOrElse {
            log.e("Daily background has invalid date '$dateIso'")
            return emptyList()
        }
        return when (val result = CalendarRepository(context).getEventsForDay(date)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                log.w("Calendar unavailable for $dateIso: ${result.error.userMessage}")
                when (result.error) {
                    // null → template prints the "permission missing" notice
                    is DomainError.PermissionDenied -> null
                    else -> emptyList()
                }
            }
        }
    }

    private fun readTodayTasks(): List<TodayTask> {
        return try {
            val file = File(resolveJournalSyncDir(), TODAY_TASKS_FILE)
            if (!file.isFile) return emptyList()
            parseTodayTasks(file.readText())
        } catch (e: Exception) {
            log.w("Could not read $TODAY_TASKS_FILE: ${e.message}")
            emptyList()
        }
    }
}
