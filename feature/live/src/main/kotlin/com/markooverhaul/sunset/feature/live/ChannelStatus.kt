package com.markooverhaul.sunset.feature.live

import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.isUpcomingEvent

/** Status of a live tile: an event still in the future, or a feed that is on air. */
sealed interface ChannelStatus {

    /** The panel's start grace has not run out yet, so the card reads "Starts …". */
    data class Upcoming(val startsAtEpochMillis: Long) : ChannelStatus

    /**
     * The feed is on air. [startedAtEpochMillis] is the event's own start when the provider
     * published one, so sports and PPV tiles can say when they began instead of only "Live".
     * Ordinary 24/7 channels never carry a start here: their EPG programme start is not an event
     * start and repeating it on every tile only added noise.
     */
    data class Live(
        val programmeTitle: String? = null,
        val startedAtEpochMillis: Long? = null,
        val progress: Float? = null,
    ) : ChannelStatus
}

/**
 * Event feeds publish their start time in the name; the provider parses it once and stores it on
 * [LiveChannel.scheduledStartUtcMs]. Until kick-off (with the panel's start grace) the tile reads
 * "Starts …"; afterwards the start time is kept so the live status can say when the feed began.
 * Only event/PPV channels have a scheduled start, so regular channels stay a plain "Live".
 */
fun channelStatus(
    channel: LiveChannel,
    programme: EpgProgramme?,
    nowMs: Long,
): ChannelStatus {
    val scheduledStart = channel.scheduledStartUtcMs
    if (scheduledStart != null && isUpcomingEvent(scheduledStart, nowMs)) {
        return ChannelStatus.Upcoming(scheduledStart)
    }
    return ChannelStatus.Live(
        programmeTitle = programme?.title,
        // Only an event's own start is meaningful here; EPG programme starts belong to the guide.
        startedAtEpochMillis = scheduledStart,
        progress = programme?.let {
            val span = (it.endUtcMs - it.startUtcMs).coerceAtLeast(1L)
            ((nowMs - it.startUtcMs).toFloat() / span).coerceIn(0f, 1f)
        },
    )
}
