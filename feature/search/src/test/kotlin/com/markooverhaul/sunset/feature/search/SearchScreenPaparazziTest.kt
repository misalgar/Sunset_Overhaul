package com.markooverhaul.sunset.feature.search

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.data.repository.RecentSearch
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SearchResults
import com.markooverhaul.sunset.core.model.SeriesSummary
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Baseline snapshot of the Search screen with an active query.
 *
 * The state is fully populated with dummy results and no artwork URLs, so the golden needs no
 * network and no provider.
 *
 * Run `recordPaparazziDebug` to refresh the golden and `verifyPaparazziDebug` to compare against
 * it.
 */
class SearchScreenPaparazziTest {

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
    fun snapshotSearchScreen() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                SearchScreen(
                    state = State,
                    onQueryChange = {},
                    onSubmit = {},
                    onClearQuery = {},
                    onClearRecent = {},
                    onSelectFilter = {},
                    onToggleFavourite = {},
                    onOpenDetails = { _, _ -> },
                    onPlayChannel = {},
                )
            }
        }
    }

    private companion object {
        val State = SearchUiState(
            query = "arsenal",
            results = SearchResults(
                live = listOf(
                    LiveChannel(id = "c1", name = "Sky Sports Main Event", number = 2),
                    LiveChannel(id = "c2", name = "Arsenal TV", number = 3),
                ),
                movies = listOf(
                    Movie(id = "m1", title = "Arsenal: The Long Season", year = 2023),
                    Movie(id = "m2", title = "Invictus", year = 2014, genres = listOf("Drama")),
                ),
                series = listOf(
                    SeriesSummary(id = "s1", title = "Arsenal Story", year = 2021),
                ),
            ),
            recent = listOf(
                RecentSearch(query = "football", lastUsedAtUtcMs = 1_725_948_900_000L),
                RecentSearch(query = "documentaries", lastUsedAtUtcMs = 1_725_940_000_000L),
            ),
            favouriteIds = setOf("c2"),
            filter = SearchFilter.All,
            isSearching = false,
        )
    }
}
