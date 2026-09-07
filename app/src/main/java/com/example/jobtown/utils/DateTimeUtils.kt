package com.example.jobtown.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * JobTown always displays date/times in Malaysia time, regardless of the
 * device's own time zone setting, since jobs/interviews are Malaysia-based.
 */
private val malaysiaTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

/**
 * Short, fixed label for Malaysia time, e.g. "GMT+8".
 */
fun currentTimeZoneLabel(): String {
    val offsetMillis = malaysiaTimeZone.getOffset(System.currentTimeMillis())
    val totalMinutes = offsetMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = kotlin.math.abs(totalMinutes % 60)
    val sign = if (totalMinutes >= 0) "+" else "-"
    return if (minutes == 0) {
        "GMT$sign${kotlin.math.abs(hours)}"
    } else {
        "GMT$sign${kotlin.math.abs(hours)}:${minutes.toString().padStart(2, '0')}"
    }
}

// Supabase / Postgres timestamptz values can come back in a few shapes
// depending on precision, so try each known pattern in turn.
private val incomingTimestampPatterns = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'"
)

private fun parseIncomingTimestamp(rawTimestamp: String): java.util.Date? {
    val trimmed = rawTimestamp.trim()
    if (trimmed.isBlank()) return null
    for (pattern in incomingTimestampPatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US)
            // Patterns ending in a literal 'Z' represent UTC, so the parser
            // needs to be told that explicitly; the offset-suffixed (XXX)
            // patterns already carry their own offset and don't need this.
            if (pattern.endsWith("'Z'")) {
                parser.timeZone = TimeZone.getTimeZone("UTC")
            }
            parser.parse(trimmed)?.let { return it }
        } catch (_: Exception) {
            // Try the next pattern.
        }
    }
    return null
}

/**
 * Formats a raw ISO-8601 timestamp (e.g. an "applied_at" value from Supabase)
 * into a friendly Malaysia-time date/time string with a trailing time zone
 * label, e.g. "7 Sep 2026, 6:42 PM (GMT+8)".
 *
 * Falls back to the raw string if it can't be parsed.
 */
fun formatTimestampWithTimeZone(rawTimestamp: String): String {
    if (rawTimestamp.isBlank()) return "Recently"
    val parsed = parseIncomingTimestamp(rawTimestamp) ?: return rawTimestamp
    val outFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).apply {
        timeZone = malaysiaTimeZone
    }
    return "${outFormat.format(parsed)} (${currentTimeZoneLabel()})"
}

/**
 * Compact, date-only variant (no time) for tight spaces like list cards,
 * e.g. "7 Sep 2026 (GMT+8)".
 */
fun formatDateWithTimeZone(rawTimestamp: String): String {
    if (rawTimestamp.isBlank()) return "Recently"
    val parsed = parseIncomingTimestamp(rawTimestamp) ?: return rawTimestamp
    val outFormat = SimpleDateFormat("d MMM yyyy", Locale.US).apply {
        timeZone = malaysiaTimeZone
    }
    return "${outFormat.format(parsed)} (${currentTimeZoneLabel()})"
}