package com.markooverhaul.sunset.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val addedAtUtcMs: Long,
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val episodeId: String? = null,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtUtcMs: Long,
    val completed: Boolean = false,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val lastUsedAtUtcMs: Long,
)

/** Marks which account's catalogue is in the database and when it was last refreshed. */
@Entity(tableName = "catalogue_meta")
data class CatalogueMetaEntity(
    @PrimaryKey val accountKey: String,
    val lastSyncUtcMs: Long,
    val movieCount: Int,
    val seriesCount: Int,
)

/**
 * A VOD row from `get_vod_streams`, enriched lazily with `get_vod_info` and TMDB fields.
 * Provider values are kept separate from TMDB values so the mapper can choose the best source.
 */
@Entity(
    tableName = "catalogue_movies",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["addedAtUtcMs"]),
        Index(value = ["title"]),
        Index(value = ["titleKey"]),
        Index(value = ["groupKey"]),
        Index(value = ["isDefault", "addedAtUtcMs"]),
    ],
)
data class CatalogueMovieEntity(
    @PrimaryKey val id: String,
    val categoryId: String = "",
    val language: String? = null,
    val quality: String? = null,
    val title: String,
    val year: Int? = null,
    val plot: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val durationMinutes: Int? = null,
    val genres: String = "",
    val cast: String = "",
    val director: String? = null,
    val streamRef: String = "",
    val certification: String? = null,
    val addedAtUtcMs: Long = 0L,
    val isAdult: Boolean = false,
    val containerExtension: String = "mp4",
    val detailSyncedAtUtcMs: Long = 0L,
    val tmdbId: String? = null,
    val tmdbPosterUrl: String? = null,
    val tmdbBackdropUrl: String? = null,
    val tmdbOverview: String? = null,
    val tmdbRating: Double? = null,
    val tmdbGenres: String = "",
    val tmdbCast: String = "",
    val tmdbRuntime: Int? = null,
    val tmdbCertification: String? = null,
    val tmdbMatchedAtUtcMs: Long = 0L,
    val tmdbLanguage: String? = null,
    /** Normalised title plus year; rows with the same key are versions of one title. */
    val titleKey: String? = null,
    /** Shared by every version of a title. Derived by [CatalogueVariantIndex]. */
    val groupKey: String? = null,
    /** True for the one version shown as the card; the others appear only on details. */
    @ColumnInfo(defaultValue = "1") val isDefault: Boolean = true,
)

@Entity(
    tableName = "catalogue_series",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["addedAtUtcMs"]),
        Index(value = ["title"]),
    ],
)
data class CatalogueSeriesEntity(
    @PrimaryKey val id: String,
    val categoryId: String = "",
    val language: String? = null,
    val quality: String? = null,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val seasonCount: Int = 0,
    val genres: String = "",
    val streamRef: String = "",
    val addedAtUtcMs: Long = 0L,
    val isAdult: Boolean = false,
    val plot: String? = null,
    val cast: String = "",
    val certification: String? = null,
    val episodesSyncedAtUtcMs: Long = 0L,
    val tmdbId: String? = null,
    val tmdbPosterUrl: String? = null,
    val tmdbBackdropUrl: String? = null,
    val tmdbOverview: String? = null,
    val tmdbRating: Double? = null,
    val tmdbGenres: String = "",
    val tmdbCast: String = "",
    val tmdbRuntime: Int? = null,
    val tmdbCertification: String? = null,
    val tmdbMatchedAtUtcMs: Long = 0L,
    val tmdbLanguage: String? = null,
)

@Entity(
    tableName = "catalogue_episodes",
    primaryKeys = ["id"],
    indices = [Index(value = ["seriesId", "seasonNumber", "episodeNumber"])],
)
data class CatalogueEpisodeEntity(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val plot: String? = null,
    val durationMinutes: Int? = null,
    val stillUrl: String? = null,
    val airDateUtcMs: Long? = null,
    val streamRef: String = "",
    val containerExtension: String = "mp4",
    val tmdbStillUrl: String? = null,
    val tmdbOverview: String? = null,
    val tmdbRuntime: Int? = null,
    val tmdbAirDateUtcMs: Long? = null,
    val tmdbMatchedAtUtcMs: Long = 0L,
)
