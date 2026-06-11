package com.ethran.notable.ui.views

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethran.notable.navigation.NavigationDestination
import com.ethran.notable.ui.viewmodels.DiagnosticsViewModel

object DiagnosticsDestination : NavigationDestination {
    override val route = "diagnostics"
}

/**
 * Phase-0 spike screen: validates on this exact device/firmware that
 * (A) the firmware MyScript HWR service can be bound and recognizes real handwriting, and
 * (B) today's calendar events are readable from CalendarContract.
 * Monochrome, animation-free, e-ink friendly.
 */
@Composable
fun DiagnosticsView(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hwr by viewModel.hwrState.collectAsStateWithLifecycle()
    val calendar by viewModel.calendarState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshCalendar() }

    LaunchedEffect(Unit) { viewModel.refreshCalendar() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.h5.copy(color = Color.Black),
                    modifier = Modifier.weight(1f)
                )
            }

            Column(Modifier.verticalScroll(rememberScrollState())) {
                DiagSectionTitle("Handwriting recognition (firmware MyScript)")
                DiagText("Service: com.onyx.android.ksync/.service.KHwrService")
                DiagText("Bind state: ${hwr.bindState}")
                Row {
                    DiagButton("Bind service") { viewModel.bindHwrService() }
                    Spacer(Modifier.width(12.dp))
                    DiagButton(
                        if (hwr.recognizing) "Recognizing…" else "Recognize last edited page"
                    ) { if (!hwr.recognizing) viewModel.recognizeLastEditedPage() }
                }
                hwr.testedPageInfo?.let { DiagText("Tested: $it") }
                hwr.latencyMs?.let { DiagText("Latency: $it ms") }
                hwr.recognizedText?.let {
                    DiagText("Recognized text:")
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.Black, fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black)
                            .padding(8.dp)
                    )
                }
                hwr.error?.let { DiagError(it) }

                Spacer(Modifier.height(24.dp))

                DiagSectionTitle("Calendar (CalendarContract.Instances)")
                DiagText(
                    "READ_CALENDAR permission: " +
                            if (calendar.permissionGranted) "granted" else "NOT granted"
                )
                if (!calendar.permissionGranted) {
                    Row {
                        DiagButton("Request permission") {
                            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                        Spacer(Modifier.width(12.dp))
                        DiagButton("Open app settings") {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    }
                } else {
                    DiagButton("List today's events") { viewModel.refreshCalendar() }
                    when {
                        calendar.error != null -> DiagError(calendar.error!!)
                        calendar.events == null -> DiagText("Loading…")
                        calendar.events!!.isEmpty() ->
                            DiagText("No events today (is DAVx5 / a calendar account configured?)")

                        else -> calendar.events!!.forEach { DiagText(it) }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DiagSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.subtitle1.copy(
            color = Color.Black, fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
    androidx.compose.material.Divider(color = Color.Black, thickness = 1.dp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DiagText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2.copy(color = Color.Black),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun DiagError(text: String) {
    Text(
        text = "⚠ $text",
        style = MaterialTheme.typography.body2.copy(
            color = Color.Black, fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .padding(8.dp)
    )
}

@Composable
private fun DiagButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.body1.copy(color = Color.Black),
        modifier = Modifier
            .padding(vertical = 6.dp)
            .border(1.dp, Color.Black)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
