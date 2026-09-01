package com.example.jobtown.ui.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.jobtown.data.model.InterviewSchedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MeetingKind { ONLINE, PHYSICAL }

fun parseTimeRange(time: String): Pair<String, String> {
    val trimmed = time.trim()
    if (trimmed.isBlank()) return "10:00 AM" to "11:00 AM"
    val parts = trimmed
        .split("–", "—", "-", " to ", " TO ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val start = parts.getOrNull(0)?.ifBlank { "10:00 AM" } ?: "10:00 AM"
    val end = parts.getOrNull(1)?.ifBlank { "" }.orEmpty()
    return start to end
}

fun formatTimeRange(start: String, end: String): String {
    val cleanStart = start.trim().ifBlank { "10:00 AM" }
    val cleanEnd = end.trim()
    return if (cleanEnd.isBlank()) cleanStart else "$cleanStart – $cleanEnd"
}

fun formatDisplayDate(isoDate: String): String {
    if (isoDate.isBlank()) return "Date not set"
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoDate)
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(parsed ?: Date())
    } catch (_: Exception) {
        isoDate
    }
}

fun formatShortDate(isoDate: String): String {
    if (isoDate.isBlank()) return "Pick a date"
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoDate)
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(parsed ?: Date())
    } catch (_: Exception) {
        isoDate
    }
}

fun detectMeetingKind(locationOrLink: String): MeetingKind {
    val value = locationOrLink.trim()
    if (value.startsWith("map:", ignoreCase = true)) return MeetingKind.PHYSICAL
    if (value.startsWith("meet:", ignoreCase = true)) return MeetingKind.ONLINE
    if (isOnlineLocation(value)) return MeetingKind.ONLINE
    return if (value.isBlank()) MeetingKind.ONLINE else MeetingKind.PHYSICAL
}

fun meetingDisplayValue(locationOrLink: String): String {
    val value = locationOrLink.trim()
    return when {
        value.startsWith("meet:", ignoreCase = true) -> value.substringAfter(":")
        value.startsWith("map:", ignoreCase = true) -> value.substringAfter(":")
        else -> value
    }
}

fun encodeMeeting(kind: MeetingKind, value: String): String {
    val clean = value.trim()
    return if (kind == MeetingKind.ONLINE) {
        val link = clean.ifBlank { "https://meet.google.com/new" }
        if (link.startsWith("meet:", ignoreCase = true)) link else "meet:$link"
    } else {
        if (clean.startsWith("map:", ignoreCase = true)) clean else "map:$clean"
    }
}

fun isOnlineLocation(value: String): Boolean {
    val target = value.trim()
    if (target.isBlank()) return false
    return target.startsWith("http://", ignoreCase = true) ||
        target.startsWith("https://", ignoreCase = true) ||
        target.contains("meet.google", ignoreCase = true) ||
        target.contains("zoom.us", ignoreCase = true) ||
        target.contains("teams.microsoft", ignoreCase = true) ||
        target.startsWith("meet:", ignoreCase = true)
}

fun InterviewSchedule.isCancelledInterview(): Boolean =
    status.equals("Cancelled", ignoreCase = true) || status.equals("Rejected", ignoreCase = true)

fun openInterviewDestination(context: Context, locationOrLink: String) {
    val kind = detectMeetingKind(locationOrLink)
    val raw = meetingDisplayValue(locationOrLink)
    try {
        val intent = if (kind == MeetingKind.ONLINE) {
            val url = when {
                raw.isBlank() -> "https://meet.google.com/new"
                raw.startsWith("http://") || raw.startsWith("https://") -> raw
                else -> "https://$raw"
            }
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            val query = Uri.encode(raw.ifBlank { "Malaysia" })
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            if (kind == MeetingKind.ONLINE) "Unable to open Google Meet" else "Unable to open Google Maps",
            Toast.LENGTH_SHORT
        ).show()
    }
}
