package com.ethran.notable.editor.ui.toolbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.editor.EditorViewModel

/**
 * Container for the Toolbar that handles its positioning (Top or Bottom).
 *
 * This component is now decoupled from navigation and engine logic,
 * delegating actions to the [EditorViewModel].
 */
@Composable
fun PositionedToolbar(
    viewModel: EditorViewModel,
    onDrawingStateCheck: () -> Unit
) {
    val settings = GlobalAppSettings.current
    val toolsPos = settings.toolbarPosition
    val actionsPos = settings.actionToolbarPosition
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()

    val bar = @Composable { section: ToolbarSection, position: AppSettings.Position ->
        ToolbarContent(
            uiState = toolbarState,
            onAction = viewModel::onToolbarAction,
            onDrawingStateCheck = onDrawingStateCheck,
            section = section,
            position = position,
            // Eye sits at the corner where the actions bar meets the tools bar.
            collapseAtStart = toolsPos == AppSettings.Position.Left ||
                toolsPos == AppSettings.Position.Top
        )
    }

    if (!settings.splitToolbar || toolsPos == actionsPos) {
        // Merged: a single bar carries both halves (legacy behaviour).
        PlaceToolbar(toolsPos) { bar(ToolbarSection.Both, toolsPos) }
    } else {
        // Split: tools and actions live on independent edges.
        PlaceToolbar(toolsPos) { bar(ToolbarSection.Tools, toolsPos) }
        PlaceToolbar(actionsPos) { bar(ToolbarSection.Actions, actionsPos) }
    }
}

/** Anchors [toolbar] to the requested screen edge. */
@Composable
private fun PlaceToolbar(
    position: AppSettings.Position,
    toolbar: @Composable () -> Unit,
) {
    when (position) {
        AppSettings.Position.Top -> toolbar()
        AppSettings.Position.Bottom -> Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            toolbar()
        }

        AppSettings.Position.Left -> Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            toolbar()
        }

        AppSettings.Position.Right -> Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            toolbar()
        }
    }
}