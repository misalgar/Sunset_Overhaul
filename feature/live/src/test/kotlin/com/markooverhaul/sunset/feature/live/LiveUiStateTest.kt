package com.markooverhaul.sunset.feature.live

import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LIVE_EVENT_START_GRACE_MS
import com.markooverhaul.sunset.core.model.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUiStateTest {

    private fun channel(
        id: String,
        categoryId: String,
        adult: Boolean = false,
    ) = LiveChannel(
        id = id,
        name = "Channel $id",
        number = id.toInt(),
        categoryIds = listOf(categoryId),
        isAdult = adult,
    )

    private val state = LiveUiState(
        isLoading = false,
        categories = listOf(
            LiveSidebarCategory(id = "sports", name = "Deportes", count = 2),
        ),
        channels = listOf(
            channel("1", "sports"),
            channel("2", "sports"),
            channel("3", "adult", adult = true),
        ),
        favourites = setOf("1", "3"),
    )

    @Test
    fun `all channels excludes adult entries`() {
        val visible = state.copy(selectedFilter = LiveFilter.All).visibleChannels
        assertEquals(listOf("1", "2"), visible.map { it.id })
    }

    @Test
    fun `favourites never expose adult entries`() {
        val visible = state.copy(selectedFilter = LiveFilter.Favourites).visibleChannels
        assertEquals(listOf("1"), visible.map { it.id })
    }

    @Test
    fun `the adult row is the only way to see adult channels`() {
        val visible = state.copy(selectedFilter = LiveFilter.Adult).visibleChannels
        assertEquals(listOf("3"), visible.map { it.id })
    }

    @Test
    fun `category filters exclude adult channels`() {
        val visible = state.copy(
            selectedFilter = LiveFilter.Category("sports"),
        ).visibleChannels
        assertEquals(listOf("1", "2"), visible.map { it.id })
    }

    @Test
    fun `counts exclude adult channels except the adult total`() {
        assertEquals(2, state.allChannelCount)
        assertEquals(1, state.favouriteChannelCount)
        assertEquals(1, state.adultChannelCount)
    }

    private fun eventChannel(startUtcMs: Long) = LiveChannel(
        id = "event",
        name = "Fleetwood Town vs Arsenal",
        number = 99,
        scheduledStartUtcMs = startUtcMs,
    )

    @Test
    fun `a past event is live and keeps its start time`() {
        val start = 1_000_000L
        val status = channelStatus(
            eventChannel(start),
            programme = null,
            nowMs = start + 60_000L,
        )
        assertTrue(status is ChannelStatus.Live)
        status as ChannelStatus.Live
        assertEquals(start, status.startedAtEpochMillis)
        assertNull(status.programmeTitle)
    }

    @Test
    fun `a future event stays upcoming until the start grace runs out`() {
        val start = 1_000_000L
        assertEquals(
            ChannelStatus.Upcoming(start),
            channelStatus(eventChannel(start), null, start - LIVE_EVENT_START_GRACE_MS - 1),
        )
        val live = channelStatus(eventChannel(start), null, start - LIVE_EVENT_START_GRACE_MS)
        assertTrue(live is ChannelStatus.Live)
        assertEquals(start, (live as ChannelStatus.Live).startedAtEpochMillis)
    }

    @Test
    fun `a 24-7 channel shows its programme without an event start`() {
        val now = 5_000_000L
        val programme = EpgProgramme(
            id = "p1",
            channelId = "1",
            title = "News",
            startUtcMs = now - 15 * 60_000L,
            endUtcMs = now + 45 * 60_000L,
        )
        val status = channelStatus(channel("1", "sports"), programme, now)
        assertTrue(status is ChannelStatus.Live)
        status as ChannelStatus.Live
        assertEquals("News", status.programmeTitle)
        // Only event/PPV feeds publish a start; an EPG slot is not an event start.
        assertNull(status.startedAtEpochMillis)
        assertEquals(0.25f, status.progress!!, 0.0001f)
    }

    @Test
    fun `a future programme does not invent an event start`() {
        val now = 5_000_000L
        val programme = EpgProgramme(
            id = "p1",
            channelId = "1",
            title = "Later",
            startUtcMs = now + 60_000L,
            endUtcMs = now + 120_000L,
        )
        val status = channelStatus(channel("1", "sports"), programme, now) as ChannelStatus.Live
        assertNull(status.startedAtEpochMillis)
    }
}
