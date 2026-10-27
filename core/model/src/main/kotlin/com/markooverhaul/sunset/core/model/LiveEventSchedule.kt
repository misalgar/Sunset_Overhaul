package com.markooverhaul.sunset.core.model

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** A provider event feed with a start time embedded in its name; null when no schedule is known. */
data class LiveEventSchedule(
    val startsAtUtcMs: Long,
    /** Channel name with the leading `HH:mm` (and AM/PM marker) removed. */
    val displayName: String,
)

/**
 * The panel activates these event streams a few minutes before kick-off and occasionally a little
 * early. Until then the card should read "Starts …" rather than LIVE.
 */
const val LIVE_EVENT_START_GRACE_MS = 10 * 60 * 1000L

/** True while [startUtcMs] is still ahead of [nowUtcMs] once the panel's start grace is applied. */
fun isUpcomingEvent(startUtcMs: Long?, nowUtcMs: Long): Boolean =
    startUtcMs != null && nowUtcMs < startUtcMs - LIVE_EVENT_START_GRACE_MS

/** True while this event's start is still ahead of [nowUtcMs] once the start grace is applied. */
fun LiveEventSchedule.isUpcoming(nowUtcMs: Long): Boolean =
    isUpcomingEvent(startsAtUtcMs, nowUtcMs)

/**
 * The clock can lead the name (`21:10 Tijuana vs …`) or sit behind a panel prefix
 * (`NHL 01 | 05:00 PM | Columbus Blue Jackets vs …`). Matching the first clock token anywhere
 * keeps one parser for every event category; names without a clock (for example DAZN) stay raw.
 */
private val EventNameTime = Regex(
    """(?:^|[\s|·•:\-–—])(\d{1,2})[:;](\d{2})\s*(?:([ap])\.?\s*m\.?)?\s*(?:[|·•:\-–—]\s*)?(.*)$""",
    RegexOption.IGNORE_CASE,
)

/** Separators the panel wraps around the clock and matchup; trimmed from the display name. */
private val DisplaySeparators = charArrayOf('|', '·', '•', ':', ' ')

private val CategoryDate = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{2,4})\b""")

private val WeekdayNames = mapOf(
    "domingo" to Calendar.SUNDAY,
    "lunes" to Calendar.MONDAY,
    "martes" to Calendar.TUESDAY,
    "miercoles" to Calendar.WEDNESDAY,
    "jueves" to Calendar.THURSDAY,
    "viernes" to Calendar.FRIDAY,
    "sabado" to Calendar.SATURDAY,
    "sunday" to Calendar.SUNDAY,
    "monday" to Calendar.MONDAY,
    "tuesday" to Calendar.TUESDAY,
    "wednesday" to Calendar.WEDNESDAY,
    "thursday" to Calendar.THURSDAY,
    "friday" to Calendar.FRIDAY,
    "saturday" to Calendar.SATURDAY,
)

/**
 * Reads the provider's event schedule from the channel name plus the dated category
 * (`21:10 Tijuana vs …` / `NFL 25/10/26`).
 *
 * The panel publishes event times in its own timezone; [panelNowUtcMs] and [panelTimeZoneId] come
 * from the Xtream login response (`server_info.time_now` / `timezone`). When the category only
 * names a weekday and it does not match the panel's current day, no schedule is returned: a wrong
 * label is worse than the generic LIVE state.
 */
fun parseLiveEventSchedule(
    rawName: String,
    categoryName: String?,
    panelNowUtcMs: Long,
    panelTimeZoneId: String?,
): LiveEventSchedule? {
    val match = EventNameTime.find(rawName) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (minute !in 0..59) return null

    val marker = match.groupValues[3].lowercase(Locale.US)
    val hour24 = when {
        marker == "p" -> if (hour == 12) 12 else hour + 12
        marker == "a" -> if (hour == 12) 0 else hour
        else -> hour
    }
    if (hour24 !in 0..23) return null

    val zone = TimeZone.getTimeZone(
        panelTimeZoneId?.takeIf { it.isNotBlank() } ?: TimeZone.getDefault().id,
    )
    val calendar = Calendar.getInstance(zone, Locale.US).apply {
        timeInMillis = panelNowUtcMs
    }

    val date = parseCategoryDate(categoryName)
    if (date != null) {
        calendar.isLenient = false
        val applied = runCatching {
            calendar.set(date[0], date[1] - 1, date[2], hour24, minute, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        if (applied.isFailure) return null
    } else {
        val weekday = weekdayOf(categoryName)
        if (weekday != null && calendar.get(Calendar.DAY_OF_WEEK) != weekday) return null
        calendar.set(Calendar.HOUR_OF_DAY, hour24)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    val display = match.groupValues[4].trim().trim(*DisplaySeparators).ifBlank {
        // No matchup after the clock: keep the panel's event label (`NHL 01`) instead of the
        // raw name with the clock still embedded.
        rawName.substring(0, match.range.first)
            .trim()
            .trimEnd(*DisplaySeparators)
            .ifBlank { rawName.trim() }
    }
    return LiveEventSchedule(startsAtUtcMs = calendar.timeInMillis, displayName = display)
}

/** `25/10/26`, `25/10/2026` → `[year, month, day]`, or null when absent/invalid. */
private fun parseCategoryDate(raw: String?): IntArray? {
    if (raw.isNullOrBlank()) return null
    val match = CategoryDate.find(raw) ?: return null
    val day = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    var year = match.groupValues[3].toIntOrNull() ?: return null
    if (year < 100) year += 2000
    if (day !in 1..31 || month !in 1..12 || year !in 2000..2100) return null
    return intArrayOf(year, month, day)
}

private fun weekdayOf(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase(Locale.US)
    return WeekdayNames.entries
        .firstOrNull { (name, _) ->
            Regex("""\b${Regex.escape(name)}\b""").containsMatchIn(normalized)
        }
        ?.value
}
