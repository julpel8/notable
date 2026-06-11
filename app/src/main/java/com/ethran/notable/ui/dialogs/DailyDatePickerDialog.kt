package com.ethran.notable.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ethran.notable.ui.noRippleClickable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Minimal e-ink friendly date picker for the daily journal: white background,
 * 1px black borders, plain text day grid, no animations.
 */
@Composable
fun DailyDatePickerDialog(
    initialDate: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var shownMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = remember { LocalDate.now() }
    val locale = remember { Locale.getDefault() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .background(Color.White)
                .border(1.dp, Color.Black)
                .padding(12.dp)
        ) {
            // Header: ‹ month year ›
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PickerButton("‹") { shownMonth = shownMonth.minusMonths(1) }
                Text(
                    text = "${
                        shownMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                            .replaceFirstChar { it.uppercase(locale) }
                    } ${shownMonth.year}",
                    style = MaterialTheme.typography.subtitle1.copy(
                        color = Color.Black, fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                PickerButton("›") { shownMonth = shownMonth.plusMonths(1) }
            }

            Spacer(Modifier.height(8.dp))

            // Weekday initials, Monday first
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = MaterialTheme.typography.caption.copy(color = Color.Black),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Day grid: weeks as rows
            val firstOfMonth = shownMonth.atDay(1)
            // 0 for Monday .. 6 for Sunday
            val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
            val daysInMonth = shownMonth.lengthOfMonth()
            val cells: List<LocalDate?> =
                List(leadingBlanks) { null } + (1..daysInMonth).map { shownMonth.atDay(it) }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day -> DayCell(day, initialDate, today, onPick) }
                    repeat(7 - week.size) {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .size(40.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PickerButton("Today") { onPick(today) }
                PickerButton("Close") { onDismiss() }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: LocalDate?,
    selected: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .size(40.dp)
            .padding(1.dp)
            .then(
                when {
                    day == null -> Modifier
                    day == selected -> Modifier
                        .background(Color.Black)
                        .noRippleClickable { onPick(day) }

                    day == today -> Modifier
                        .border(2.dp, Color.Black)
                        .noRippleClickable { onPick(day) }

                    else -> Modifier.noRippleClickable { onPick(day) }
                }
            )
    ) {
        if (day != null) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.body2.copy(
                    color = if (day == selected) Color.White else Color.Black,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun PickerButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.body1.copy(color = Color.Black),
        modifier = Modifier
            .border(1.dp, Color.Black)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
