package com.markooverhaul.sunset.feature.movies

import androidx.compose.runtime.remember
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.CategoryType
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.Movie
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Baseline snapshot of the Movies grid.
 *
 * Paging loads on the main dispatcher, so the test replaces it with an unconfined dispatcher:
 * the page is presented synchronously and the grid renders with its posters in the first frame
 * instead of an empty or skeleton state.
 *
 * All poster URLs are null, so no network or provider is involved.
 *
 * Run `recordPaparazziDebug` to refresh the golden and `verifyPaparazziDebug` to compare against
 * it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoviesScreenPaparazziTest {

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
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun snapshotMoviesScreen() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                // collectAsLazyPagingItems is the public entry point; the internal
                // LazyPagingItems(Flow) constructor is not accessible from a test.
                val flow = remember { flowOf(PagingData.from(Movies)) }
                val movies = flow.collectAsLazyPagingItems(Dispatchers.Unconfined)
                MoviesScreen(
                    state = State,
                    movies = movies,
                    onSelectCategory = {},
                    onOpenDetails = { _, _ -> },
                )
            }
        }
    }

    private companion object {
        val State = MoviesUiState(
            categories = listOf(
                Category(id = "cat1", type = CategoryType.MOVIE, name = "Action", sortOrder = 1),
                Category(id = "cat2", type = CategoryType.MOVIE, name = "Comedy", sortOrder = 2),
                Category(id = "cat3", type = CategoryType.MOVIE, name = "Drama", sortOrder = 3),
                Category(id = "cat4", type = CategoryType.MOVIE, name = "Sci-Fi", sortOrder = 4),
                Category(id = "cat5", type = CategoryType.MOVIE, name = "Thriller", sortOrder = 5),
            ),
            selectedCategoryId = null,
            isLoadingCategories = false,
        )

        val Movies = listOf(
            Movie(id = "m1", title = "Dune: Part Two", year = 2024, genres = listOf("Sci-Fi")),
            Movie(id = "m2", title = "Oppenheimer", year = 2023, genres = listOf("Drama")),
            Movie(id = "m3", title = "The Batman", year = 2022, genres = listOf("Action")),
            Movie(id = "m4", title = "Blade Runner 2049", year = 2017, genres = listOf("Sci-Fi")),
            Movie(id = "m5", title = "Arrival", year = 2016, genres = listOf("Sci-Fi")),
            Movie(id = "m6", title = "Heat", year = 1995, genres = listOf("Crime")),
            Movie(id = "m7", title = "Sicario", year = 2015, genres = listOf("Thriller")),
            Movie(id = "m8", title = "Mad Max: Fury Road", year = 2015, genres = listOf("Action")),
            Movie(id = "m9", title = "Whiplash", year = 2014, genres = listOf("Drama")),
            Movie(id = "m10", title = "Ex Machina", year = 2014, genres = listOf("Sci-Fi")),
            Movie(id = "m11", title = "Prisoners", year = 2013, genres = listOf("Crime")),
            Movie(id = "m12", title = "Gravity", year = 2013, genres = listOf("Sci-Fi")),
        )
    }
}
