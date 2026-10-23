package com.markooverhaul.sunset.core.ui.util

import android.content.Context
import android.text.format.DateFormat
import com.markooverhaul.sunset.core.ui.R
import java.util.Date
import java.util.Locale

/** Locale-aware formatting for durations, clock times, dates and progress. */
object SunsetFormat {

    fun runtime(context: Context, minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val hours = minutes / 60
        val remaining = minutes % 60
        return when {
            hours > 0 && remaining > 0 -> context.getString(R.string.runtime_hours_minutes, hours, remaining)
            hours > 0 -> context.getString(R.string.runtime_hours, hours)
            else -> context.getString(R.string.runtime_minutes, remaining)
        }
    }

    fun time(context: Context, epochMs: Long): String =
        DateFormat.getTimeFormat(context).format(Date(epochMs))

    fun date(context: Context, epochMs: Long): String =
        DateFormat.getMediumDateFormat(context).format(Date(epochMs))

    fun timeRange(context: Context, startMs: Long, endMs: Long): String =
        context.getString(R.string.time_range, time(context, startMs), time(context, endMs))

    fun remaining(context: Context, positionMs: Long, durationMs: Long): String? {
        if (durationMs <= 0L) return null
        val remainingMinutes = ((durationMs - positionMs).coerceAtLeast(0L) / 60_000L).toInt()
        return context.getString(R.string.minutes_remaining, remainingMinutes)
    }

    fun clock(positionMs: Long): String {
        val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
