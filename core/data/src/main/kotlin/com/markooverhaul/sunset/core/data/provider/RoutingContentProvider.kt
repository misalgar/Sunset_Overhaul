package com.markooverhaul.sunset.core.data.provider

import com.markooverhaul.sunset.core.model.AccountSummary
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.Episode
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.PagedResult
import com.markooverhaul.sunset.core.model.ProviderStatus
import com.markooverhaul.sunset.core.model.SearchResults
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.Session
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.StreamSource
import com.markooverhaul.sunset.core.network.xtream.XtreamContentProvider
import com.markooverhaul.sunset.core.network.xtream.XtreamTokenCodec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug binding that serves either the offline demo catalogue or a live Xtream panel.
 *
 * The login form decides: a server address means the live panel, an empty address means the demo.
 * The mode is re-established from the stored session token on every process start, so the rest of
 * the app keeps talking to a single [ContentProvider].
 */
@Singleton
class RoutingContentProvider @Inject constructor(
    private val demo: FakeContentProvider,
    private val live: XtreamContentProvider,
) : ContentProvider {

    @Volatile
    private var liveMode: Boolean = false

    private val active: ContentProvider
        get() = if (liveMode) live else demo

    override val status: ProviderStatus
        get() = active.status

    override suspend fun login(serverUrl: String?, username: String, password: String): Session {
        liveMode = !serverUrl.isNullOrBlank()
        return active.login(serverUrl, username, password)
    }

    override suspend fun restoreSession(token: String): Session {
        liveMode = XtreamTokenCodec.isXtream(token)
        return active.restoreSession(token)
    }

    override suspend fun getAccountSummary(): AccountSummary = active.getAccountSummary()

    override suspend fun getLiveCategories(): List<Category> = active.getLiveCategories()

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> =
        active.getLiveChannels(categoryId)

    override fun cachedLiveChannels(): List<LiveChannel> = active.cachedLiveChannels()

    override suspend fun getGuide(days: Int): List<EpgProgramme> = active.getGuide(days)

    override suspend fun refreshLive() = active.refreshLive()

    override suspend fun getMovieCategories(): List<Category> = active.getMovieCategories()

    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> =
        active.getMovies(categoryId, page)

    override suspend fun getMovieSummary(id: String): Movie? = active.getMovieSummary(id)

    override suspend fun getMovie(id: String): Movie? = active.getMovie(id)

    override suspend fun getSeriesCategories(): List<Category> = active.getSeriesCategories()

    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> =
        active.getSeries(categoryId, page)

    override suspend fun getSeriesSummary(id: String): SeriesSummary? =
        active.getSeriesSummary(id)

    override suspend fun getSeriesDetail(id: String): SeriesDetail? = active.getSeriesDetail(id)

    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> =
        active.getEpisodes(seriesId, season)

    override suspend fun getMovieRecommendations(id: String): List<Movie> =
        active.getMovieRecommendations(id)

    override suspend fun getSeriesRecommendations(id: String): List<SeriesSummary> =
        active.getSeriesRecommendations(id)

    override suspend fun search(query: String): SearchResults = active.search(query)

    override suspend fun resolveStream(request: StreamRequest): StreamSource =
        active.resolveStream(request)
}
