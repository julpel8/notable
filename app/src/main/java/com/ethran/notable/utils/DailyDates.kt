package com.ethran.notable.utils

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure date helpers for the daily journal. Dates are always device-local
 * ISO yyyy-MM-dd strings (LocalDate.toString()); never UTC-derived.
 * All functions take a Clock so tests can pin "today".
 */

fun todayIso(clock: Clock = Clock.systemDefaultZone()): String =
    LocalDate.now(clock).toString()

fun shiftDay(iso: String, delta: Long): String =
    LocalDate.parse(iso).plusDays(delta).toString()

fun isToday(iso: String, clock: Clock = Clock.systemDefaultZone()): Boolean =
    iso == todayIso(clock)

/**
 * Banner line for the daily template, e.g. "mercredi 10 juin 2026" (system
 * locale decides the language).
 */
fun formatBannerDate(iso: String, locale: Locale = Locale.getDefault()): String {
    val date = LocalDate.parse(iso)
    return date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale))
}
