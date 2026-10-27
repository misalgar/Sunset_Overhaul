package com.markooverhaul.sunset.core.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEventScheduleTest {

    private val panelZone = TimeZone.getTimeZone("America/Mexico_City")

    private fun panelTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
        zone: TimeZone = panelZone,
    ): Long = Calendar.getInstance(zone).apply {
        set(year, month - 1, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `reads a 24-hour event and cleans the name`() {
        val schedule = parseLiveEventSchedule(
            rawName = "21:10 Tijuana v Cruz Azul",
            categoryName = "Live Soccer Domingo",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 25, 21, 10), schedule?.startsAtUtcMs)
        assertEquals("Tijuana v Cruz Azul", schedule?.displayName)
    }

    @Test
    fun `reads 12-hour English names`() {
        val morning = parseLiveEventSchedule(
            rawName = "07:00 AM | Steelers vs. Saints",
            categoryName = "NFL 25/10/26",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 25, 7), morning?.startsAtUtcMs)
        assertEquals("Steelers vs. Saints", morning?.displayName)

        val noon = parseLiveEventSchedule(
            rawName = "12:00 p.m. · Yella Wood 500",
            categoryName = "PAY PER VIEW 25/10/2026",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 25, 12), noon?.startsAtUtcMs)
        assertEquals("Yella Wood 500", noon?.displayName)
    }

    @Test
    fun `reads a clock behind a panel prefix and cleans the name`() {
        val schedule = parseLiveEventSchedule(
            rawName = "NHL 01 | 05:00 PM | Columbus Blue Jackets vs. New York Rangers",
            categoryName = "NHL 27/10/26",
            panelNowUtcMs = panelTime(2026, 10, 27, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 27, 17), schedule?.startsAtUtcMs)
        assertEquals("Columbus Blue Jackets vs. New York Rangers", schedule?.displayName)
    }

    @Test
    fun `reads a prefixed 24-hour clock`() {
        val schedule = parseLiveEventSchedule(
            rawName = "MLB 02 | 19:10 | Dodgers vs Padres",
            categoryName = "MLB 27/10/26",
            panelNowUtcMs = panelTime(2026, 10, 27, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 27, 19, 10), schedule?.startsAtUtcMs)
        assertEquals("Dodgers vs Padres", schedule?.displayName)
    }

    @Test
    fun `keeps the panel label when the prefix has no matchup`() {
        val schedule = parseLiveEventSchedule(
            rawName = "NHL 01 | 05:00 PM |",
            categoryName = "NHL 27/10/26",
            panelNowUtcMs = panelTime(2026, 10, 27, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 27, 17), schedule?.startsAtUtcMs)
        assertEquals("NHL 01", schedule?.displayName)
    }

    @Test
    fun `uses the date from the category`() {
        val schedule = parseLiveEventSchedule(
            rawName = "21:10 Tijuana v Cruz Azul",
            categoryName = "MLB 26/10/26",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 26, 21, 10), schedule?.startsAtUtcMs)
    }

    @Test
    fun `refuses a weekday category on the wrong day`() {
        val schedule = parseLiveEventSchedule(
            rawName = "21:10 Tijuana v Cruz Azul",
            categoryName = "Live Soccer Lunes",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertNull(schedule)
    }

    @Test
    fun `ignores channels without a clock`() {
        assertNull(
            parseLiveEventSchedule(
                rawName = "ESPN",
                categoryName = "Deportes",
                panelNowUtcMs = panelTime(2026, 10, 25, 13),
                panelTimeZoneId = panelZone.id,
            ),
        )
        assertNull(
            parseLiveEventSchedule(
                rawName = "24/7 Noticias",
                categoryName = "Noticias",
                panelNowUtcMs = panelTime(2026, 10, 25, 13),
                panelTimeZoneId = panelZone.id,
            ),
        )
        assertNull(
            parseLiveEventSchedule(
                rawName = "DAZN 1",
                categoryName = "DAZN",
                panelNowUtcMs = panelTime(2026, 10, 25, 13),
                panelTimeZoneId = panelZone.id,
            ),
        )
    }

    @Test
    fun `rejects invalid clock values`() {
        assertNull(
            parseLiveEventSchedule(
                rawName = "25:99 Event",
                categoryName = "Live Soccer Domingo",
                panelNowUtcMs = panelTime(2026, 10, 25, 13),
                panelTimeZoneId = panelZone.id,
            ),
        )
    }

    @Test
    fun `accepts the panel semicolon time separator`() {
        val schedule = parseLiveEventSchedule(
            rawName = "21;10 Tijuana vs Santos",
            categoryName = "Live Soccer Domingo",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        assertEquals(panelTime(2026, 10, 25, 21, 10), schedule?.startsAtUtcMs)
        assertEquals("Tijuana vs Santos", schedule?.displayName)
    }

    @Test
    fun `interprets the name in the panel timezone`() {
        val schedule = parseLiveEventSchedule(
            rawName = "21:10 Tijuana v Cruz Azul",
            categoryName = "Live Soccer Domingo",
            panelNowUtcMs = panelTime(2026, 10, 25, 13),
            panelTimeZoneId = panelZone.id,
        )
        // 21:10 in Mexico City (UTC-6) is 03:10 UTC the next day.
        val expectedUtc = panelTime(2026, 10, 26, 3, 10, TimeZone.getTimeZone("UTC"))
        assertEquals(expectedUtc, schedule?.startsAtUtcMs)
    }

    @Test
    fun `upcoming applies the panel start grace`() {
        val start = 1_000_000L
        val schedule = LiveEventSchedule(startsAtUtcMs = start, displayName = "Match")
        assertTrue(schedule.isUpcoming(start - LIVE_EVENT_START_GRACE_MS - 1))
        assertFalse(schedule.isUpcoming(start - LIVE_EVENT_START_GRACE_MS))
        assertFalse(schedule.isUpcoming(start))
    }

    @Test
    fun `a channel without a parsed schedule is never upcoming`() {
        assertFalse(isUpcomingEvent(null, 0L))
    }
}
