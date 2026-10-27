package com.markooverhaul.sunset.feature.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LIVE_EVENT_START_GRACE_MS
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.util.SunsetFormat

/** Status line for a live tile: a scheduled start when the feed has not begun, else EPG progress. */
data class ChannelStatus(
    val startsAtLabel: String?,
    val progress: Float?,
)

/**
 * Event feeds publish their start time in the name. Until kick-off (with the panel's start grace)
 * the tile reads "Starts …"; afterwards the EPG programme drives the live dot and progress.
 */
@Composable
fun rememberChannelStatus(
    channel: LiveChannel,
    programme: EpgProgramme?,
    nowMs: Long,
): ChannelStatus {
    val context = LocalContext.current
    val start = channel.scheduledStartUtcMs
    val scheduled = start != null && nowMs < start - LIVE_EVENT_START_GRACE_MS
    return ChannelStatus(
        startsAtLabel = if (scheduled) {
            stringResource(R.string.live_starts_at, SunsetFormat.time(context, start))
        } else {
            null
        },
        progress = if (!scheduled) {
            programme?.let {
                val span = (it.endUtcMs - it.startUtcMs).coerceAtLeast(1L)
                ((nowMs - it.startUtcMs).toFloat() / span).coerceIn(0f, 1f)
            }
        } else {
            null
        },
    )
}
