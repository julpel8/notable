package com.ethran.notable.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethran.notable.SCREEN_HEIGHT
import com.ethran.notable.SCREEN_WIDTH
import com.ethran.notable.data.CalendarRepository
import com.ethran.notable.data.db.PageDao
import com.ethran.notable.data.db.PageRepository
import com.ethran.notable.io.OnyxHWREngine
import com.ethran.notable.utils.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Backs the Diagnostics screen: the two Phase-0 de-risking spikes, runnable on-device.
 * Spike A: bind the Boox firmware MyScript service and recognize a handwritten page.
 * Spike B: read today's events from CalendarContract (fed by DAVx5).
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pageDao: PageDao,
    private val pageRepository: PageRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    data class HwrUiState(
        val bindState: String = "Not bound",
        val bindOk: Boolean? = null,
        val recognizing: Boolean = false,
        val testedPageInfo: String? = null,
        val recognizedText: String? = null,
        val error: String? = null,
        val latencyMs: Long? = null,
    )

    data class CalendarUiState(
        val permissionGranted: Boolean = false,
        val events: List<String>? = null,
        val error: String? = null,
    )

    private val _hwrState = MutableStateFlow(HwrUiState())
    val hwrState: StateFlow<HwrUiState> = _hwrState.asStateFlow()

    private val _calendarState = MutableStateFlow(CalendarUiState())
    val calendarState: StateFlow<CalendarUiState> = _calendarState.asStateFlow()

    fun bindHwrService() {
        _hwrState.update { it.copy(bindState = "Binding…", bindOk = null, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val ok = try {
                OnyxHWREngine.bindAndAwait(context)
            } catch (e: Exception) {
                _hwrState.update { it.copy(error = "Bind threw: ${e.message}") }
                false
            }
            val elapsed = System.currentTimeMillis() - start
            _hwrState.update {
                it.copy(
                    bindState = if (ok) "Bound (${elapsed} ms)"
                    else "Bind FAILED (${elapsed} ms) — service com.onyx.android.ksync/.service.KHwrService unavailable on this firmware?",
                    bindOk = ok,
                )
            }
        }
    }

    /**
     * Run recognition on the most recently edited page that has ink — that is the
     * actual spike: real handwriting, this device, this firmware.
     */
    fun recognizeLastEditedPage() {
        _hwrState.update {
            it.copy(recognizing = true, recognizedText = null, error = null, latencyMs = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!OnyxHWREngine.bindAndAwait(context)) {
                    _hwrState.update {
                        it.copy(recognizing = false, error = "HWR service not available")
                    }
                    return@launch
                }
                val page = pageDao.getLastEditedPageWithStrokes()
                if (page == null) {
                    _hwrState.update {
                        it.copy(recognizing = false, error = "No page with strokes found — write something first")
                    }
                    return@launch
                }
                val data = pageRepository.getWithDataById(page.id)
                if (data == null || data.strokes.isEmpty()) {
                    _hwrState.update {
                        it.copy(recognizing = false, error = "Page ${page.id} has no stroke data")
                    }
                    return@launch
                }
                val start = System.currentTimeMillis()
                val text = OnyxHWREngine.recognizeStrokes(
                    data.strokes, SCREEN_WIDTH.toFloat(), SCREEN_HEIGHT.toFloat()
                )
                val elapsed = System.currentTimeMillis() - start
                _hwrState.update {
                    it.copy(
                        recognizing = false,
                        testedPageInfo = "page ${page.id.take(8)}…, ${data.strokes.size} strokes",
                        recognizedText = when {
                            text == null -> null
                            text.isBlank() -> "(empty result)"
                            else -> text.take(500)
                        },
                        error = if (text == null) "Recognition returned null (timeout or service gone)" else null,
                        latencyMs = elapsed,
                    )
                }
            } catch (e: Exception) {
                _hwrState.update {
                    it.copy(recognizing = false, error = "Recognition threw: ${e.message}")
                }
            }
        }
    }

    fun refreshCalendar() {
        val granted = calendarRepository.hasPermission()
        if (!granted) {
            _calendarState.update {
                it.copy(permissionGranted = false, events = null, error = null)
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = calendarRepository.getEventsForDay(LocalDate.now())) {
                is AppResult.Success -> _calendarState.update {
                    it.copy(
                        permissionGranted = true,
                        events = result.data.map { e -> formatEvent(e) },
                        error = null,
                    )
                }

                is AppResult.Error -> _calendarState.update {
                    it.copy(permissionGranted = true, events = null, error = result.error.userMessage)
                }
            }
        }
    }

    private fun formatEvent(e: CalendarRepository.CalendarEvent): String {
        val time = if (e.allDay) "all-day" else {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val zone = ZoneId.systemDefault()
            val begin = Instant.ofEpochMilli(e.beginMillis).atZone(zone).format(fmt)
            val end = Instant.ofEpochMilli(e.endMillis).atZone(zone).format(fmt)
            "$begin–$end"
        }
        return "[$time] ${e.title} — ${e.calendarName}"
    }

    override fun onCleared() {
        super.onCleared()
        OnyxHWREngine.unbind(context)
    }
}
