package com.markooverhaul.sunset.core.data.provider

import com.markooverhaul.sunset.core.data.fake.FakeCatalog
import com.markooverhaul.sunset.core.model.AccountSummary
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.Episode
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.PagedResult
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.ProviderStatus
import com.markooverhaul.sunset.core.model.SearchResults
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.Session
import com.markooverhaul.sunset.core.model.StreamFormat
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.StreamSource
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline demo provider backed entirely by [FakeCatalog].
 *
 * This provider never opens a socket. It exists so every screen and journey can be demonstrated
 * without provider credentials or production access. It is bound only in debug builds; release
 * builds bind [com.markooverhaul.sunset.core.network.EzServerContentProvider], which fails closed.
 *
 * Stream references use the `demo://` scheme. [com.markooverhaul.sunset.core.player] maps them to
 * the bundled sample MP4, so playback also works in airplane mode.
 */
@Singleton
class FakeContentProvider @Inject constructor() : ContentProvider {

    override val status: ProviderStatus = ProviderStatus.Configured("Sunset demo catalogue")

    override suspend fun login(serverUrl: String?, username: String, password: String): Session {
        tick()
        if (username.isBlank() || password.isBlank()) throw ProviderError.InvalidCredentials
        return Session(
            username = username.trim(),
            token = DEMO_TOKEN_PREFIX + System.currentTimeMillis(),
            isDemo = true,
        )
    }

    override suspend fun restoreSession(token: String): Session {
        tick()
        if (!token.startsWith(DEMO_TOKEN_PREFIX)) throw ProviderError.SessionExpired
        return Session(
            username = DEMO_USERNAME,
            token = token,
            isDemo = true,
        )
    }

    override suspend fun getAccountSummary(): AccountSummary {
        tick()
        return AccountSummary(
            username = DEMO_USERNAME,
            status = "Demo session",
            deviceName = "Android device (demo)",
            maxConnections = 1,
        )
    }

    override suspend fun getLiveCategories(): List<Category> {
        tick()
        return FakeCatalog.liveCategories
    }

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> {
        tick()
        val channels = FakeCatalog.channels
        return if (categoryId.isNullOrBlank()) channels
        else channels.filter { categoryId in it.categoryIds }
    }

    override suspend fun getGuide(days: Int): List<EpgProgramme> {
        tick()
        return FakeCatalog.guide(days)
    }

    override suspend fun getMovieCategories(): List<Category> {
        tick()
        return FakeCatalog.movieCategories
    }

    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> =
        page(FakeCatalog.movies, categoryId, page) { movie, id ->
            movie.genres.any { genre -> categoryName(id) == genre.lowercase(Locale.ROOT) }
        }

    override suspend fun getMovie(id: String): Movie? {
        tick()
        return FakeCatalog.findMovie(id)
    }

    override suspend fun getSeriesCategories(): List<Category> {
        tick()
        return FakeCatalog.seriesCategories
    }

    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> =
        page(FakeCatalog.allSeriesSummaries(), categoryId, page) { series, id ->
            series.genres.any { genre -> genre.lowercase() == categoryName(id) }
        }

    override suspend fun getSeriesDetail(id: String): SeriesDetail? {
        tick()
        return FakeCatalog.findSeries(id)
    }

    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> {
        tick()
        return FakeCatalog.findSeries(seriesId)
            ?.seasons
            ?.firstOrNull { it.number == season }
            ?.episodes
            .orEmpty()
    }

    override suspend fun search(query: String): SearchResults {
        tick()
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.length < 2) return SearchResults()

        val movies = FakeCatalog.movies.filter { movie ->
            movie.title.lowercase(Locale.ROOT).contains(needle) ||
                movie.genres.any { it.lowercase(Locale.ROOT).contains(needle) } ||
                movie.plot.orEmpty().lowercase(Locale.ROOT).contains(needle)
        }.take(12)

        val series = FakeCatalog.allSeriesSummaries().filter { series ->
            series.title.lowercase(Locale.ROOT).contains(needle) ||
                series.genres.any { it.lowercase(Locale.ROOT).contains(needle) }
        }.take(12)

        val live = FakeCatalog.channels.filter { channel ->
            channel.name.lowercase(Locale.ROOT).contains(needle) ||
                channel.number.toString().contains(needle)
        }.take(12)

        return SearchResults(live = live, movies = movies, series = series)
    }

    override suspend fun resolveStream(request: StreamRequest): StreamSource {
        tick()
        val known = when (request.contentType) {
            ContentType.MOVIE -> FakeCatalog.findMovie(request.contentId) != null
            ContentType.SERIES, ContentType.EPISODE -> FakeCatalog.findSeries(request.contentId) != null
            ContentType.LIVE -> FakeCatalog.channels.any { it.id == request.contentId }
        }
        if (!known) throw ProviderError.NotFound
        // The player recognises this scheme and plays a bundled local sample.
        return StreamSource(url = DEMO_STREAM_URL, format = StreamFormat.MP4)
    }

    /**
     * Shared in-memory paging. Provider pagination is simulated with a fixed page size so the
     * Paging 3 integration exercises real append/prepend behaviour.
     */
    private suspend fun <T> page(
        all: List<T>,
        categoryId: String?,
        requestedPage: Int,
        matches: (T, String) -> Boolean,
    ): PagedResult<T> {
        tick()
        val filtered = if (categoryId.isNullOrBlank()) all else all.filter { matches(it, categoryId) }
        val page = requestedPage.coerceAtLeast(1)
        val from = (page - 1) * PAGE_SIZE
        val items = filtered.drop(from).take(PAGE_SIZE)
        val totalPages = ((filtered.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        return PagedResult(
            items = items,
            page = page,
            totalPages = totalPages,
            hasMore = page < totalPages,
        )
    }

    private fun categoryName(categoryId: String): String =
        categoryId.substringAfterLast('-').lowercase(Locale.ROOT)

    private suspend fun tick() = delay(SIMULATED_LATENCY_MS)

    companion object {
        const val DEMO_TOKEN_PREFIX = "demo-session-"
        const val DEMO_USERNAME = "demo"
        const val DEMO_STREAM_URL = "demo://local-sample"
        const val PAGE_SIZE = 8
        private const val SIMULATED_LATENCY_MS = 60L
    }
}
