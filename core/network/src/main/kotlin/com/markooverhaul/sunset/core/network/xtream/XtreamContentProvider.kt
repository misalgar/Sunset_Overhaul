package com.markooverhaul.sunset.core.network.xtream

import android.util.Base64
import com.markooverhaul.sunset.core.database.CatalogueDao
import com.markooverhaul.sunset.core.database.CatalogueEpisodeEntity
import com.markooverhaul.sunset.core.database.CatalogueMetaEntity
import com.markooverhaul.sunset.core.database.CatalogueMovieEntity
import com.markooverhaul.sunset.core.database.CatalogueSeriesEntity
import com.markooverhaul.sunset.core.database.toDetail
import com.markooverhaul.sunset.core.database.toDomain
import com.markooverhaul.sunset.core.database.toSummary
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
import com.markooverhaul.sunset.core.model.isAdultChannel
import com.markooverhaul.sunset.core.model.isAdultName
import com.markooverhaul.sunset.core.model.isInternalLiveCategory
import com.markooverhaul.sunset.core.model.sanitizeProviderName
import com.markooverhaul.sunset.core.network.tmdb.TmdbEnricher
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

/**
 * Xtream Codes adapter.
 *
 * Covers authentication, account information, live categories/channels and playback, plus the VOD
 * and series catalogues with their detail responses. Catalogue lists are large (the reference panel
 * returns 52k movies in a 16.6 MB payload), so they are stored in Room and paged with indexed
 * queries. The raw JSON is still cached for six hours so a failed sync does not refetch it.
 */
@Singleton
class XtreamContentProvider @Inject constructor(
    private val api: XtreamApi,
    private val cache: XtreamCache,
    private val catalogueDao: CatalogueDao,
    private val tmdb: TmdbEnricher,
) : ContentProvider {

    private val mutex = Mutex()

    /** Serialises catalogue syncs; the catalogue lives in Room, not in a 16 MB in-memory list. */
    private val catalogueMutex = Mutex()

    private val categoryMutex = Mutex()

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var credentials: XtreamCredentials? = null

    @Volatile
    private var account: XtreamAuthResponse? = null

    @Volatile
    private var accountKey: String? = null

    @Volatile
    private var categories: List<Category>? = null

    @Volatile
    private var categoriesFetchedAt = 0L

    @Volatile
    private var channels: List<LiveChannel>? = null

    @Volatile
    private var channelsFetchedAt = 0L

    @Volatile
    private var movieCategories: List<Category>? = null

    @Volatile
    private var seriesCategories: List<Category>? = null

    private val guideMutex = Mutex()

    @Volatile
    private var guide: List<EpgProgramme>? = null

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
        val fresh = categories != null &&
            System.currentTimeMillis() - categoriesFetchedAt < LIVE_TTL_MS
        if (fresh) return@withLock categories!!
        val body = cached(
            key = "live_categories",
            action = "get_live_categories",
            maxAgeMs = LIVE_TTL_MS,
        )
        val mapped = XtreamApi.json
            .decodeFromString<List<XtreamCategoryDto>>(body)
            .mapIndexed { index, dto ->
                val name = sanitizeProviderName(dto.name)
                Category(
                    id = dto.id,
                    type = CategoryType.LIVE,
                    name = name,
                    isAdult = isAdultName(name),
                    sortOrder = index,
                )
            }
            .filter {
                it.id.isNotBlank() &&
                    it.name.isNotBlank() &&
                    !isInternalLiveCategory(it.name)
            }
        categories = mapped
        categoriesFetchedAt = System.currentTimeMillis()
        mapped
    }

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> = mutex.withLock {
        val fresh = channels != null &&
            System.currentTimeMillis() - channelsFetchedAt < LIVE_TTL_MS
        val all = if (fresh) channels!! else loadChannels()
        if (categoryId.isNullOrBlank()) all else all.filter { categoryId in it.categoryIds }
    }

    override suspend fun getGuide(days: Int): List<EpgProgramme> {
        val windowDays = days.coerceIn(1, 7)
        val now = System.currentTimeMillis()
        val from = now - HOUR_MS
        val to = now + windowDays * DAY_MS
        val all = guideMutex.withLock {
            guide ?: loadGuide().also { guide = it }
        }
        return all.filter { it.endUtcMs > from && it.startUtcMs < to }
    }

    override suspend fun refreshLive() = mutex.withLock {
        categories = null
        channels = null
        categoriesFetchedAt = 0L
        channelsFetchedAt = 0L
        cache.remove("live_categories")
        cache.remove("live_streams")
    }

    override suspend fun getMovieCategories(): List<Category> = categoryMutex.withLock {
        movieCategories ?: loadCategories("vod_categories", "get_vod_categories", CategoryType.MOVIE)
            .also { movieCategories = it }
    }

    override suspend fun getMovies(categoryId: String?, page: Int): PagedResult<Movie> {
        ensureCatalogue()
        val safePage = page.coerceAtLeast(1)
        val offset = (safePage - 1) * PAGE_SIZE
        val total = if (categoryId.isNullOrBlank()) {
            catalogueDao.movieCount()
        } else {
            catalogueDao.movieCount(categoryId)
        }
        val items = if (categoryId.isNullOrBlank()) {
            catalogueDao.moviesPage(PAGE_SIZE, offset)
        } else {
            catalogueDao.moviesPage(categoryId, PAGE_SIZE, offset)
        }
        val totalPages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
        return PagedResult(
            items = items.map { it.toDomain() },
            page = safePage,
            totalPages = totalPages,
            hasMore = offset + items.size < total,
        )
    }

    override suspend fun getMovie(id: String): Movie? {
        ensureCatalogue()
        var entity = catalogueDao.movie(id) ?: return null
        entity = enrichMovie(entity)
        return entity.toDomain()
    }

    override suspend fun getMovieSummary(id: String): Movie? {
        ensureCatalogue()
        return catalogueDao.movie(id)?.toDomain()
    }

    override suspend fun getSeriesCategories(): List<Category> = categoryMutex.withLock {
        seriesCategories
            ?: loadCategories("series_categories", "get_series_categories", CategoryType.SERIES)
                .also { seriesCategories = it }
    }

    override suspend fun getSeries(categoryId: String?, page: Int): PagedResult<SeriesSummary> {
        ensureCatalogue()
        val safePage = page.coerceAtLeast(1)
        val offset = (safePage - 1) * PAGE_SIZE
        val total = if (categoryId.isNullOrBlank()) {
            catalogueDao.seriesCount()
        } else {
            catalogueDao.seriesCount(categoryId)
        }
        val items = if (categoryId.isNullOrBlank()) {
            catalogueDao.seriesPage(PAGE_SIZE, offset)
        } else {
            catalogueDao.seriesPage(categoryId, PAGE_SIZE, offset)
        }
        val totalPages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
        return PagedResult(
            items = items.map { it.toSummary() },
            page = safePage,
            totalPages = totalPages,
            hasMore = offset + items.size < total,
        )
    }

    override suspend fun getSeriesSummary(id: String): SeriesSummary? {
        ensureCatalogue()
        return catalogueDao.series(id)?.toSummary()
    }

    override suspend fun getSeriesDetail(id: String): SeriesDetail? {
        ensureCatalogue()
        var entity = catalogueDao.series(id) ?: return null
        if (entity.episodesSyncedAtUtcMs == 0L) {
            syncSeriesEpisodes(entity)
            entity = catalogueDao.series(id) ?: entity
        }
        val episodes = catalogueDao.episodes(id)

        // TMDB metadata and episode stills are enrichment, not a prerequisite for showing the
        // series. Keeping them off the critical path is the difference between a ~1 s hero and a
        // 10 s full-screen skeleton on the Fire TV.
        if (tmdb.isConfigured) {
            providerScope.launch {
                val enriched = if (entity.tmdbId == null && entity.tmdbMatchedAtUtcMs == 0L) {
                    runCatching { tmdb.enrichSeries(entity) }.getOrDefault(entity)
                } else {
                    entity
                }
                runCatching { tmdb.enrichSeasons(enriched, episodes) }
            }
        }
        return entity.toDetail(episodes)
    }

    override suspend fun getEpisodes(seriesId: String, season: Int): List<Episode> {
        getSeriesDetail(seriesId)
        return catalogueDao.episodes(seriesId, season).map { it.toDomain() }
    }

    override suspend fun getMovieRecommendations(id: String): List<Movie> {
        ensureCatalogue()
        var entity = catalogueDao.movie(id) ?: return emptyList()
        entity = enrichMovie(entity)
        val tmdbItems = tmdb.movieRecommendations(entity).map { it.toDomain() }
        if (tmdbItems.isNotEmpty()) return tmdbItems
        val genre = entity.genres.split(',').map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: return emptyList()
        return catalogueDao.moviesByGenre(genre, entity.id, 12).map { it.toDomain() }
    }

    override suspend fun getSeriesRecommendations(id: String): List<SeriesSummary> {
        ensureCatalogue()
        var entity = catalogueDao.series(id) ?: return emptyList()
        entity = enrichSeries(entity)
        val tmdbItems = tmdb.seriesRecommendations(entity).map { it.toSummary() }
        if (tmdbItems.isNotEmpty()) return tmdbItems
        val genre = entity.genres.split(',').map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: return emptyList()
        return catalogueDao.seriesByGenre(genre, entity.id, 12).map { it.toSummary() }
    }

    override suspend fun search(query: String): SearchResults {
        val term = query.trim()
        if (term.length < MIN_QUERY) return SearchResults()
        ensureCatalogue()
        val liveMatches = mutex.withLock {
            val all = channels ?: loadChannels()
            all.filterNot { it.isAdult }
                .map { channel -> channel to fuzzyScore(channel.name, term) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(MAX_RESULTS)
        }
        val movieMatches = searchMoviesFuzzy(term)
        val seriesMatches = searchSeriesFuzzy(term)
        return SearchResults(
            live = liveMatches,
            movies = movieMatches,
            series = seriesMatches,
        )
    }

    private suspend fun searchMoviesFuzzy(query: String): List<Movie> {
        val candidates = searchCandidates(query)
            .flatMap { candidate -> catalogueDao.searchMoviesByTitle(candidate, 600) }
            .distinctBy { it.id }
        return candidates
            .map { movie -> movie to fuzzyScore(movie.title, query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first.toDomain() }
            .take(MAX_RESULTS)
    }

    private suspend fun searchSeriesFuzzy(query: String): List<SeriesSummary> {
        val candidates = searchCandidates(query)
            .flatMap { candidate -> catalogueDao.searchSeriesByTitle(candidate, 600) }
            .distinctBy { it.id }
        return candidates
            .map { series -> series to fuzzyScore(series.title, query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first.toSummary() }
            .take(MAX_RESULTS)
    }

    /** SQL `LIKE` is only a candidate filter; the fuzzy score below does the real ranking. */
    private fun searchCandidates(query: String): List<String> {
        val tokens = query.trim()
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 }
            .take(3)
        val prefixes = tokens
            .filter { it.length >= 4 }
            .map { it.take(3) }
        return (listOf(query) + tokens + prefixes)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(5)
    }

    /**
     * Exact/prefix/substring first, then token and typo matching. Returns 0 when no token matches,
     * so a typo in the middle of a word still matches ("deapool" -> "Deadpool").
     */
    private fun fuzzyScore(title: String, query: String): Int {
        val titleNorm = normalizeSearch(title)
        val queryNorm = normalizeSearch(query)
        if (titleNorm.isBlank() || queryNorm.isBlank()) return 0
        if (titleNorm == queryNorm) return 1000
        if (titleNorm.startsWith("$queryNorm ")) return 850
        if (titleNorm.contains(queryNorm)) return 700

        val titleCompact = titleNorm.replace(" ", "")
        val queryCompact = queryNorm.replace(" ", "")
        if (titleCompact == queryCompact) return 900
        if (titleCompact.contains(queryCompact)) return 520
        if (queryCompact.length >= 5 && levenshtein(titleCompact, queryCompact) <= 1) {
            return 450
        }

        val titleTokens = titleNorm.split(' ').filter { it.isNotBlank() }
        val queryTokens = queryNorm.split(' ').filter { it.isNotBlank() }
        var total = 0
        for (queryToken in queryTokens) {
            val best = titleTokens.maxOfOrNull { titleToken ->
                when {
                    titleToken == queryToken -> 120
                    titleToken.startsWith(queryToken) -> 90
                    queryToken.length >= 4 &&
                        levenshtein(titleToken, queryToken) <= 1 -> 60
                    else -> 0
                }
            } ?: 0
            if (best == 0) return 0
            total += best
        }
        return total
    }

    private fun normalizeSearch(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.US)
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
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
                val ext = catalogueDao.movie(request.contentId)?.containerExtension
                    ?: DEFAULT_EXTENSION
                StreamSource(
                    url = "$base/movie/$user/$pass/${encodeSegment(request.contentId)}.$ext",
                    format = formatOf(ext),
                )
            }

            ContentType.SERIES, ContentType.EPISODE -> {
                val id = request.episodeId ?: request.contentId
                val ext = catalogueDao.episode(id)?.containerExtension ?: DEFAULT_EXTENSION
                StreamSource(
                    url = "$base/series/$user/$pass/${encodeSegment(id)}.$ext",
                    format = formatOf(ext),
                )
            }
        }
    }

    private suspend fun loadChannels(): List<LiveChannel> {
        val body = cached("live_streams", "get_live_streams", maxAgeMs = LIVE_TTL_MS)
        val categoryNames = runCatching {
            XtreamApi.json
                .decodeFromString<List<XtreamCategoryDto>>(
                    cached("live_categories", "get_live_categories", maxAgeMs = LIVE_TTL_MS),
                )
                .associate { it.id to it.name }
        }.getOrDefault(emptyMap())
        // Admin categories are removed app-wide, including the channels they would have exposed.
        val internalCategoryIds = categoryNames
            .filterValues { isInternalLiveCategory(it) }
            .keys
        val mapped = XtreamApi.json
            .decodeFromString<List<XtreamLiveStreamDto>>(body)
            .mapIndexedNotNull { index, dto ->
                if (dto.streamId.isBlank()) return@mapIndexedNotNull null
                if (dto.categoryId in internalCategoryIds) return@mapIndexedNotNull null
                val categoryName = categoryNames[dto.categoryId]
                val resolved = CountryResolver.resolve(
                    rawName = dto.name,
                    categoryName = categoryName,
                )
                val displayName = sanitizeProviderName(resolved.name)
                    .ifBlank { "Channel ${dto.streamId}" }
                LiveChannel(
                    id = dto.streamId,
                    name = displayName,
                    number = dto.num.takeIf { it > 0 } ?: (index + 1),
                    logoUrl = dto.streamIcon.takeIf { it.isNotBlank() },
                    categoryIds = listOfNotNull(dto.categoryId.takeIf { it.isNotBlank() }),
                    streamRef = dto.streamId,
                    epgChannelId = dto.epgChannelId.takeIf { it.isNotBlank() },
                    hasCatchUp = dto.tvArchive == 1,
                    catchUpDays = dto.tvArchiveDuration,
                    countryCode = resolved.countryCode,
                    isAdult = isAdultChannel(displayName, listOf(categoryName)),
                )
            }
        channels = mapped
        channelsFetchedAt = System.currentTimeMillis()
        return mapped
    }

    /**
     * XMLTV keys programmes by the provider's EPG channel id; the UI matches programmes to
     * [LiveChannel.id] (the stream id), so translate once while parsing.
     */
    private suspend fun loadGuide(): List<EpgProgramme> {
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        val body = cache.read(GUIDE_KEY)
            ?: api.xmltv(credentials.baseUrl, credentials.username, credentials.password)
                .also { cache.write(GUIDE_KEY, it) }
        val liveChannels = mutex.withLock { channels ?: loadChannels() }
        val streamIdByEpgId = liveChannels
            .mapNotNull { channel -> channel.epgChannelId?.let { it to channel.id } }
            .toMap()
        if (streamIdByEpgId.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            XmltvParser.parse(body)
                .mapNotNull { programme ->
                    val streamId = streamIdByEpgId[programme.channelId] ?: return@mapNotNull null
                    EpgProgramme(
                        id = "${programme.channelId}-${programme.startUtcMs}",
                        channelId = streamId,
                        title = programme.title,
                        description = programme.description,
                        startUtcMs = programme.startUtcMs,
                        endUtcMs = programme.endUtcMs,
                        category = programme.category,
                        rating = programme.rating,
                        iconUrl = programme.iconUrl,
                    )
                }
                .sortedBy { it.startUtcMs }
        }
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

    private suspend fun ensureCatalogue() {
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        val key = accountKey ?: return
        val meta = catalogueDao.metaFor(key)
        if (meta != null && meta.movieCount > 0) {
            if (System.currentTimeMillis() - meta.lastSyncUtcMs > CATALOGUE_TTL_MS) {
                providerScope.launch {
                    catalogueMutex.withLock {
                        runCatching { syncCatalogue(credentials, key) }
                    }
                }
            }
            return
        }

        catalogueMutex.withLock {
            val again = catalogueDao.metaFor(key)
            if (again != null && again.movieCount > 0) return
            val existing = catalogueDao.meta()
            if (existing != null && existing.accountKey != key) {
                catalogueDao.clearCatalogue()
                cache.clear()
                cache.write(ACCOUNT_KEY, key)
            }
            syncCatalogue(credentials, key)
        }
    }

    private suspend fun syncCatalogue(credentials: XtreamCredentials, key: String) {
        // `cached` downloads once and keeps the raw payload for six hours, so a failed insert can be
        // retried without pulling another 16 MB.
        val moviesBody = cached("vod_streams", "get_vod_streams")
        val seriesBody = cached("series", "get_series")
        val movies = withContext(Dispatchers.IO) { parseMovieEntities(moviesBody) }
        val series = withContext(Dispatchers.IO) { parseSeriesEntities(seriesBody) }
        catalogueDao.replaceCatalogue(
            meta = CatalogueMetaEntity(
                accountKey = key,
                lastSyncUtcMs = System.currentTimeMillis(),
                movieCount = movies.size,
                seriesCount = series.size,
            ),
            movies = movies,
            series = series,
        )
        providerScope.launch {
            runCatching { backfillArtwork() }
        }
    }

    /**
     * Fills missing card artwork for the most recent titles. Bounded and sequential so a catalogue
     * with tens of thousands of titles never turns into a TMDB crawl.
     */
    private suspend fun backfillArtwork() {
        if (!tmdb.isConfigured) return
        catalogueDao.moviesMissingArtwork(60).forEach { movie ->
            runCatching { tmdb.enrichMovie(movie) }
        }
        catalogueDao.seriesMissingArtwork(40).forEach { series ->
            runCatching { tmdb.enrichSeries(series) }
        }
    }

    private fun parseMovieEntities(body: String): List<CatalogueMovieEntity> =
        XtreamApi.json
            .decodeFromString<List<XtreamVodStreamDto>>(body)
            .mapNotNull { dto ->
                if (dto.streamId.isBlank()) return@mapNotNull null
                val (title, year) = splitTitleYear(dto.name)
                CatalogueMovieEntity(
                    id = dto.streamId,
                    categoryId = dto.categoryId,
                    title = title,
                    year = year,
                    posterUrl = dto.streamIcon.takeIf { it.isNotBlank() },
                    rating = dto.rating.toDoubleOrNull(),
                    addedAtUtcMs = dto.added.toLongOrNull()?.times(1000L) ?: 0L,
                    streamRef = dto.streamId,
                    containerExtension = dto.containerExtension.ifBlank { DEFAULT_EXTENSION },
                )
            }

    private fun parseSeriesEntities(body: String): List<CatalogueSeriesEntity> =
        XtreamApi.json
            .decodeFromString<List<XtreamSeriesDto>>(body)
            .mapNotNull { dto ->
                if (dto.seriesId.isBlank()) return@mapNotNull null
                CatalogueSeriesEntity(
                    id = dto.seriesId,
                    categoryId = dto.categoryId,
                    title = dto.name,
                    year = dto.releaseDate.take(4).toIntOrNull(),
                    posterUrl = dto.cover.takeIf { it.isNotBlank() },
                    rating = dto.rating.toDoubleOrNull(),
                    genres = splitGenres(dto.genre).joinToString(", "),
                    streamRef = dto.seriesId,
                )
            }

    /** Lazily loads `get_vod_info` once per title and then applies the TMDB layer. */
    private suspend fun enrichMovie(entity: CatalogueMovieEntity): CatalogueMovieEntity {
        if (entity.detailSyncedAtUtcMs > 0L) return tmdb.enrichMovie(entity)
        val credentials = credentials ?: return entity
        val body = runCatching {
            api.call(
                credentials.baseUrl,
                credentials.username,
                credentials.password,
                action = "get_vod_info",
                params = mapOf("vod_id" to entity.id),
            )
        }.getOrNull() ?: return tmdb.enrichMovie(entity)
        val info = runCatching {
            XtreamApi.json.decodeFromString<XtreamVodInfoResponse>(body)
        }.getOrNull() ?: return tmdb.enrichMovie(entity)
        val enriched = entity.copy(
            plot = info.info?.plot?.takeIf { it.isNotBlank() } ?: entity.plot,
            cast = splitPeople(info.info?.cast).joinToString(", ").ifBlank { entity.cast },
            director = info.info?.director?.takeIf { it.isNotBlank() } ?: entity.director,
            genres = splitGenres(info.info?.genre).joinToString(", ").ifBlank { entity.genres },
            durationMinutes = parseDurationMinutes(info.info?.duration) ?: entity.durationMinutes,
            backdropUrl = info.info?.backdrop?.firstOrNull { it.isNotBlank() }
                ?: entity.backdropUrl,
            detailSyncedAtUtcMs = System.currentTimeMillis(),
        )
        catalogueDao.updateMovie(enriched)
        return tmdb.enrichMovie(enriched)
    }

    private suspend fun enrichSeries(entity: CatalogueSeriesEntity): CatalogueSeriesEntity =
        tmdb.enrichSeries(entity)

    private suspend fun syncSeriesEpisodes(entity: CatalogueSeriesEntity) {
        val credentials = credentials ?: return
        val response = runCatching {
            api.call(
                credentials.baseUrl,
                credentials.username,
                credentials.password,
                action = "get_series_info",
                params = mapOf("series_id" to entity.id),
            )
        }.getOrNull()?.let {
            runCatching { XtreamApi.json.decodeFromString<XtreamSeriesInfoResponse>(it) }.getOrNull()
        } ?: return

        val info = response.info
        val episodes = mutableListOf<CatalogueEpisodeEntity>()
        response.episodes.forEach { (seasonKey, items) ->
            val seasonNumber = seasonKey.toIntOrNull() ?: return@forEach
            items.forEach { episode ->
                if (episode.id.isBlank()) return@forEach
                episodes += CatalogueEpisodeEntity(
                    id = episode.id,
                    seriesId = entity.id,
                    seasonNumber = seasonNumber,
                    episodeNumber = episode.episodeNum,
                    title = episode.title.ifBlank { "Episode ${episode.episodeNum}" },
                    plot = episode.info?.plot?.takeIf { it.isNotBlank() },
                    durationMinutes = parseDurationMinutes(episode.info?.duration),
                    stillUrl = episode.info?.image?.takeIf { it.isNotBlank() },
                    airDateUtcMs = parseDateUtcMs(episode.info?.releaseDate),
                    streamRef = episode.id,
                    containerExtension = episode.containerExtension.ifBlank { DEFAULT_EXTENSION },
                )
            }
        }
        val now = System.currentTimeMillis()
        val updated = entity.copy(
            title = info?.name?.takeIf { it.isNotBlank() } ?: entity.title,
            posterUrl = info?.cover?.takeIf { it.isNotBlank() } ?: entity.posterUrl,
            backdropUrl = info?.backdrop?.firstOrNull { it.isNotBlank() } ?: entity.backdropUrl,
            rating = info?.rating?.toDoubleOrNull() ?: entity.rating,
            genres = splitGenres(info?.genre).joinToString(", ").ifBlank { entity.genres },
            plot = info?.plot?.takeIf { it.isNotBlank() } ?: entity.plot,
            cast = splitPeople(info?.cast).joinToString(", ").ifBlank { entity.cast },
            seasonCount = maxOf(entity.seasonCount, episodes.map { it.seasonNumber }.distinct().size),
            episodesSyncedAtUtcMs = now,
        )
        catalogueDao.updateSeries(updated)
        catalogueDao.replaceEpisodes(entity.id, episodes, now)
    }

    /** Titles arrive as "Name (1999)" or "Name (1999) SUB"; split the year for the caption. */
    private fun splitTitleYear(raw: String): Pair<String, Int?> {
        val match = TITLE_YEAR.find(raw) ?: return raw.trim() to null
        val year = match.groupValues[1].toIntOrNull()
        val withoutYear = raw.removeRange(match.range)
            .replace(WHITESPACE, " ")
            .trim()
            .trimEnd('-', '–')
            .trim()
        val title = withoutYear.replace(QUALITY_SUFFIX, "").trim()
        return (title.ifBlank { withoutYear.ifBlank { raw.trim() } }) to year
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

    private suspend fun cached(
        key: String,
        action: String,
        maxAgeMs: Long = XtreamCache.DEFAULT_TTL_MS,
    ): String {
        val credentials = credentials ?: throw ProviderError.ServiceNotConfigured
        cache.read(key, maxAgeMs)?.let { return it }
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
        this.accountKey = accountKey
        val cachedAccount = cache.read(ACCOUNT_KEY, maxAgeMs = Long.MAX_VALUE)
        if (cachedAccount != accountKey) {
            categories = null
            channels = null
            movieCategories = null
            seriesCategories = null
            guide = null
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
        const val GUIDE_KEY = "xmltv"
        const val CATALOGUE_TTL_MS = 6 * 60 * 60 * 1000L
        const val LIVE_TTL_MS = 15 * 60 * 1000L
        const val HOUR_MS = 60 * 60 * 1000L
        const val DAY_MS = 24 * HOUR_MS
        val TITLE_YEAR = Regex("""\((\d{4})\)""")
        val WHITESPACE = Regex("""\s+""")
        val QUALITY_SUFFIX = Regex(
            """\s+(?:SUB|DUB|LAT|LATINO|CAST|ESP|ING|HD|FHD|UHD|4K)$""",
            RegexOption.IGNORE_CASE,
        )
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
