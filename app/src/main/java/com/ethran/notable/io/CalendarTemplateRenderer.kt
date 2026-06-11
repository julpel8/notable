package com.ethran.notable.io

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import com.ethran.notable.data.CalendarRepository.CalendarEvent
import com.ethran.notable.data.model.TodayTask
import com.ethran.notable.utils.formatBannerDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Draws the daily page template: date banner on top, a left column with the
 * day's event timeline and a tasks zone, and free writing space everywhere
 * else. Pure drawing — events/tasks are passed in pre-fetched, so this class
 * touches no ContentResolver and is unit-testable.
 *
 * E-ink constraints: no antialiasing, pure black/white plus dash patterns for
 * "gray", stroke widths >= 2px. The bitmap is exactly one viewport tall: the
 * template anchors to the top of the page and the page continues blank below.
 */
class CalendarTemplateRenderer {

    data class TemplateData(
        // null = READ_CALENDAR not granted (renders a discreet notice)
        val events: List<CalendarEvent>?,
        val tasks: List<TodayTask> = emptyList(),
    )

    fun render(
        dateIso: String,
        data: TemplateData,
        widthPx: Int,
        heightPx: Int,
        scale: Float,
    ): Bitmap {
        // Cap the render scale: beyond 2x the e-ink display gains nothing.
        val s = scale.coerceIn(1f, 2f)
        val bitmap = createBitmap((widthPx * s).toInt(), (heightPx * s).toInt())
        val canvas = Canvas(bitmap)
        // All coordinates below are in page units.
        canvas.scale(s, s)
        canvas.drawColor(Color.WHITE)

        val width = widthPx.toFloat()
        val height = heightPx.toFloat()
        val margin = 24f
        val columnWidth = width / 3f

        var y = drawBanner(canvas, dateIso, width, margin)
        y += 18f

        // Separator under the banner, full width
        canvas.drawRect(margin, y, width - margin, y + 3f, blackFill)
        y += 24f

        // Vertical separator to the right of the events/tasks column
        canvas.drawRect(columnWidth, y, columnWidth + 2f, height - margin, grayFill)

        y = drawEvents(canvas, data.events, margin, y, columnWidth - margin * 1.5f)
        y += 30f
        drawTasks(canvas, data.tasks, margin, y, columnWidth - margin * 1.5f, height)

        return bitmap
    }

    // ---- sections ----

    private fun drawBanner(canvas: Canvas, dateIso: String, width: Float, margin: Float): Float {
        val banner = runCatching { formatBannerDate(dateIso) }.getOrDefault(dateIso)
        val y = margin + bannerPaint.textSize
        canvas.drawText(ellipsize(banner, bannerPaint, width - margin * 2), margin, y, bannerPaint)
        return y
    }

    private fun drawEvents(
        canvas: Canvas,
        events: List<CalendarEvent>?,
        x: Float,
        startY: Float,
        maxWidth: Float,
    ): Float {
        var y = startY

        if (events == null) {
            y += noticePaint.textSize
            canvas.drawText("calendar permission missing", x, y, noticePaint)
            return y
        }
        if (events.isEmpty()) {
            y += noticePaint.textSize
            canvas.drawText("no events", x, y, noticePaint)
            return y
        }

        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
        val zone = ZoneId.systemDefault()
        for (event in events) {
            // Hour line (all-day events get a bullet instead)
            y += eventTimePaint.textSize + 14f
            val timeLabel = if (event.allDay) "■ all day" else {
                val begin = Instant.ofEpochMilli(event.beginMillis).atZone(zone).format(timeFormat)
                val end = Instant.ofEpochMilli(event.endMillis).atZone(zone).format(timeFormat)
                "$begin–$end"
            }
            canvas.drawText(timeLabel, x, y, eventTimePaint)

            // Title line, ellipsized to the column
            y += eventTitlePaint.textSize + 6f
            val title = ellipsize(event.title, eventTitlePaint, maxWidth)
            canvas.drawText(title, x, y, eventTitlePaint)

            // Dashed separator
            y += 12f
            canvas.drawLine(x, y, x + maxWidth, y, dashedSeparatorPaint)
        }
        return y
    }

    private fun drawTasks(
        canvas: Canvas,
        tasks: List<TodayTask>,
        x: Float,
        startY: Float,
        maxWidth: Float,
        pageHeight: Float,
    ) {
        var y = startY + sectionPaint.textSize
        canvas.drawText("Tasks", x, y, sectionPaint)
        y += 16f

        val checkboxSize = 28f
        val rowHeight = checkboxSize + 22f
        val textX = x + checkboxSize + 12f

        // Printed tasks (from today-tasks.json) first, then blank rows to fill.
        val blankRows = if (tasks.isEmpty()) 6 else 2
        val rows: List<TodayTask?> = tasks + List(blankRows) { null }

        for (task in rows) {
            if (y + rowHeight > pageHeight - 30f) break
            val top = y + (rowHeight - checkboxSize) / 2f
            canvas.drawRect(x, top, x + checkboxSize, top + checkboxSize, checkboxPaint)
            if (task != null) {
                val label = buildString {
                    append(task.title)
                    task.project?.let { append("  [").append(it).append("]") }
                }
                canvas.drawText(
                    ellipsize(label, taskPaint, maxWidth - checkboxSize - 12f),
                    textX,
                    top + checkboxSize - 7f,
                    taskPaint
                )
            }
            y += rowHeight
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String =
        TextUtils.ellipsize(text, android.text.TextPaint(paint), maxWidth, TextUtils.TruncateAt.END)
            .toString()

    // ---- paints (no antialiasing, e-ink) ----

    private val bannerPaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 52f
        isFakeBoldText = true
    }
    private val sectionPaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 34f
        isFakeBoldText = true
    }
    private val eventTimePaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 26f
        isFakeBoldText = true
    }
    private val eventTitlePaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 28f
    }
    private val taskPaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 28f
    }
    private val noticePaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        textSize = 24f
    }
    private val blackFill = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    // "Gray" for e-ink: pure black, the dash/checker pattern does the graying
    private val grayFill = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        alpha = 90
        style = Paint.Style.FILL
    }
    private val dashedSeparatorPaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(6f, 8f), 0f)
    }
    private val checkboxPaint = Paint().apply {
        isAntiAlias = false
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
}
