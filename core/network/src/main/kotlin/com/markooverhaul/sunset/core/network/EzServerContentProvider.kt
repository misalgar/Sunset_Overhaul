package com.markooverhaul.sunset.core.network

import com.markooverhaul.sunset.core.model.AccountSummary
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
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
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.StreamSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production adapter placeholder.
 *
 * STATUS: BLOCKED. No network implementation exists. There is no test server, no API
 * documentation, no approved production access and no confirmed JSON schema. Every call fails with
 * [ProviderError.ServiceNotConfigured] so release builds fail closed and can never bypass login.
 *
 * When access is approved (see docs/PROVIDER_INTEGRATION.md), implement this class behind the
 * [ContentProvider] interface, add contract tests against redacted fixtures, and feature-flag the
 * rollout. Do not add guessed DTOs or call production from this scaffold.
 */
@Singleton
class EzServerContentProvider @Inject constructor(
    private val config: EzServerConfig,
) : ContentProvider {

    override val status: ProviderStatus = ProviderStatus.NotConfigured(
        reason = "EZServer integration is blocked: no test access, API documentation or approval.",
    )

    override suspend fun login(serverUrl: String?, username: String, password: String): Session = blocked()
    override suspend fun restoreSession(token: String): Session = blocked()
    override suspend fun getAccountSummary(): AccountSummary = blocked()

    override suspend fun getLiveCategories(): List<Category> = blocked()
    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> = blocked()
    override suspend fun getGuide(days: Int): List<EpgProgramme> = blocked()

    override suspend fun getMovieCategories(): List<Category> = blocked()
    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> = blocked()
    override suspend fun getMovie(id: String): Movie? = blocked()

    override suspend fun getSeriesCategories(): List<Category> = blocked()
    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> = blocked()
    override suspend fun getSeriesDetail(id: String): SeriesDetail? = blocked()
    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> = blocked()

    override suspend fun search(query: String): SearchResults = blocked()
    override suspend fun resolveStream(request: StreamRequest): StreamSource = blocked()

    private fun blocked(): Nothing = throw ProviderError.ServiceNotConfigured
}
