package com.ethran.notable.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethran.notable.R
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.ui.views.EInkTextField
import com.ethran.notable.utils.hasCalendarPermission


@Composable
fun GeneralSettings(
    settings: AppSettings, onSettingsChange: (AppSettings) -> Unit
) {
    Column {
        SelectorRow(
            label = stringResource(R.string.default_page_background_template), options = listOf(
                "blank" to stringResource(R.string.blank_page),
                "dotted" to stringResource(R.string.dot_grid),
                "lined" to stringResource(R.string.lines),
                "squared" to stringResource(R.string.small_squares_grid),
                "hexed" to stringResource(R.string.hexagon_grid),
            ), value = settings.defaultNativeTemplate, onValueChange = {
                onSettingsChange(settings.copy(defaultNativeTemplate = it))
            })
        SelectorRow(
            label = stringResource(R.string.toolbar_position), options = listOf(
                AppSettings.Position.Top to stringResource(R.string.toolbar_position_top),
                AppSettings.Position.Bottom to stringResource(
                    R.string.toolbar_position_bottom
                ),
                AppSettings.Position.Left to stringResource(R.string.toolbar_position_left),
                AppSettings.Position.Right to stringResource(R.string.toolbar_position_right)
            ), value = settings.toolbarPosition, onValueChange = { newPosition ->
                onSettingsChange(settings.copy(toolbarPosition = newPosition))
            })

        SettingToggleRow(
            label = stringResource(R.string.split_toolbar),
            value = settings.splitToolbar,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(splitToolbar = isChecked))
            })

        if (settings.splitToolbar) {
            SelectorRow(
                label = stringResource(R.string.toolbar_position_actions), options = listOf(
                    AppSettings.Position.Top to stringResource(R.string.toolbar_position_top),
                    AppSettings.Position.Bottom to stringResource(R.string.toolbar_position_bottom),
                    AppSettings.Position.Left to stringResource(R.string.toolbar_position_left),
                    AppSettings.Position.Right to stringResource(R.string.toolbar_position_right)
                ), value = settings.actionToolbarPosition, onValueChange = { newPosition ->
                    onSettingsChange(settings.copy(actionToolbarPosition = newPosition))
                })
        }

        SettingToggleRow(
            label = stringResource(R.string.use_onyx_neotools_may_cause_crashes),
            value = settings.neoTools,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(neoTools = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.enable_scribble_to_erase),
            value = settings.scribbleToEraseEnabled,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(scribbleToEraseEnabled = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.enable_smooth_scrolling),
            value = settings.smoothScroll,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(smoothScroll = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.continuous_zoom),
            value = settings.continuousZoom,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(continuousZoom = isChecked))
            })
        SettingToggleRow(
            label = stringResource(R.string.continuous_stroke_slider),
            value = settings.continuousStrokeSlider,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(continuousStrokeSlider = isChecked))
            })
        SettingToggleRow(
            label = stringResource(R.string.monochrome_mode) + " " + stringResource(R.string.work_in_progress),
            value = settings.monochromeMode,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(monochromeMode = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.rename_on_create),
            value = settings.renameOnCreate,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(renameOnCreate = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.paginate_pdf),
            value = settings.paginatePdf,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(paginatePdf = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.preview_pdf_pagination),
            value = settings.visualizePdfPagination,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(visualizePdfPagination = isChecked))
            })

        SettingToggleRow(
            label = stringResource(R.string.daily_journal_enabled),
            value = settings.dailyJournalEnabled,
            onToggle = { isChecked ->
                onSettingsChange(settings.copy(dailyJournalEnabled = isChecked))
            })

        CalendarPermissionRow()

        EInkTextField(
            label = stringResource(R.string.journal_sync_folder_label),
            value = settings.journalSyncFolder,
            onValueChange = { onSettingsChange(settings.copy(journalSyncFolder = it)) },
            placeholder = stringResource(R.string.journal_sync_folder_placeholder)
        )
    }
}

/**
 * Shown only while READ_CALENDAR is missing: without it the journal template
 * prints "calendar permission missing" instead of the day's events.
 */
@Composable
private fun CalendarPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCalendarPermission(context)) }
    if (granted) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.grant_calendar_access_hint),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            maxLines = 2
        )
        Button(
            onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Text(stringResource(R.string.grant_calendar_access))
        }
    }
    SettingsDivider()
}
