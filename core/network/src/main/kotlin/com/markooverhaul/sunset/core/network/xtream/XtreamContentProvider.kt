package com.markooverhaul.sunset.core.network.xtream

import android.util.Base64
import com.markooverhaul.sunset.core.model.AccountSummary
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.CategoryType
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
import com.markooverhaul.sunset.core.model.Season
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.Session
import com.markooverhaul.sunset.core.model.StreamFormat
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.StreamSource
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

/**
 * Xtream Codes adapter.
 *
 * Covers authentication, account information, live categories/channels and playback, plus the VOD
 * and series catalogues with their detail responses. Catalogue lists are large (the reference panel
 * returns 52k movies in a 16.6 MB payload), so they are fetched once, cached on disk for six hours
 * and paged in memory. EPG is still the next slice.
 */
@Singleton
class XtreamContentProvider @Inject constructor(
    private val api: XtreamApi,
    private val cache: XtreamCache,
) : ContentProvider {

    private val mutex = Mutex()

    /** Separate lock: a 16 MB VOD download must not block live channel requests. */
    private val catalogueMutex = Mutex()

    @Volatile
    private var credentials: XtreamCredentials? = null

    @Volatile
    private var account: XtreamAuthResponse? = null

    @Volatile
    private var categories: List<Category>? = null

    @Volatile
    private var channels: List<LiveChannel>? = null

    @Volatile
    private var movieCategories: List<Category>? = null

    @Volatile
    private var seriesCategories: List<Category>? = null

    @Volatile
    private var movies: List<MovieEntry>? = null

    @Volatile
    private var series: List<SeriesEntry>? = null

    private val movieDetails = mutableMapOf<String, Movie>()

    private val seriesDetails = mutableMapOf<String, SeriesDetail>()

    /** stream/episode id -> container extension, used to build playback URLs. */
    private val streamExtensions = ConcurrentHashMap<String, String>()

    override val status: ProviderStatus
        get() = credentials?.let { ProviderStatus.Configured("Sunset live service") }
            ?: ProviderStatus.NotConfigured("No provider configured")

    override suspend fun login(serverUrl: String?, username: String, password: String): Session {
        val baseUrl = XtreamApi.normalizeBaseUrl(serverUrl) ?: throw ProviderError.InvalidCredentials
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty() || password.isEmpty()) throw ProviderError.InvalidCredentials
        val response = api.auth(baseUrl, trimmedUser, password)
        val credentials = XtreamCredentials(baseUrl, trimmedUser, password)
        apply(credentials, response)
        return sessionOf(credentials, response)
    }

    override suspend fun restoreSession(token: String): Session {
        val credentials = XtreamTokenCodec.decode(token) ?: throw ProviderError.SessionExpired
        val response = try {
            api.auth(credentials.baseUrl, credentials.username, credentials.password)
        } catch (error: ProviderError) {
            throw when (error) {
                is ProviderError.Network, is ProviderError.RateLimited -> error
                else -> ProviderError.SessionExpired
            }
        }
        try {
            apply(credentials, response)
        } catch (_: ProviderError.InvalidCredentials) {
            throw ProviderError.SessionExpired
        }
        return sessionOf(credentials, response)
    }

    override suspend fun getAccountSummary(): AccountSummary {
        val info = account?.userInfo ?: throw ProviderError.ServiceNotConfigured
        return AccountSummary(
            username = info.username.ifBlank { credentials?.username.orEmpty() },
            expiresAtUtcMs = info.expDate.toLongOrNull()?.times(1000L),
            status = info.status,
            maxConnections = info.maxConnections.toIntOrNull(),
        )
    }

    override suspend fun getLiveCategories(): List<Category> = mutex.withLock {
        categories ?: run {
            val body = cached("live_categories", "get_live_categories")
            val mapped = XtreamApi.json
                .decodeFromString<List<XtreamCategoryDto>>(body)
                .mapIndexed { index, dto ->
                    Category(
                        id = dto.id,
                        type = CategoryType.LIVE,
                        name = dto.name,
                        sortOrder = index,
                    )
                }
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            categories = mapped
            mapped
        }
    }

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> = mutex.withLock {
        val all = channels ?: loadChannels()
        if (categoryId.isNullOrBlank()) all else all.filter { categoryId in it.categoryIds }
    }

    /** EPG arrives with the guide slice; an empty list keeps the UI in its "no guide data" state. */
    override suspend fun getGuide(days: Int): List<EpgProgramme> = emptyList()

    override suspend fun getMovieCategories(): List<Category> = catalogueMutex.withLock {
        movieCategories ?: loadCategories("vod_categories", "get_vod_categories", CategoryType.MOVIE)
            .also { movieCategories = it }
    }

    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> {
        val all = catalogueMutex.withLock {
            movies ?: loadMovies().also { movies = it }
        }
        val filtered = if (categoryId.isNullOrBlank()) all else all.filter { it.categoryId == categoryId }
        return pageOf(filtered, page) { it.movie }
    }

    override suspend fun getMovie(id: String): Movie? {
        movieDetails[id]?.let { return it }
        val entry = catalogueMutex.withLock {
            movies?.firstOrNull { it.movie.id == id }
                ?: loadMovies().also { movies = it }.firstOrNull { it.movie.id == id }
        } ?: return null
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        val body = runCatching {
            api.call(
                credentials.baseUrl,
                credentials.username,
                credentials.password,
                action = "get_vod_info",
                params = mapOf("vod_id" to id),
            )
        }.getOrNull()
        val info = body?.let {
            runCatching { XtreamApi.json.decodeFromString<XtreamVodInfoResponse>(it) }.getOrNull()
        }
        val enriched = entry.movie.copy(
            plot = info?.info?.plot?.takeIf { it.isNotBlank() } ?: entry.movie.plot,
            cast = splitPeople(info?.info?.cast).ifEmpty { entry.movie.cast },
            director = info?.info?.director?.takeIf { it.isNotBlank() } ?: entry.movie.director,
            genres = splitGenres(info?.info?.genre).ifEmpty { entry.movie.genres },
            durationMinutes = parseDurationMinutes(info?.info?.duration) ?: entry.movie.durationMinutes,
            backdropUrl = info?.info?.backdrop?.firstOrNull { it.isNotBlank() }
                ?: entry.movie.backdropUrl,
            certification = entry.movie.certification,
        )
        movieDetails[id] = enriched
        return enriched
    }

    override suspend fun getSeriesCategories(): List<Category> = catalogueMutex.withLock {
        seriesCategories
            ?: loadCategories("series_categories", "get_series_categories", CategoryType.SERIES)
                .also { seriesCategories = it }
    }

    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> {
        val all = catalogueMutex.withLock {
            series ?: loadSeries().also { series = it }
        }
        val filtered = if (categoryId.isNullOrBlank()) all else all.filter { it.categoryId == categoryId }
        return pageOf(filtered, page) { it.summary }
    }

    override suspend fun getSeriesDetail(id: String): SeriesDetail? {
        seriesDetails[id]?.let { return it }
        val entry = catalogueMutex.withLock {
            series?.firstOrNull { it.summary.id == id }
                ?: loadSeries().also { series = it }.firstOrNull { it.summary.id == id }
        } ?: return null
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        val response = runCatching {
            api.call(
                credentials.baseUrl,
                credentials.username,
                credentials.password,
                action = "get_series_info",
                params = mapOf("series_id" to id),
            )
        }.getOrNull()?.let {
            runCatching { XtreamApi.json.decodeFromString<XtreamSeriesInfoResponse>(it) }.getOrNull()
        } ?: return entry.summary.let { SeriesDetail(summary = it) }

        val info = response.info
        val summary = entry.summary.copy(
            title = info?.name?.takeIf { it.isNotBlank() } ?: entry.summary.title,
            posterUrl = info?.cover?.takeIf { it.isNotBlank() } ?: entry.summary.posterUrl,
            backdropUrl = info?.backdrop?.firstOrNull { it.isNotBlank() } ?: entry.summary.backdropUrl,
            rating = info?.rating?.toDoubleOrNull() ?: entry.summary.rating,
            genres = splitGenres(info?.genre).ifEmpty { entry.summary.genres },
        )
        val seasons = response.episodes.entries
            .mapNotNull { (seasonKey, episodes) ->
                seasonKey.toIntOrNull()?.let { it to episodes }
            }
            .sortedBy { it.first }
            .map { (seasonNumber, episodes) ->
                Season(
                    number = seasonNumber,
                    episodes = episodes
                        .sortedBy { it.episodeNum }
                        .map { episode ->
                            streamExtensions[episode.id] =
                                episode.containerExtension.ifBlank { DEFAULT_EXTENSION }
                            Episode(
                                id = episode.id,
                                seriesId = id,
                                seasonNumber = seasonNumber,
                                episodeNumber = episode.episodeNum,
                                title = episode.title.ifBlank { "Episode ${episode.episodeNum}" },
                                plot = episode.info?.plot?.takeIf { it.isNotBlank() },
                                durationMinutes = parseDurationMinutes(episode.info?.duration),
                                stillUrl = episode.info?.image?.takeIf { it.isNotBlank() },
                                airDateUtcMs = parseDateUtcMs(episode.info?.releaseDate),
                                streamRef = episode.id,
                            )
                        },
                )
            }
        val detail = SeriesDetail(
            summary = summary.copy(seasonCount = seasons.size.coerceAtLeast(summary.seasonCount)),
            plot = info?.plot?.takeIf { it.isNotBlank() },
            cast = splitPeople(info?.cast),
            seasons = seasons,
        )
        seriesDetails[id] = detail
        return detail
    }

    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> =
        getSeriesDetail(seriesId)?.seasons?.firstOrNull { it.number == season }?.episodes.orEmpty()

    override suspend fun search(query: String): SearchResults {
        val term = query.trim()
        if (term.length < MIN_QUERY) return SearchResults()
        val liveMatches = mutex.withLock {
            val all = channels ?: loadChannels()
            all.filter { it.name.contains(term, ignoreCase = true) }
        }
        val movieMatches = catalogueMutex.withLock {
            (movies ?: loadMovies().also { movies = it })
                .asSequence()
                .map { it.movie }
                .filter { it.title.contains(term, ignoreCase = true) }
                .take(MAX_RESULTS)
                .toList()
        }
        val seriesMatches = catalogueMutex.withLock {
            (series ?: loadSeries().also { series = it })
                .asSequence()
                .map { it.summary }
                .filter { it.title.contains(term, ignoreCase = true) }
                .take(MAX_RESULTS)
                .toList()
        }
        return SearchResults(
            live = liveMatches.take(MAX_RESULTS),
            movies = movieMatches,
            series = seriesMatches,
        )
    }

    override suspend fun resolveStream(request: StreamRequest): StreamSource {
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        val base = credentials.baseUrl
        val user = encodeSegment(credentials.username)
        val pass = encodeSegment(credentials.password)
        return when (request.contentType) {
            ContentType.LIVE -> StreamSource(
                url = "$base/live/$user/$pass/${encodeSegment(request.contentId)}.ts",
                format = StreamFormat.MPEG_TS,
            )

            ContentType.MOVIE -> {
                val ext = streamExtensions[request.contentId] ?: DEFAULT_EXTENSION
                StreamSource(
                    url = "$base/movie/$user/$pass/${encodeSegment(request.contentId)}.$ext",
                    format = formatOf(ext),
                )
            }

            ContentType.SERIES, ContentType.EPISODE -> {
                val id = request.episodeId ?: request.contentId
                val ext = streamExtensions[id] ?: DEFAULT_EXTENSION
                StreamSource(
                    url = "$base/series/$user/$pass/${encodeSegment(id)}.$ext",
                    format = formatOf(ext),
                )
            }
        }
    }

    private suspend fun loadChannels(): List<LiveChannel> {
        val body = cached("live_streams", "get_live_streams")
        val mapped = XtreamApi.json
            .decodeFromString<List<XtreamLiveStreamDto>>(body)
            .mapIndexedNotNull { index, dto ->
                if (dto.streamId.isBlank()) return@mapIndexedNotNull null
                LiveChannel(
                    id = dto.streamId,
                    name = dto.name.ifBlank { "Channel ${dto.streamId}" },
                    number = dto.num.takeIf { it > 0 } ?: (index + 1),
                    logoUrl = dto.streamIcon.takeIf { it.isNotBlank() },
                    categoryIds = listOfNotNull(dto.categoryId.takeIf { it.isNotBlank() }),
                    streamRef = dto.streamId,
                    epgChannelId = dto.epgChannelId.takeIf { it.isNotBlank() },
                    hasCatchUp = dto.tvArchive == 1,
                    catchUpDays = dto.tvArchiveDuration,
                )
            }
        channels = mapped
        return mapped
    }

    private suspend fun loadCategories(
        cacheKey: String,
        action: String,
        type: CategoryType,
    ): List<Category> {
        val body = cached(cacheKey, action)
        return withContext(Dispatchers.IO) {
            XtreamApi.json
                .decodeFromString<List<XtreamCategoryDto>>(body)
                .mapIndexed { index, dto ->
                    Category(id = dto.id, type = type, name = dto.name, sortOrder = index)
                }
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
        }
    }

    private suspend fun loadMovies(): List<MovieEntry> {
        val body = cached("vod_streams", "get_vod_streams")
        return withContext(Dispatchers.IO) {
            XtreamApi.json
                .decodeFromString<List<XtreamVodStreamDto>>(body)
                .mapNotNull { dto ->
                    if (dto.streamId.isBlank()) return@mapNotNull null
                    val (title, year) = splitTitleYear(dto.name)
                    val extension = dto.containerExtension.ifBlank { DEFAULT_EXTENSION }
                    streamExtensions[dto.streamId] = extension
                    MovieEntry(
                        movie = Movie(
                            id = dto.streamId,
                            title = title,
                            year = year,
                            posterUrl = dto.streamIcon.takeIf { it.isNotBlank() },
                            rating = dto.rating.toDoubleOrNull(),
                            addedAtUtcMs = dto.added.toLongOrNull()?.times(1000L) ?: 0L,
                            streamRef = dto.streamId,
                        ),
                        categoryId = dto.categoryId,
                        extension = extension,
                    )
                }
        }
    }

    private suspend fun loadSeries(): List<SeriesEntry> {
        val body = cached("series", "get_series")
        return withContext(Dispatchers.IO) {
            XtreamApi.json
                .decodeFromString<List<XtreamSeriesDto>>(body)
                .mapNotNull { dto ->
                    if (dto.seriesId.isBlank()) return@mapNotNull null
                    SeriesEntry(
                        summary = SeriesSummary(
                            id = dto.seriesId,
                            title = dto.name,
                            year = dto.releaseDate.take(4).toIntOrNull(),
                            posterUrl = dto.cover.takeIf { it.isNotBlank() },
                            rating = dto.rating.toDoubleOrNull(),
                            genres = splitGenres(dto.genre),
                            streamRef = dto.seriesId,
                        ),
                        categoryId = dto.categoryId,
                    )
                }
        }
    }

    private fun <T, R> pageOf(
        entries: List<T>,
        page: Int,
        mapper: (T) -> R,
    ): PagedResult<R> {
        val totalPages = maxOf(1, (entries.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val safePage = page.coerceIn(1, totalPages)
        val start = (safePage - 1) * PAGE_SIZE
        if (start >= entries.size) {
            return PagedResult(emptyList(), safePage, totalPages, hasMore = false)
        }
        val end = minOf(start + PAGE_SIZE, entries.size)
        return PagedResult(
            items = entries.subList(start, end).map(mapper),
            page = safePage,
            totalPages = totalPages,
            hasMore = safePage < totalPages,
        )
    }

    /** Titles arrive as "Name (1999)" or "Name (1999) SUB"; split the year for the caption. */
    private fun splitTitleYear(raw: String): Pair<String, Int?> {
        val match = TITLE_YEAR.find(raw) ?: return raw.trim() to null
        val year = match.groupValues[1].toIntOrNull()
        val title = raw.removeRange(match.range)
            .replace(WHITESPACE, " ")
            .trim()
            .trimEnd('-', '–')
            .trim()
        return (title.ifBlank { raw.trim() }) to year
    }

    private fun splitPeople(raw: String?): List<String> =
        raw.orEmpty()
            .split(',', ';', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(12)

    private fun splitGenres(raw: String?): List<String> =
        raw.orEmpty()
            .split(',', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Accepts "01:32:00", "92:00" and raw seconds. */
    private fun parseDurationMinutes(raw: String?): Int? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return if (value.contains(':')) {
            val parts = value.split(':').mapNotNull { it.toIntOrNull() }
            when (parts.size) {
                3 -> parts[0] * 60 + parts[1]
                2 -> parts[0]
                else -> null
            }
        } else {
            value.toLongOrNull()?.let { (it / 60L).toInt() }
        }?.takeIf { it > 0 }
    }

    private fun parseDateUtcMs(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.length < 10) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(value.take(10))
                ?.time
        }.getOrNull()
    }

    private fun formatOf(extension: String): StreamFormat =
        if (extension.equals("m3u8", ignoreCase = true)) StreamFormat.HLS else StreamFormat.MP4

    private suspend fun cached(key: String, action: String): String {
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        cache.read(key)?.let { return it }
        val body = api.call(credentials.baseUrl, credentials.username, credentials.password, action)
        cache.write(key, body)
        return body
    }

    private fun apply(credentials: XtreamCredentials, response: XtreamAuthResponse) {
        val info = response.userInfo ?: throw ProviderError.Server
        if (info.auth != 1) {
            throw when (info.status.lowercase()) {
                "expired" -> ProviderError.AccountExpired
                "disabled", "banned" -> ProviderError.DeviceNotActive
                else -> ProviderError.InvalidCredentials
            }
        }
        this.credentials = credentials
        this.account = response

        // Keep the catalogue cache across launches: only drop it when a different account signs in.
        // Wiping on every restore forced a full 16 MB VOD download each cold start.
        val accountKey = "${credentials.baseUrl}|${credentials.username}"
        val cachedAccount = cache.read(ACCOUNT_KEY, maxAgeMs = Long.MAX_VALUE)
        if (cachedAccount != accountKey) {
            categories = null
            channels = null
            movieCategories = null
            seriesCategories = null
            movies = null
            series = null
            movieDetails.clear()
            seriesDetails.clear()
            streamExtensions.clear()
            cache.clear()
            cache.write(ACCOUNT_KEY, accountKey)
        }
    }

    private fun sessionOf(credentials: XtreamCredentials, response: XtreamAuthResponse) = Session(
        username = credentials.username,
        token = XtreamTokenCodec.encode(credentials),
        expiresAtUtcMs = response.userInfo?.expDate?.toLongOrNull()?.times(1000L),
        isDemo = false,
    )

    private fun encodeSegment(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val MIN_QUERY = 2
        const val MAX_RESULTS = 40
        const val PAGE_SIZE = 60
        const val DEFAULT_EXTENSION = "mp4"
        const val ACCOUNT_KEY = "account"
        val TITLE_YEAR = Regex("""\((\d{4})\)""")
        val WHITESPACE = Regex("""\s+""")
    }
}

/** A movie plus the fields needed for category filtering and playback. */
private data class MovieEntry(
    val movie: Movie,
    val categoryId: String,
    val extension: String,
)

private data class SeriesEntry(
    val summary: SeriesSummary,
    val categoryId: String,
)

/**
 * Xtream has no bearer token: every call repeats the credentials, so the session token carries
 * them. [com.markooverhaul.sunset.core.data.session.EncryptedSessionStore] encrypts it at rest.
 */
object XtreamTokenCodec {
    private const val PREFIX = "xt:"

    fun isXtream(token: String): Boolean = token.startsWith(PREFIX)

    fun encode(credentials: XtreamCredentials): String {
        val json = XtreamApi.json.encodeToString(XtreamCredentials.serializer(), credentials)
        return PREFIX + Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decode(token: String): XtreamCredentials? {
        if (!isXtream(token)) return null
        return runCatching {
            val bytes = Base64.decode(token.removePrefix(PREFIX), Base64.NO_WRAP)
            XtreamApi.json.decodeFromString(XtreamCredentials.serializer(), String(bytes, Charsets.UTF_8))
        }.getOrNull()
    }
}
