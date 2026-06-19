package com.example.alfcapp.features.chat.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val DATE_HEADER = DateTimeFormatter.ofPattern("EEE d MMM yyyy").withZone(ZoneId.systemDefault())
private val DATE_HEADER_THIS_YEAR = DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())

/** "Just now" / "5m ago" / "Yesterday" / "Tue" / "12 May" / "12 May 2025". */
fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val sent = Instant.parse(iso)
        val now = Instant.now()
        val mins = Duration.between(sent, now).toMinutes()
        val today = LocalDate.now()
        val sentDate = sent.atZone(ZoneId.systemDefault()).toLocalDate()
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m"
            sentDate == today -> TIME_OF_DAY.format(sent)
            sentDate == today.minusDays(1) -> "Yesterday"
            sentDate.year == today.year -> DATE_HEADER_THIS_YEAR.format(sent)
            else -> DATE_HEADER.format(sent)
        }
    } catch (_: Exception) { "" }
}

/** "HH:mm" inside a bubble. */
fun timeOfDay(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try { TIME_OF_DAY.format(Instant.parse(iso)) } catch (_: Exception) { "" }
}

/** "Today" / "Yesterday" / "Tue 12 May" / "12 May 2024". */
fun dateHeader(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val sent = Instant.parse(iso)
        val sentDate = sent.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        when {
            sentDate == today -> "Today"
            sentDate == today.minusDays(1) -> "Yesterday"
            sentDate.year == today.year -> DATE_HEADER_THIS_YEAR.format(sent)
            else -> DATE_HEADER.format(sent)
        }
    } catch (_: Exception) { "" }
}

/** True if both ISO timestamps fall on the same calendar day in the system zone. */
fun sameLocalDay(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    return try {
        val da = Instant.parse(a).atZone(ZoneId.systemDefault()).toLocalDate()
        val db = Instant.parse(b).atZone(ZoneId.systemDefault()).toLocalDate()
        da == db
    } catch (_: Exception) { false }
}
