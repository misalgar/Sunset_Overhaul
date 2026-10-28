package com.markooverhaul.sunset.feature.guide

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Baseline snapshot of the TV guide grid.
 *
 * The guide draws a "current time" marker from [System.currentTimeMillis], so the programme
 * blocks are laid out across the whole local day: whatever time the snapshot is taken, one block
 * is on air and the ruler marker lands inside it.
 *
 * All artwork URLs are null, so no network or provider is involved.
 *
 * Run `recordPaparazziDebug` to refresh the golden and `verifyPaparazziDebug` to compare against
 * it.
 */
class GuideScreenPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
            screenWidth = 1920,
            screenHeight = 1080,
            orientation = ScreenOrientation.LANDSCAPE,
            // 320 dpi (xhdpi) is the 1080p TV canvas: 1920x1080 px becomes 960x540 dp.
            // PIXEL_6_PRO ships 560 dpi, which would give a 548 dp phone-width viewport.
            density = Density.XHIGH,
        ),
        maxPercentDifference = 0.1,
    )

    private lateinit var defaultLocale: Locale
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinLocaleAndTimeZone() {
        defaultLocale = Locale.getDefault()
        defaultTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun snapshotGuideScreen() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                GuideScreen(
                    state = state(),
                    onPlayChannel = {},
                    onBack = {},
                    onSelectDay = {},
                    onToggleFavouritesOnly = {},
                    onSelectCountry = {},
                    onOpenCountryPicker = {},
                    onDismissCountryPicker = {},
                    onRetry = {},
                    // Freeze the clock: the guide draws a "now" marker, a countdown and a clock,
                    // so without this the golden would drift every minute.
                    nowProvider = { FixedNowUtcMs },
                )
            }
        }
    }

    private companion object {
        /** 2024-09-10T19:15:00Z. Frozen so the guide's "now" marker never moves. */
        const val FixedNowUtcMs = 1_725_995_700_000L

        /** A channel's schedule: block length, first block offset and its own programme titles. */
        data class Plan(
            val id: String,
            val name: String,
            val number: Int,
            val blockMinutes: Long,
            val firstOffsetMinutes: Long,
            val titles: List<String>,
        )

        /**
         * Each channel has its own titles and its own schedule, so a block that shows the wrong
         * programme or the wrong time is visible in the golden instead of being masked by
         * identical rows. The blocks tile the whole day, so a programme is always on air at
         * [FixedNowUtcMs].
         */
        val Plans = listOf(
            Plan(
                id = "c1",
                name = "BBC One",
                number = 1,
                blockMinutes = 240,
                firstOffsetMinutes = 0,
                titles = listOf(
                    "Morning News",
                    "The One Show",
                    "BBC News at Six",
                    "Match of the Day",
                    "Evening Drama",
                    "Late Film",
                ),
            ),
            Plan(
                id = "c2",
                name = "Sky Sports Main Event",
                number = 2,
                blockMinutes = 240,
                firstOffsetMinutes = 0,
                titles = listOf(
                    "Sports Review",
                    "Premier League Live",
                    "Match Analysis",
                    "Live F1",
                    "Post-Race Debrief",
                    "Night Match",
                ),
            ),
            Plan(
                id = "c3",
                name = "Cartoon Network",
                number = 3,
                blockMinutes = 240,
                firstOffsetMinutes = 240,
                titles = listOf(
                    "Cartoon Time",
                    "Adventure Time",
                    "Ben 10",
                    "Powerpuff Girls",
                    "Toon Loose",
                    "Late Toons",
                ),
            ),
            Plan(
                id = "c4",
                name = "National Geographic",
                number = 4,
                blockMinutes = 240,
                firstOffsetMinutes = 120,
                titles = listOf(
                    "Discovery Documentary",
                    "Wild Planet",
                    "Engineering Disasters",
                    "Ultimate Factories",
                    "Mega Structures",
                    "Night Sky",
                ),
            ),
        )

        val Channels = Plans.map { LiveChannel(id = it.id, name = it.name, number = it.number) }

        /**
         * Local midnight of the day that contains [FixedNowUtcMs], matching
         * GuideViewModel.startOfDay(0). Computed per call, not in the companion initialiser, so it
         * runs after the test pins the timezone to UTC.
         */
        fun dayStart(): Long = Calendar.getInstance().apply {
            timeInMillis = FixedNowUtcMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Every plan covers the whole day, so a block is always on air at [FixedNowUtcMs]. */
        fun programmes(): List<EpgProgramme> {
            val dayStart = dayStart()
            return Plans.flatMap { plan ->
                plan.titles.mapIndexed { index, title ->
                    val start =
                        dayStart + (plan.firstOffsetMinutes + index * plan.blockMinutes) * 60_000L
                    EpgProgramme(
                        id = "${plan.id}-p$index",
                        channelId = plan.id,
                        title = title,
                        description = "$title on ${plan.name}",
                        startUtcMs = start,
                        endUtcMs = start + plan.blockMinutes * 60_000L,
                    )
                }
            }
        }

        fun state(): GuideUiState = GuideUiState(
            isLoading = false,
            channels = Channels,
            programmes = programmes(),
            favourites = setOf("c1"),
            selectedDay = 0,
            favouritesOnly = false,
            selectedCountry = null,
            countryPickerOpen = false,
        )
    }
}
