package com.markooverhaul.sunset.core.network.tmdb

import com.markooverhaul.sunset.core.database.CatalogueDao
import com.markooverhaul.sunset.core.database.CatalogueEpisodeEntity
import com.markooverhaul.sunset.core.database.CatalogueMovieEntity
import com.markooverhaul.sunset.core.database.CatalogueSeriesEntity
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches provider titles to TMDB and persists the enrichment. Matching is deliberately
 * conservative: an exact/normalised title (with a compatible year) is required, so a wrong poster
 * or plot is never attached to a different film.
 */
@Singleton
class TmdbEnricher @Inject constructor(
    private val api: TmdbApi,
    private val dao: CatalogueDao,
) {
    val isConfigured: Boolean get() = api.isConfigured

    suspend fun enrichMovie(entity: CatalogueMovieEntity): CatalogueMovieEntity {
        if (!api.isConfigured) return entity
        if (isFresh(entity.tmdbMatchedAtUtcMs)) return entity
        val now = System.currentTimeMillis()
        val results = api.searchMovie(entity.title, entity.year)
        val match = matchTitle(results, entity.title, entity.year)
        if (match == null) {
            // An empty result set can mean a transient API failure; leave it unmatched so a later
            // attempt can still succeed. Only a real result set with no match is cached.
            if (results.isEmpty()) return entity
            return entity.copy(tmdbMatchedAtUtcMs = now, tmdbLanguage = api.language)
                .also { dao.updateMovie(it) }
        }
        val detail = api.movieDetail(match.id) ?: return entity
        val updated = entity.copy(
            tmdbId = detail.id.toString(),
            tmdbPosterUrl = api.posterUrl(detail.posterPath) ?: entity.tmdbPosterUrl,
            tmdbBackdropUrl = api.backdropUrl(detail.backdropPath) ?: entity.tmdbBackdropUrl,
            tmdbOverview = detail.overview.takeIf { it.isNotBlank() } ?: entity.tmdbOverview,
            tmdbRating = detail.voteAverage ?: entity.tmdbRating,
            tmdbGenres = detail.genres.map { it.name }.filter { it.isNotBlank() }
                .joinToString(", ").ifBlank { entity.tmdbGenres },
            tmdbCast = detail.credits?.cast
                ?.sortedBy { it.order }
                ?.map { it.name }
                ?.filter { it.isNotBlank() }
                ?.take(10)
                ?.joinToString(", ")
                .orEmpty()
                .ifBlank { entity.tmdbCast },
            tmdbRuntime = detail.runtime ?: entity.tmdbRuntime,
            tmdbCertification = api.certification(detail.releaseDates) ?: entity.tmdbCertification,
            tmdbMatchedAtUtcMs = now,
            tmdbLanguage = api.language,
        )
        dao.updateMovie(updated)
        return updated
    }

    suspend fun enrichSeries(entity: CatalogueSeriesEntity): CatalogueSeriesEntity {
        if (!api.isConfigured) return entity
        if (isFresh(entity.tmdbMatchedAtUtcMs)) return entity
        val now = System.currentTimeMillis()
        val results = api.searchSeries(entity.title, entity.year)
        val match = matchTitle(results, entity.title, entity.year)
        if (match == null) {
            if (results.isEmpty()) return entity
            return entity.copy(tmdbMatchedAtUtcMs = now, tmdbLanguage = api.language)
                .also { dao.updateSeries(it) }
        }
        val detail = api.seriesDetail(match.id) ?: return entity
        val updated = entity.copy(
            tmdbId = detail.id.toString(),
            tmdbPosterUrl = api.posterUrl(detail.posterPath) ?: entity.tmdbPosterUrl,
            tmdbBackdropUrl = api.backdropUrl(detail.backdropPath) ?: entity.tmdbBackdropUrl,
            tmdbOverview = detail.overview.takeIf { it.isNotBlank() } ?: entity.tmdbOverview,
            tmdbRating = detail.voteAverage ?: entity.tmdbRating,
            tmdbGenres = detail.genres.map { it.name }.filter { it.isNotBlank() }
                .joinToString(", ").ifBlank { entity.tmdbGenres },
            tmdbCast = detail.credits?.cast
                ?.sortedBy { it.order }
                ?.map { it.name }
                ?.filter { it.isNotBlank() }
                ?.take(10)
                ?.joinToString(", ")
                .orEmpty()
                .ifBlank { entity.tmdbCast },
            tmdbRuntime = detail.episodeRunTime.firstOrNull() ?: entity.tmdbRuntime,
            tmdbCertification = api.certification(detail.contentRatings)
                ?: entity.tmdbCertification,
            seasonCount = maxOf(entity.seasonCount, detail.numberOfSeasons),
            tmdbMatchedAtUtcMs = now,
            tmdbLanguage = api.language,
        )
        dao.updateSeries(updated)
        return updated
    }

    /**
     * Fills episode stills/overviews for up to three seasons. TMDB and the panel occasionally
     * number episodes differently, so only matching episode numbers are enriched.
     */
    suspend fun enrichSeasons(
        series: CatalogueSeriesEntity,
        episodes: List<CatalogueEpisodeEntity>,
    ) {
        if (!api.isConfigured) return
        val seriesId = series.tmdbId?.toLongOrNull() ?: return
        val seasons = episodes
            .groupBy { it.seasonNumber }
            .entries
            .sortedBy { it.key }
            .take(3)
        for ((season, items) in seasons) {
            if (items.all { isFresh(it.tmdbMatchedAtUtcMs) }) continue
            val detail = api.seasonDetail(seriesId, season) ?: continue
            val byNumber = detail.episodes.associateBy { it.episodeNumber }
            val now = System.currentTimeMillis()
            for (episode in items) {
                val tmdb = byNumber[episode.episodeNumber] ?: continue
                dao.updateEpisode(
                    episode.copy(
                        tmdbStillUrl = api.stillUrl(tmdb.stillPath) ?: episode.tmdbStillUrl,
                        tmdbOverview = tmdb.overview.takeIf { it.isNotBlank() }
                            ?: episode.tmdbOverview,
                        tmdbRuntime = tmdb.runtime ?: episode.tmdbRuntime,
                        tmdbAirDateUtcMs = parseDate(tmdb.airDate) ?: episode.tmdbAirDateUtcMs,
                        tmdbMatchedAtUtcMs = now,
                    ),
                )
            }
        }
    }

    private fun matchTitle(
        results: List<TmdbSearchItem>,
        title: String,
        year: Int?,
    ): TmdbSearchItem? {
        val query = normalize(title.replace(QUALITY_SUFFIX, " "))
        if (query.isBlank()) return null
        val scored = results.flatMap { item ->
            listOf(item.displayTitle, item.displayOriginal)
                .map { normalize(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .map { item to it }
        }

        for ((item, candidate) in scored) {
            if (candidate == query && yearsCompatible(year, item.year)) return item
        }
        for ((item, candidate) in scored) {
            if ((candidate.startsWith(query) || query.startsWith(candidate)) &&
                yearsCompatible(year, item.year)
            ) {
                return item
            }
        }
        for ((item, candidate) in scored) {
            if (candidate.contains(query) && yearsCompatibleStrict(year, item.year)) return item
        }
        return null
    }

    private fun yearsCompatible(expected: Int?, candidate: Int?): Boolean {
        if (expected == null || candidate == null) return true
        return kotlin.math.abs(expected - candidate) <= 1
    }

    private fun yearsCompatibleStrict(expected: Int?, candidate: Int?): Boolean {
        if (expected == null) return true
        if (candidate == null) return false
        return kotlin.math.abs(expected - candidate) <= 1
    }

    private fun isFresh(matchedAtUtcMs: Long): Boolean =
        matchedAtUtcMs > 0L &&
            System.currentTimeMillis() - matchedAtUtcMs < MATCH_TTL_MS

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.US)
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun parseDate(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.length < 10) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(value.take(10))
                ?.time
        }.getOrNull()
    }

    private companion object {
        const val MATCH_TTL_MS = 30L * 24 * 60 * 60 * 1000
        val QUALITY_SUFFIX = Regex(
            """\s+(?:SUB|DUB|LAT|LATINO|CAST|ESP|ING|HD|FHD|UHD|4K)$""",
            RegexOption.IGNORE_CASE,
        )
    }
}
