package com.ethran.notable.editor.ui.toolbar

import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.ui.convertDpToPixel

/** True when the toolbar is docked on a side and laid out vertically. */
fun isToolbarVertical(
    position: AppSettings.Position = GlobalAppSettings.current.toolbarPosition
): Boolean = position == AppSettings.Position.Left || position == AppSettings.Position.Right

data class ToolbarPopupPlacement(val alignment: Alignment, val offset: IntOffset)

/**
 * Where popups anchored to a toolbar button should open so they clear the
 * bar: below it when docked top/bottom (legacy behavior, the bottom case is
 * additionally handled by a bottom padding in the popup content), beside it
 * when docked left/right.
 */
fun toolbarPopupPlacement(
    context: Context,
    position: AppSettings.Position = GlobalAppSettings.current.toolbarPosition,
): ToolbarPopupPlacement {
    val sideShift = convertDpToPixel((BUTTON_SIZE + 6).dp, context).toInt()
    return when (position) {
        AppSettings.Position.Left ->
            ToolbarPopupPlacement(Alignment.TopStart, IntOffset(sideShift, 0))

        AppSettings.Position.Right ->
            ToolbarPopupPlacement(Alignment.TopEnd, IntOffset(-sideShift, 0))

        else -> ToolbarPopupPlacement(
            Alignment.TopCenter,
            IntOffset(0, convertDpToPixel(43.dp, context).toInt())
        )
    }
}
