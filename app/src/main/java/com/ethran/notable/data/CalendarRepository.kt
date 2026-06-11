package com.ethran.notable.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ethran.notable.utils.AppResult
import com.ethran.notable.utils.DomainError
import dagger.hilt.android.qualifiers.ApplicationContext
import io.shipbook.shipbooksdk.ShipBook
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val log = ShipBook.getLogger("CalendarRepository")

/**
 * Read-only access to the device calendar (CalendarContract), fed by DAVx5 or any
 * other sync provider. Queries go through the Instances table so recurring events
 * are expanded — never query Events directly.
 */
@Singleton
class CalendarRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class CalendarEvent(
        val title: String,
        val beginMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        val calendarName: String,
    )

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * All event instances overlapping the given local day, all-day events first,
     * then by start time.
     */
    fun getEventsForDay(date: LocalDate): AppResult<List<CalendarEvent>, DomainError> {
        if (!hasPermission()) {
            return AppResult.Error(DomainError.PermissionDenied(Manifest.permission.READ_CALENDAR))
        }

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, dayStart)
        ContentUris.appendId(uriBuilder, dayEnd)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

        return try {
            val events = mutableListOf<CalendarEvent>()
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.ALL_DAY} DESC, ${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val calNameIdx =
                    cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            title = cursor.getString(titleIdx) ?: "(untitled)",
                            beginMillis = cursor.getLong(beginIdx),
                            endMillis = cursor.getLong(endIdx),
                            allDay = cursor.getInt(allDayIdx) == 1,
                            calendarName = cursor.getString(calNameIdx) ?: "",
                        )
                    )
                }
            }
            AppResult.Success(events)
        } catch (e: SecurityException) {
            log.w("Calendar permission revoked mid-query: ${e.message}")
            AppResult.Error(DomainError.PermissionDenied(Manifest.permission.READ_CALENDAR))
        } catch (e: Exception) {
            log.e("Calendar query failed: ${e.message}")
            AppResult.Error(DomainError.UnexpectedState("Calendar query failed: ${e.message}"))
        }
    }
}
