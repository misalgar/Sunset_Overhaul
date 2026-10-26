package com.markooverhaul.sunset.core.database

import com.markooverhaul.sunset.core.model.Episode
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.Season
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary

private fun String?.splitPeople(): List<String> =
    this.orEmpty().split(',', ';', '/').map { it.trim() }.filter { it.isNotEmpty() }

private fun String?.splitGenres(): List<String> =
    this.orEmpty().split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private val QUALITY_SUFFIX = Regex(
    """\s+(?:SUB|DUB|LAT|LATINO|CAST|ESP|ING|HD|FHD|UHD|4K)$""",
    RegexOption.IGNORE_CASE,
)

/** Cleans titles stored by older catalogue versions without requiring a resync. */
private fun String.cleanDisplayTitle(): String =
    replace(QUALITY_SUFFIX, "").trim().ifBlank { this }

/** Restores TMDB enrichment onto a freshly parsed row so a resync keeps the existing matches. */
fun CatalogueMovieEntity.withTmdbState(state: MovieTmdbState?): CatalogueMovieEntity =
    if (state == null) {
        this
    } else {
        copy(
            tmdbId = state.tmdbId,
            tmdbPosterUrl = state.tmdbPosterUrl,
            tmdbBackdropUrl = state.tmdbBackdropUrl,
            tmdbOverview = state.tmdbOverview,
            tmdbRating = state.tmdbRating,
            tmdbGenres = state.tmdbGenres,
            tmdbCast = state.tmdbCast,
            tmdbRuntime = state.tmdbRuntime,
            tmdbCertification = state.tmdbCertification,
            tmdbMatchedAtUtcMs = state.tmdbMatchedAtUtcMs,
            tmdbLanguage = state.tmdbLanguage,
        )
    }

/**
 * Domain mapping policy: provider identity and playback fields always win. TMDB supplies the
 * editorial fields (overview, cast, genres, runtime, certification) and fills missing artwork,
 * ratings or titles. This keeps stream ids stable while replacing thin provider descriptions.
 */
fun CatalogueMovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title.cleanDisplayTitle(),
    year = year,
    plot = tmdbOverview.clean() ?: plot.clean(),
    posterUrl = posterUrl.clean() ?: tmdbPosterUrl.clean(),
    backdropUrl = backdropUrl.clean() ?: tmdbBackdropUrl.clean(),
    rating = rating ?: tmdbRating,
    durationMinutes = tmdbRuntime ?: durationMinutes,
    genres = tmdbGenres.splitGenres().ifEmpty { genres.splitGenres() },
    cast = tmdbCast.splitPeople().ifEmpty { cast.splitPeople() },
    director = director,
    streamRef = streamRef.ifBlank { id },
    tmdbId = tmdbId,
    certification = tmdbCertification.clean() ?: certification.clean(),
    addedAtUtcMs = addedAtUtcMs,
    isAdult = isAdult,
    language = language,
    quality = quality,
)

fun CatalogueSeriesEntity.toSummary(): SeriesSummary = SeriesSummary(
    id = id,
    title = title.cleanDisplayTitle(),
    year = year,
    posterUrl = posterUrl.clean() ?: tmdbPosterUrl.clean(),
    backdropUrl = backdropUrl.clean() ?: tmdbBackdropUrl.clean(),
    rating = rating ?: tmdbRating,
    seasonCount = seasonCount,
    genres = tmdbGenres.splitGenres().ifEmpty { genres.splitGenres() },
    streamRef = streamRef.ifBlank { id },
    tmdbId = tmdbId,
    addedAtUtcMs = addedAtUtcMs,
    isAdult = isAdult,
    language = language,
    quality = quality,
)

fun CatalogueSeriesEntity.toDetail(episodes: List<CatalogueEpisodeEntity>): SeriesDetail {
    val seasons = episodes
        .groupBy { it.seasonNumber }
        .toSortedMap()
        .map { (seasonNumber, items) ->
            Season(
                number = seasonNumber,
                episodes = items.sortedBy { it.episodeNumber }.map { it.toDomain() },
            )
        }
    return SeriesDetail(
        summary = toSummary().copy(
            seasonCount = seasons.size.coerceAtLeast(seasonCount),
        ),
        plot = tmdbOverview.clean() ?: plot.clean(),
        cast = tmdbCast.splitPeople().ifEmpty { cast.splitPeople() },
        seasons = seasons,
        certification = tmdbCertification.clean() ?: certification.clean(),
    )
}

fun CatalogueEpisodeEntity.toDomain(): Episode = Episode(
    id = id,
    seriesId = seriesId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = title.cleanDisplayTitle(),
    plot = tmdbOverview.clean() ?: plot.clean(),
    durationMinutes = tmdbRuntime ?: durationMinutes,
    stillUrl = stillUrl.clean() ?: tmdbStillUrl.clean(),
    airDateUtcMs = airDateUtcMs ?: tmdbAirDateUtcMs,
    streamRef = streamRef.ifBlank { id },
)
