package com.ethran.notable.editor.ui.toolbar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethran.notable.R
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.editor.ToolbarAction
import com.ethran.notable.editor.ToolbarUiState
import com.ethran.notable.editor.state.Mode
import com.ethran.notable.editor.utils.Pen
import com.ethran.notable.editor.utils.PenSetting
import com.ethran.notable.ui.dialogs.BackgroundSelector
import com.ethran.notable.ui.dialogs.DailyDatePickerDialog
import com.ethran.notable.ui.noRippleClickable
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Clipboard
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.Grid
import compose.icons.feathericons.RefreshCcw
import compose.icons.feathericons.RotateCw
import java.time.LocalDate


private fun isSelected(state: ToolbarUiState, penType: Pen): Boolean {
    return (state.mode == Mode.Draw || state.mode == Mode.Line) && state.pen == penType
}

/**
 * Which half of the toolbar a bar renders. [Both] is the default merged bar;
 * [Tools] and [Actions] are the two halves when the toolbar is split across two
 * positions. The collapse toggle and popups-host modals live on [Tools]/[Both].
 */
enum class ToolbarSection { Tools, Actions, Both }

private val SIZES_STROKES_DEFAULT = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f)
private val SIZES_MARKER_DEFAULT = listOf("M" to 25f, "L" to 40f, "XL" to 60f, "XXL" to 80f)

@Composable
fun ToolbarContent(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    onDrawingStateCheck: () -> Unit,
    section: ToolbarSection = ToolbarSection.Both,
    position: AppSettings.Position = GlobalAppSettings.current.toolbarPosition,
    // For a split actions bar: render the collapse (eye) button at the start of
    // the bar when true, at the end when false, so it lands at the corner where
    // it crosses the tools bar.
    collapseAtStart: Boolean = true,
) {
    // Activity result launcher for picking images
    val pickMedia = rememberLauncherForActivityResult(contract = PickVisualMedia()) { uri ->
        uri?.let { onAction(ToolbarAction.ImagePicked(it)) }
    }

    // On exit or change of toolbar states, check if we should allow raw drawing
    LaunchedEffect(
        uiState.isBackgroundSelectorModalOpen, uiState.isMenuOpen, uiState.isDatePickerOpen
    ) {
        onDrawingStateCheck()
    }

    // Modals are hosted on the tools/merged bar so they render exactly once even
    // when the toolbar is split across two bars.
    if (section != ToolbarSection.Actions) {
        if (uiState.isBackgroundSelectorModalOpen) {
            BackgroundSelector(
                initialPageBackgroundType = uiState.backgroundType,
                initialPageBackground = uiState.backgroundPath,
                initialPageNumberInPdf = uiState.backgroundPageNumber,
                notebookId = uiState.notebookId,
                pageNumberInBook = uiState.currentPageNumber,
                onChange = { type, path -> onAction(ToolbarAction.BackgroundChanged(type, path)) },
                onClose = { onAction(ToolbarAction.ToggleBackgroundSelector(false)) }
            )
        }

        if (uiState.isDatePickerOpen && uiState.dailyDate != null) {
            DailyDatePickerDialog(
                initialDate = LocalDate.parse(uiState.dailyDate),
                onPick = { picked -> onAction(ToolbarAction.JumpToDate(picked.toString())) },
                onDismiss = { onAction(ToolbarAction.ToggleDatePicker(false)) }
            )
        }
    }

    val pickImage = { pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }

    if (uiState.isToolbarOpen) {
        if (isToolbarVertical(position)) {
            VerticalToolbar(uiState, onAction, pickImage, position, section, collapseAtStart)
        } else {
            HorizontalToolbar(uiState, onAction, pickImage, position, section, collapseAtStart)
        }
    } else if (section != ToolbarSection.Actions) {
        // Collapsed: only the tools/merged bar shows the button to reopen.
        ToolbarButton(
            onSelect = { onAction(ToolbarAction.ToggleToolbar) },
            iconId = presentlyUsedToolIcon(uiState.mode, uiState.pen),
            penColor = if (uiState.mode != Mode.Erase) uiState.penSettings[uiState.pen.penName]?.color?.let {
                Color(
                    it
                )
            } else null,
            contentDescription = "open toolbar",
            modifier = Modifier
                .height((BUTTON_SIZE + 1).dp)
                .padding(bottom = 1.dp)
        )
    }
}

@Composable
private fun HorizontalToolbar(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    pickImage: () -> Unit,
    position: AppSettings.Position,
    section: ToolbarSection,
    collapseAtStart: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height((BUTTON_SIZE + 2).dp)
            .padding(bottom = 1.dp)
    ) {
        if (position == AppSettings.Position.Bottom) {
            EdgeLineHorizontal()
        }
        Row(
            Modifier
                .background(Color.White)
                .height(BUTTON_SIZE.dp)
                .fillMaxWidth()
        ) {

            if (section != ToolbarSection.Actions) {
                // Close button: only on the merged bar; the split tools bar
                // delegates hiding to the eye on the actions bar.
                if (section == ToolbarSection.Both) {
                    CollapseButton(onAction)
                }

                // Left scrollable section: drawing tools, centered
                Row(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ToolButtons(
                        uiState = uiState,
                        onAction = onAction,
                        pickImage = pickImage,
                        divider = { VerticalDivider() }
                    )
                }
            }

            if (section != ToolbarSection.Tools) {
                // Right fixed section: undo/redo, navigation, menu. The split
                // actions bar carries the single eye that hides both bars; the
                // eye stays pinned to the corner while the actions center.
                if (section == ToolbarSection.Actions && collapseAtStart) {
                    CollapseButton(onAction)
                }
                Row(
                    modifier = if (section == ToolbarSection.Actions) Modifier.weight(1f)
                    else Modifier,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ActionButtons(
                        uiState = uiState,
                        onAction = onAction,
                        divider = { VerticalDivider() },
                        vertical = false,
                        position = position
                    )
                }
                if (section == ToolbarSection.Actions && !collapseAtStart) {
                    CollapseButton(onAction)
                }
            }
        }

        EdgeLineHorizontal()
    }
}

@Composable
private fun VerticalToolbar(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    pickImage: () -> Unit,
    position: AppSettings.Position,
    section: ToolbarSection,
    collapseAtStart: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .width((BUTTON_SIZE + 1).dp)
    ) {
        if (position == AppSettings.Position.Right) {
            EdgeLineVertical()
        }
        Column(
            Modifier
                .background(Color.White)
                .width(BUTTON_SIZE.dp)
                .fillMaxHeight()
        ) {

            if (section != ToolbarSection.Actions) {
                // Close button: only on the merged bar; the split tools bar
                // delegates hiding to the eye on the actions bar.
                if (section == ToolbarSection.Both) {
                    CollapseButton(onAction)
                }

                // Scrollable section: drawing tools, centered
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    ToolButtons(
                        uiState = uiState,
                        onAction = onAction,
                        pickImage = pickImage,
                        divider = { HorizontalDivider() }
                    )
                }
            }

            if (section != ToolbarSection.Tools) {
                // Bottom fixed section: undo/redo, navigation, menu. The split
                // actions bar carries the single eye that hides both bars; the
                // eye stays pinned to the corner while the actions center.
                if (section == ToolbarSection.Actions && collapseAtStart) {
                    CollapseButton(onAction)
                }
                Column(
                    modifier = if (section == ToolbarSection.Actions) Modifier.weight(1f)
                    else Modifier,
                    verticalArrangement = Arrangement.Center
                ) {
                    ActionButtons(
                        uiState = uiState,
                        onAction = onAction,
                        divider = { HorizontalDivider() },
                        vertical = true,
                        position = position
                    )
                }
                if (section == ToolbarSection.Actions && !collapseAtStart) {
                    CollapseButton(onAction)
                }
            }
        }
        if (position == AppSettings.Position.Left) {
            EdgeLineVertical()
        }
    }
}

/** The eye button that collapses the toolbar (both bars when split). */
@Composable
private fun CollapseButton(onAction: (ToolbarAction) -> Unit) {
    ToolbarButton(
        onSelect = { onAction(ToolbarAction.ToggleToolbar) },
        vectorIcon = FeatherIcons.EyeOff,
        contentDescription = "close toolbar",
        penColor = Color.Black,
        isSelected = true
    )
}

/**
 * Drawing tools shared by both orientations: pens, line, marker, eraser,
 * lasso, image picker, paste, reset view. Container-agnostic — the caller
 * provides the layout (Row or Column) and the matching [divider].
 */
@Composable
private fun ToolButtons(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    pickImage: () -> Unit,
    divider: @Composable () -> Unit,
) {
    divider()

    // Pens
    PenToolbarButton(
        pen = Pen.BALLPEN,
        icon = R.drawable.ballpen,
        isSelected = isSelected(uiState, Pen.BALLPEN),
        onSelect = { onAction(ToolbarAction.ChangePen(Pen.BALLPEN)) },
        sizes = SIZES_STROKES_DEFAULT,
        penSetting = uiState.penSettings[Pen.BALLPEN.penName] ?: PenSetting(
            5f,
            android.graphics.Color.BLACK
        ),
        onChangeSetting = {
            onAction(
                ToolbarAction.ChangePenSetting(
                    Pen.BALLPEN,
                    it
                )
            )
        })

    if (!GlobalAppSettings.current.monochromeMode) {
        listOf(
            Triple(
                Pen.REDBALLPEN,
                R.drawable.ballpenred,
                android.graphics.Color.RED
            ),
            Triple(
                Pen.BLUEBALLPEN,
                R.drawable.ballpenblue,
                android.graphics.Color.BLUE
            ),
            Triple(
                Pen.GREENBALLPEN,
                R.drawable.ballpengreen,
                android.graphics.Color.GREEN
            )
        ).forEach { (pen, icon, defaultColor) ->
            PenToolbarButton(
                pen = pen,
                icon = icon,
                isSelected = isSelected(uiState, pen),
                onSelect = { onAction(ToolbarAction.ChangePen(pen)) },
                sizes = SIZES_STROKES_DEFAULT,
                penSetting = uiState.penSettings[pen.penName] ?: PenSetting(
                    5f,
                    defaultColor
                ),
                onChangeSetting = {
                    onAction(
                        ToolbarAction.ChangePenSetting(
                            pen,
                            it
                        )
                    )
                },
            )
        }
    }

    if (GlobalAppSettings.current.neoTools) {
        PenToolbarButton(
            pen = Pen.PENCIL,
            icon = R.drawable.pencil,
            isSelected = isSelected(uiState, Pen.PENCIL),
            onSelect = { onAction(ToolbarAction.ChangePen(Pen.PENCIL)) },
            sizes = SIZES_STROKES_DEFAULT,
            penSetting = uiState.penSettings[Pen.PENCIL.penName] ?: PenSetting(
                5f,
                android.graphics.Color.BLACK
            ),
            onChangeSetting = {
                onAction(
                    ToolbarAction.ChangePenSetting(
                        Pen.PENCIL,
                        it
                    )
                )
            }
        )

        PenToolbarButton(
            pen = Pen.BRUSH,
            icon = R.drawable.brush,
            isSelected = isSelected(uiState, Pen.BRUSH),
            onSelect = { onAction(ToolbarAction.ChangePen(Pen.BRUSH)) },
            sizes = SIZES_STROKES_DEFAULT,
            penSetting = uiState.penSettings[Pen.BRUSH.penName] ?: PenSetting(
                5f,
                android.graphics.Color.BLACK
            ),
            onChangeSetting = {
                onAction(
                    ToolbarAction.ChangePenSetting(
                        Pen.BRUSH,
                        it
                    )
                )
            }
        )
    }
    PenToolbarButton(
        pen = Pen.FOUNTAIN,
        icon = R.drawable.fountain,
        isSelected = isSelected(uiState, Pen.FOUNTAIN),
        onSelect = { onAction(ToolbarAction.ChangePen(Pen.FOUNTAIN)) },
        sizes = SIZES_STROKES_DEFAULT,
        penSetting = uiState.penSettings[Pen.FOUNTAIN.penName] ?: PenSetting(
            5f,
            android.graphics.Color.BLACK
        ),
        onChangeSetting = {
            onAction(
                ToolbarAction.ChangePenSetting(
                    Pen.FOUNTAIN,
                    it
                )
            )
        },
    )

    LineToolbarButton(
        unSelect = { onAction(ToolbarAction.ChangeMode(Mode.Draw)) },
        icon = R.drawable.line,
        isSelected = uiState.mode == Mode.Line,
        onSelect = { onAction(ToolbarAction.ChangeMode(Mode.Line)) },
    )

    divider()

    PenToolbarButton(
        pen = Pen.MARKER,
        icon = R.drawable.marker,
        isSelected = isSelected(uiState, Pen.MARKER),
        onSelect = { onAction(ToolbarAction.ChangePen(Pen.MARKER)) },
        sizes = SIZES_MARKER_DEFAULT,
        penSetting = uiState.penSettings[Pen.MARKER.penName] ?: PenSetting(
            40f,
            android.graphics.Color.LTGRAY
        ),
        onChangeSetting = {
            onAction(
                ToolbarAction.ChangePenSetting(
                    Pen.MARKER,
                    it
                )
            )
        }
    )

    divider()

    EraserToolbarButton(
        isSelected = uiState.mode == Mode.Erase,
        onSelect = { onAction(ToolbarAction.ChangeMode(Mode.Erase)) },
        value = uiState.eraser,
        onChange = { onAction(ToolbarAction.ChangeEraser(it)) },
        toggleScribbleToErase = { onAction(ToolbarAction.ToggleScribbleToErase(it)) },
        onMenuOpenChange = { onAction(ToolbarAction.ToggleEraserManu(it)) },
        isMenuOpen = uiState.isStrokeSelectionOpen
    )

    divider()

    ToolbarButton(
        isSelected = uiState.mode == Mode.Select,
        onSelect = { onAction(ToolbarAction.ChangeMode(Mode.Select)) },
        iconId = R.drawable.lasso,
        contentDescription = "lasso"
    )

    divider()

    ToolbarButton(
        iconId = R.drawable.image,
        contentDescription = "Image picker",
        onSelect = pickImage
    )

    divider()

    if (uiState.hasClipboard) {
        ToolbarButton(
            vectorIcon = FeatherIcons.Clipboard,
            contentDescription = "paste",
            onSelect = { onAction(ToolbarAction.Paste) }
        )
        divider()
    }

    if (uiState.showResetView) {
        ToolbarButton(
            vectorIcon = FeatherIcons.RefreshCcw,
            contentDescription = "reset zoom and scroll",
            onSelect = { onAction(ToolbarAction.ResetView) }
        )
        divider()
    }
}

/**
 * Fixed-end actions shared by both orientations: undo/redo, page number,
 * daily journal navigation, home, menu. In the vertical layout the date text
 * is replaced by a calendar button opening the date picker (a full ISO date
 * does not fit in a 37dp-wide bar).
 */
@Composable
private fun ActionButtons(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    divider: @Composable () -> Unit,
    vertical: Boolean,
    position: AppSettings.Position,
) {
    divider()

    ToolbarButton(
        onSelect = { onAction(ToolbarAction.Undo) },
        iconId = R.drawable.undo,
        contentDescription = "undo"
    )
    ToolbarButton(
        onSelect = { onAction(ToolbarAction.Redo) },
        iconId = R.drawable.redo,
        contentDescription = "redo"
    )

    divider()

    if (uiState.notebookId != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (vertical) Modifier
                .width(BUTTON_SIZE.dp)
                .padding(vertical = 6.dp)
            else Modifier
                .height(35.dp)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = uiState.pageNumberInfo,
                fontWeight = FontWeight.Light,
                fontSize = if (vertical) 11.sp else TextUnit.Unspecified,
                maxLines = 1,
                modifier = Modifier.noRippleClickable { onAction(ToolbarAction.NavigateToPages) },
                textAlign = TextAlign.Center
            )
        }
        divider()
    }

    if (uiState.dailyDate != null) {
        ToolbarButton(
            vectorIcon = FeatherIcons.ChevronLeft,
            contentDescription = "previous day",
            onSelect = { onAction(ToolbarAction.PreviousDay) }
        )
        if (vertical) {
            ToolbarButton(
                vectorIcon = FeatherIcons.Calendar,
                contentDescription = "pick date",
                onSelect = { onAction(ToolbarAction.ToggleDatePicker(true)) }
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(35.dp)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = uiState.dailyDate,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.noRippleClickable {
                        onAction(ToolbarAction.ToggleDatePicker(true))
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
        ToolbarButton(
            vectorIcon = FeatherIcons.ChevronRight,
            contentDescription = "next day",
            onSelect = { onAction(ToolbarAction.NextDay) }
        )
        ToolbarButton(
            vectorIcon = FeatherIcons.RotateCw,
            contentDescription = "refresh daily template",
            onSelect = { onAction(ToolbarAction.RefreshDailyTemplate) }
        )
        divider()
    }

    ToolbarButton(
        vectorIcon = FeatherIcons.Grid,
        contentDescription = "change background",
        onSelect = { onAction(ToolbarAction.ToggleBackgroundSelector(true)) }
    )

    ToolbarButton(
        iconId = R.drawable.home,
        contentDescription = "library",
        onSelect = { onAction(ToolbarAction.NavigateToHome) }
    )

    divider()

    Column {
        ToolbarButton(
            onSelect = { onAction(ToolbarAction.ToggleMenu) },
            iconId = R.drawable.menu,
            contentDescription = "menu"
        )
        if (uiState.isMenuOpen) {
            ToolbarMenu(
                uiState = uiState,
                onAction = onAction,
                position = position
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(Color.Black)
    )
}

@Composable
private fun HorizontalDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.Black)
    )
}

// Solid 1dp line separating the toolbar from the page.
@Composable
private fun EdgeLineHorizontal() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black)
    )
}

@Composable
private fun EdgeLineVertical() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color.Black)
    )
}

fun presentlyUsedToolIcon(mode: Mode, pen: Pen): Int {
    return when (mode) {
        Mode.Draw -> {
            when (pen) {
                Pen.BALLPEN -> R.drawable.ballpen
                Pen.REDBALLPEN -> R.drawable.ballpenred
                Pen.BLUEBALLPEN -> R.drawable.ballpenblue
                Pen.GREENBALLPEN -> R.drawable.ballpengreen
                Pen.FOUNTAIN -> R.drawable.fountain
                Pen.BRUSH -> R.drawable.brush
                Pen.MARKER -> R.drawable.marker
                Pen.PENCIL -> R.drawable.pencil
                Pen.DASHED -> R.drawable.line_dashed
            }
        }

        Mode.Erase -> R.drawable.eraser
        Mode.Select -> R.drawable.lasso
        Mode.Line -> R.drawable.line
    }
}

@Composable
@Preview(showBackground = true, widthDp = 1200)
fun ToolbarPreview() {
    val uiState = ToolbarUiState(
        isToolbarOpen = true,
        mode = Mode.Draw,
        pen = Pen.BALLPEN,
        penSettings = mapOf(
            Pen.BALLPEN.penName to PenSetting(5f, android.graphics.Color.BLACK),
            Pen.MARKER.penName to PenSetting(40f, android.graphics.Color.LTGRAY)
        ),
        pageNumberInfo = "3/12",
        notebookId = "dummy_book"
    )

    ToolbarContent(
        uiState = uiState,
        onAction = {},
        onDrawingStateCheck = {}
    )
}
