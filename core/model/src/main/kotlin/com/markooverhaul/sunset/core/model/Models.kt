package com.markooverhaul.sunset.core.model

/** The four content kinds the UI understands. Provider-specific types never leak into the UI. */
enum class ContentType { LIVE, MOVIE, SERIES, EPISODE }

enum class CategoryType { LIVE, MOVIE, SERIES }

data class Category(
    val id: String,
    val type: CategoryType,
    val name: String,
    val isAdult: Boolean = false,
    val sortOrder: Int = 0,
)

data class LiveChannel(
    val id: String,
    val name: String,
    val number: Int,
    val logoUrl: String? = null,
    val categoryIds: List<String> = emptyList(),
    val streamRef: String = id,
    val epgChannelId: String? = id,
    val hasCatchUp: Boolean = false,
    val catchUpDays: Int = 0,
    /** ISO-ish country code inferred from the channel prefix or its country category. */
    val countryCode: String? = null,
    /** Hidden app-wide except in the explicit Adultos row on Live TV. */
    val isAdult: Boolean = false,
)

data class EpgProgramme(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startUtcMs: Long,
    val endUtcMs: Long,
    val category: String? = null,
    val rating: String? = null,
    val iconUrl: String? = null,
    val catchUpRef: String? = null,
    val isEstimatedEnd: Boolean = false,
)

data class Movie(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null,
    val plot: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val durationMinutes: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: String? = null,
    val streamRef: String = id,
    val tmdbId: String? = null,
    val certification: String? = null,
    val addedAtUtcMs: Long = 0L,
    val isAdult: Boolean = false,
)

data class SeriesSummary(
    val id: String,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val seasonCount: Int = 0,
    val genres: List<String> = emptyList(),
    val streamRef: String = id,
    val tmdbId: String? = null,
    val addedAtUtcMs: Long = 0L,
    val isAdult: Boolean = false,
)

data class SeriesDetail(
    val summary: SeriesSummary,
    val plot: String? = null,
    val cast: List<String> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val certification: String? = null,
)

data class Season(
    val number: Int,
    val name: String? = null,
    val episodes: List<Episode> = emptyList(),
) {
    val episodeCount: Int get() = episodes.size
}

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val plot: String? = null,
    val durationMinutes: Int? = null,
    val stillUrl: String? = null,
    val airDateUtcMs: Long? = null,
    val streamRef: String = id,
)

data class SearchResults(
    val live: List<LiveChannel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<SeriesSummary> = emptyList(),
) {
    val isEmpty: Boolean get() = live.isEmpty() && movies.isEmpty() && series.isEmpty()
    val totalCount: Int get() = live.size + movies.size + series.size
}

data class HomeFeed(
    val hero: Movie? = null,
    val liveNow: List<LiveChannel> = emptyList(),
    val recentlyAddedMovies: List<Movie> = emptyList(),
    val recentlyAddedSeries: List<SeriesSummary> = emptyList(),
    val recommended: List<Movie> = emptyList(),
)

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)

data class AccountSummary(
    val username: String,
    val expiresAtUtcMs: Long? = null,
    val status: String? = null,
    val deviceName: String = "Android device",
    val maxConnections: Int? = null,
)

data class Session(
    val username: String,
    val token: String,
    val expiresAtUtcMs: Long? = null,
    val isDemo: Boolean = false,
)

data class WatchProgress(
    val contentId: String,
    val contentType: ContentType,
    val episodeId: String? = null,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtUtcMs: Long,
    val completed: Boolean = false,
    /** Catalogue backdrop/poster resolved for Home rails; not persisted. */
    val artworkUrl: String? = null,
) {
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    companion object {
        const val MIN_RESUME_FRACTION = 0.05f
        const val COMPLETE_FRACTION = 0.95f
    }
}

data class Favourite(
    val contentId: String,
    val contentType: ContentType,
    val addedAtUtcMs: Long,
)

enum class StreamFormat { HLS, MPEG_TS, MP4, OTHER }

data class StreamRequest(
    val contentType: ContentType,
    val contentId: String,
    val episodeId: String? = null,
    val startPositionMs: Long = 0L,
    val live: Boolean = false,
    val catchUp: Boolean = false,
)

data class StreamSource(
    val url: String,
    val format: StreamFormat,
    val headers: Map<String, String> = emptyMap(),
    val engineHint: String? = null,
    val expiresAtUtcMs: Long? = null,
)

/** Provider availability. Release builds must render a "Service not configured" state when blocked. */
sealed interface ProviderStatus {
    data class Configured(val displayName: String) : ProviderStatus
    data class NotConfigured(val reason: String) : ProviderStatus
}

/**
 * The single boundary between the app and any content backend.
 *
 * Only endpoint names are known (spec Appendix B). No DTOs, JSON schemas or production URLs are
 * defined here; [com.markooverhaul.sunset.core.network.EzServerContentProvider] is deliberately
 * unimplemented until provider access is approved.
 */
interface ContentProvider {
    val status: ProviderStatus

    /**
     * [serverUrl] is the provider panel address entered by the viewer, or null for the bundled
     * demo catalogue. Providers that are configured at build time may ignore it.
     */
    suspend fun login(serverUrl: String?, username: String, password: String): Session
    suspend fun restoreSession(token: String): Session
    suspend fun getAccountSummary(): AccountSummary

    suspend fun getLiveCategories(): List<Category>
    suspend fun getLiveChannels(categoryId: String? = null): List<LiveChannel>
    suspend fun getGuide(days: Int = 1): List<EpgProgramme>

    /** Drops any short-lived live caches so the next read reflects a schedule/category change. */
    suspend fun refreshLive() {}

    suspend fun getMovieCategories(): List<Category>
    suspend fun getMovies(categoryId: String? = null, page: Int = 1): PagedResult<Movie>
    suspend fun getMovieSummary(id: String): Movie? = null
    suspend fun getMovie(id: String): Movie?

    suspend fun getSeriesCategories(): List<Category>
    suspend fun getSeries(categoryId: String? = null, page: Int = 1): PagedResult<SeriesSummary>
    suspend fun getSeriesSummary(id: String): SeriesSummary? = null
    suspend fun getSeriesDetail(id: String): SeriesDetail?
    suspend fun getEpisodes(seriesId: String, season: Int): List<Episode>

    /** Playable local titles recommended for a movie/series; empty when enrichment is unavailable. */
    suspend fun getMovieRecommendations(id: String): List<Movie> = emptyList()
    suspend fun getSeriesRecommendations(id: String): List<SeriesSummary> = emptyList()

    suspend fun search(query: String): SearchResults

    suspend fun resolveStream(request: StreamRequest): StreamSource
}

/** Typed provider failures. UI maps these to human copy; raw provider text is never shown. */
sealed class ProviderError(message: String) : Exception(message) {
    data object InvalidCredentials : ProviderError("Invalid credentials")
    data object SessionExpired : ProviderError("Session expired")
    data object AccountExpired : ProviderError("Account expired")
    data object DeviceNotActive : ProviderError("Device not active")
    data object ConnectionLimit : ProviderError("Connection limit reached")
    data object Maintenance : ProviderError("Provider maintenance")
    data object Network : ProviderError("Network unavailable")
    data object Server : ProviderError("Provider server error")
    data object RateLimited : ProviderError("Rate limited")
    data object NotFound : ProviderError("Content not found")
    data object PlaybackUnsupported : ProviderError("Playback unsupported")
    data object PlaybackAuth : ProviderError("Playback authorization failed")
    data object ParentalLocked : ProviderError("Parental lock")
    data object ServiceNotConfigured : ProviderError("Provider not configured")
    data class Unknown(val detail: String? = null) : ProviderError("Unknown provider error")
}
