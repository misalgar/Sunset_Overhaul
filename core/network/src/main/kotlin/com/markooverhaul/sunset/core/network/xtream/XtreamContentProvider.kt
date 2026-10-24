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
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.Session
import com.markooverhaul.sunset.core.model.StreamFormat
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.StreamSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString

/**
 * Live Xtream Codes adapter.
 *
 * Slice 1 covers authentication, account information, live categories and channels, search across
 * channels, and live stream URLs. Movies, series and EPG return empty results until the next slice;
 * they never throw, so the UI degrades to empty rows instead of error screens.
 */
@Singleton
class XtreamContentProvider @Inject constructor(
    private val api: XtreamApi,
    private val cache: XtreamCache,
) : ContentProvider {

    private val mutex = Mutex()

    @Volatile
    private var credentials: XtreamCredentials? = null

    @Volatile
    private var account: XtreamAuthResponse? = null

    @Volatile
    private var categories: List<Category>? = null

    @Volatile
    private var channels: List<LiveChannel>? = null

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

    override suspend fun getMovieCategories(): List<Category> = emptyList()

    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> =
        PagedResult(items = emptyList(), page = page, totalPages = 1, hasMore = false)

    override suspend fun getMovie(id: String): Movie? = null

    override suspend fun getSeriesCategories(): List<Category> = emptyList()

    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> =
        PagedResult(items = emptyList(), page = page, totalPages = 1, hasMore = false)

    override suspend fun getSeriesDetail(id: String): SeriesDetail? = null

    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> = emptyList()

    override suspend fun search(query: String): SearchResults {
        val term = query.trim()
        if (term.length < MIN_QUERY) return SearchResults()
        val matches = mutex.withLock {
            val all = channels ?: loadChannels()
            all.filter { it.name.contains(term, ignoreCase = true) }
        }
        return SearchResults(live = matches.take(MAX_RESULTS))
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

            ContentType.MOVIE -> StreamSource(
                url = "$base/movie/$user/$pass/${encodeSegment(request.contentId)}.mp4",
                format = StreamFormat.MP4,
            )

            ContentType.SERIES, ContentType.EPISODE -> StreamSource(
                url = "$base/series/$user/$pass/${encodeSegment(request.episodeId ?: request.contentId)}.mp4",
                format = StreamFormat.MP4,
            )
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
        categories = null
        channels = null
        cache.clear()
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
    }
}

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
