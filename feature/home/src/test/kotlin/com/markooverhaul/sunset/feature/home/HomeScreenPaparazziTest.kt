package com.markooverhaul.sunset.feature.home

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.WatchProgress
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Baseline snapshot of the Home screen.
 *
 * The state is fully populated with dummy catalogue data so the hero and every rail render, and
 * all artwork URLs are null: [com.markooverhaul.sunset.core.ui.components.SunsetArtwork] then
 * falls back to its deterministic gradient + monogram, so the golden needs no network and no
 * provider.
 *
 * Run `recordPaparazziDebug` to refresh the golden and `verifyPaparazziDebug` to compare against
 * it.
 */
class HomeScreenPaparazziTest {

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
    fun snapshotHomeScreen() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                HomeScreen(
                    state = State,
                    onPlayMovie = {},
                    onResume = {},
                    onOpenDetails = { _, _ -> },
                    onOpenChannel = {},
                    onOpenLive = {},
                    onOpenMovies = {},
                    onOpenSeries = {},
                    onOpenMyList = {},
                    onRetry = {},
                )
            }
        }
    }

    private companion object {
        val Hero = Movie(
            id = "m1",
            title = "Dune: Part Two",
            year = 2024,
            plot = "Paul Atreides unites with Chani and the Fremen while seeking revenge.",
            rating = 8.6,
            durationMinutes = 166,
            genres = listOf("Sci-Fi", "Adventure"),
            language = "LAT",
            quality = "4K",
        )

        val LiveChannels = listOf(
            LiveChannel(id = "c1", name = "BBC One", number = 1),
            LiveChannel(id = "c2", name = "Sky Sports Main Event", number = 2),
            LiveChannel(id = "c3", name = "Cartoon Network", number = 3),
            LiveChannel(id = "c4", name = "National Geographic", number = 4),
        )

        fun Programme(id: String, channelId: String, title: String) = EpgProgramme(
            id = id,
            channelId = channelId,
            title = title,
            startUtcMs = 1_725_948_900_000L,
            endUtcMs = 1_725_949_800_000L,
        )

        val State = HomeUiState(
            isLoading = false,
            loadingLive = false,
            loadingCatalogue = false,
            hero = Hero,
            liveNow = LiveChannels,
            recentMovies = listOf(
                Movie(id = "m2", title = "Oppenheimer", year = 2023, genres = listOf("Drama")),
                Movie(id = "m3", title = "The Batman", year = 2022, genres = listOf("Action")),
                Movie(id = "m4", title = "Blade Runner 2049", year = 2017, genres = listOf("Sci-Fi")),
                Movie(id = "m5", title = "Arrival", year = 2016, genres = listOf("Sci-Fi")),
                Movie(id = "m6", title = "Heat", year = 1995, genres = listOf("Crime")),
                Movie(id = "m7", title = "Sicario", year = 2015, genres = listOf("Thriller")),
            ),
            recentSeries = listOf(
                SeriesSummary(id = "s1", title = "Severance", year = 2022, genres = listOf("Drama")),
                SeriesSummary(id = "s2", title = "Chernobyl", year = 2019, genres = listOf("Drama")),
                SeriesSummary(id = "s3", title = "Dark", year = 2017, genres = listOf("Mystery")),
            ),
            recommended = listOf(
                Movie(id = "m8", title = "Mad Max: Fury Road", year = 2015, genres = listOf("Action")),
                Movie(id = "m9", title = "Whiplash", year = 2014, genres = listOf("Drama")),
                Movie(id = "m10", title = "Ex Machina", year = 2014, genres = listOf("Sci-Fi")),
            ),
            continueWatching = listOf(
                WatchProgress(
                    contentId = "m1",
                    contentType = ContentType.MOVIE,
                    title = "Dune: Part Two",
                    positionMs = 42 * 60_000L,
                    durationMs = 166 * 60_000L,
                    updatedAtUtcMs = 1_725_948_900_000L,
                ),
            ),
            favouriteIds = setOf("m2"),
            nowNext = mapOf(
                "c1" to ChannelNowNext(
                    now = Programme("p1", "c1", "BBC News at Six"),
                    next = Programme("p2", "c1", "The One Show"),
                ),
                "c2" to ChannelNowNext(
                    now = Programme("p3", "c2", "Premier League: Arsenal v Chelsea"),
                ),
            ),
        )
    }
}
