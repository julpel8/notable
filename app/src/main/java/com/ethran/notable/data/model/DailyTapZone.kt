package com.ethran.notable.data.model

/**
 * One finger-tappable rectangle of a daily template, in page coordinates
 * (the template is anchored at the top of the page, so these are stable
 * under scroll/zoom — callers transform screen taps into page space first).
 * Tapping it toggles [valueKey] in the page's DailyValues map.
 */
data class DailyTapZone(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val valueKey: String,
) {
    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom
}

fun List<DailyTapZone>.zoneAt(x: Float, y: Float): DailyTapZone? =
    firstOrNull { it.contains(x, y) }
