package com.markooverhaul.sunset.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CatalogueDao {
    @Query("SELECT * FROM catalogue_meta LIMIT 1")
    suspend fun meta(): CatalogueMetaEntity?

    @Query("SELECT * FROM catalogue_meta WHERE accountKey = :accountKey LIMIT 1")
    suspend fun metaFor(accountKey: String): CatalogueMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(entity: CatalogueMetaEntity)

    @Query("SELECT COUNT(*) FROM catalogue_movies WHERE isDefault = 1")
    suspend fun movieCount(): Int

    @Query("SELECT COUNT(*) FROM catalogue_movies WHERE isDefault = 1 AND categoryId = :categoryId")
    suspend fun movieCount(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM catalogue_series")
    suspend fun seriesCount(): Int

    @Query("SELECT COUNT(*) FROM catalogue_series WHERE categoryId = :categoryId")
    suspend fun seriesCount(categoryId: String): Int

    @Query(
        "SELECT * FROM catalogue_movies WHERE isDefault = 1 " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit OFFSET :offset",
    )
    suspend fun moviesPage(limit: Int, offset: Int): List<CatalogueMovieEntity>

    @Query(
        "SELECT * FROM catalogue_movies WHERE isDefault = 1 AND categoryId = :categoryId " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit OFFSET :offset",
    )
    suspend fun moviesPage(categoryId: String, limit: Int, offset: Int): List<CatalogueMovieEntity>

    @Query(
        "SELECT * FROM catalogue_movies WHERE isDefault = 1 AND title LIKE '%' || :term || '%' " +
            "ORDER BY title LIMIT :limit",
    )
    suspend fun searchMoviesByTitle(term: String, limit: Int): List<CatalogueMovieEntity>

    @Query(
        "SELECT * FROM catalogue_movies WHERE isDefault = 1 AND id != :excludeId " +
            "AND genres LIKE '%' || :genre || '%' ORDER BY addedAtUtcMs DESC, title LIMIT :limit",
    )
    suspend fun moviesByGenre(
        genre: String,
        excludeId: String,
        limit: Int,
    ): List<CatalogueMovieEntity>

    @Query(
        "SELECT * FROM catalogue_series WHERE title LIKE '%' || :term || '%' " +
            "ORDER BY title LIMIT :limit",
    )
    suspend fun searchSeriesByTitle(term: String, limit: Int): List<CatalogueSeriesEntity>

    @Query(
        "SELECT * FROM catalogue_series WHERE id != :excludeId AND genres LIKE '%' || :genre || '%' " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit",
    )
    suspend fun seriesByGenre(
        genre: String,
        excludeId: String,
        limit: Int,
    ): List<CatalogueSeriesEntity>

    @Query("SELECT * FROM catalogue_movies WHERE id = :id LIMIT 1")
    suspend fun movie(id: String): CatalogueMovieEntity?

    @Query(
        "SELECT * FROM catalogue_movies WHERE isDefault = 1 AND title LIKE '%' || :term || '%' " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit",
    )
    suspend fun searchMovies(term: String, limit: Int): List<CatalogueMovieEntity>

    @Query(
        "SELECT * FROM catalogue_movies WHERE posterUrl IS NULL AND tmdbPosterUrl IS NULL " +
            "ORDER BY addedAtUtcMs DESC LIMIT :limit",
    )
    suspend fun moviesMissingArtwork(limit: Int): List<CatalogueMovieEntity>

    @Update
    suspend fun updateMovie(entity: CatalogueMovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(items: List<CatalogueMovieEntity>)

    @Query("DELETE FROM catalogue_movies")
    suspend fun clearMovies()

    @Query("SELECT * FROM catalogue_movies WHERE groupKey = :groupKey")
    suspend fun movieVariants(groupKey: String): List<CatalogueMovieEntity>

    @Query(
        "SELECT id, titleKey, tmdbId, language, quality, addedAtUtcMs, groupKey, isDefault " +
            "FROM catalogue_movies",
    )
    suspend fun movieVariantRows(): List<MovieVariantRow>

    @Query(
        "SELECT id, tmdbId, tmdbPosterUrl, tmdbBackdropUrl, tmdbOverview, tmdbRating, tmdbGenres, " +
            "tmdbCast, tmdbRuntime, tmdbCertification, tmdbMatchedAtUtcMs, tmdbLanguage " +
            "FROM catalogue_movies WHERE tmdbId IS NOT NULL OR tmdbMatchedAtUtcMs > 0",
    )
    suspend fun movieTmdbStates(): List<MovieTmdbState>

    @Query("UPDATE catalogue_movies SET groupKey = :groupKey, isDefault = :isDefault WHERE id = :id")
    suspend fun setMovieVariant(id: String, groupKey: String, isDefault: Boolean)

    @Transaction
    suspend fun applyMovieVariants(updates: List<MovieVariantUpdate>) {
        updates.forEach { setMovieVariant(it.id, it.groupKey, it.isDefault) }
    }

    @Query(
        "SELECT * FROM catalogue_series ORDER BY addedAtUtcMs DESC, title " +
            "LIMIT :limit OFFSET :offset",
    )
    suspend fun seriesPage(limit: Int, offset: Int): List<CatalogueSeriesEntity>

    @Query(
        "SELECT * FROM catalogue_series WHERE categoryId = :categoryId " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit OFFSET :offset",
    )
    suspend fun seriesPage(categoryId: String, limit: Int, offset: Int): List<CatalogueSeriesEntity>

    @Query("SELECT * FROM catalogue_series WHERE id = :id LIMIT 1")
    suspend fun series(id: String): CatalogueSeriesEntity?

    @Query(
        "SELECT * FROM catalogue_series WHERE title LIKE '%' || :term || '%' " +
            "ORDER BY addedAtUtcMs DESC, title LIMIT :limit",
    )
    suspend fun searchSeries(term: String, limit: Int): List<CatalogueSeriesEntity>

    @Query(
        "SELECT * FROM catalogue_series WHERE posterUrl IS NULL AND tmdbPosterUrl IS NULL " +
            "ORDER BY addedAtUtcMs DESC LIMIT :limit",
    )
    suspend fun seriesMissingArtwork(limit: Int): List<CatalogueSeriesEntity>

    @Update
    suspend fun updateSeries(entity: CatalogueSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(items: List<CatalogueSeriesEntity>)

    @Query("DELETE FROM catalogue_series")
    suspend fun clearSeries()

    @Query(
        "SELECT * FROM catalogue_episodes WHERE seriesId = :seriesId " +
            "ORDER BY seasonNumber, episodeNumber",
    )
    suspend fun episodes(seriesId: String): List<CatalogueEpisodeEntity>

    @Query(
        "SELECT * FROM catalogue_episodes WHERE seriesId = :seriesId AND seasonNumber = :season " +
            "ORDER BY episodeNumber",
    )
    suspend fun episodes(seriesId: String, season: Int): List<CatalogueEpisodeEntity>

    @Query("SELECT * FROM catalogue_episodes WHERE id = :id LIMIT 1")
    suspend fun episode(id: String): CatalogueEpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(items: List<CatalogueEpisodeEntity>)

    @Update
    suspend fun updateEpisode(entity: CatalogueEpisodeEntity)

    @Query("DELETE FROM catalogue_episodes WHERE seriesId = :seriesId")
    suspend fun clearEpisodes(seriesId: String)

    @Query("UPDATE catalogue_series SET episodesSyncedAtUtcMs = :syncedAt WHERE id = :seriesId")
    suspend fun markEpisodesSynced(seriesId: String, syncedAt: Long)

    @Query("DELETE FROM catalogue_episodes")
    suspend fun clearAllEpisodes()

    @Query("DELETE FROM catalogue_meta")
    suspend fun clearMeta()

    @Transaction
    suspend fun replaceCatalogue(
        meta: CatalogueMetaEntity,
        movies: List<CatalogueMovieEntity>,
        series: List<CatalogueSeriesEntity>,
    ) {
        clearMovies()
        insertMovies(movies)
        clearSeries()
        insertSeries(series)
        clearAllEpisodes()
        upsertMeta(meta)
    }

    @Transaction
    suspend fun replaceEpisodes(
        seriesId: String,
        items: List<CatalogueEpisodeEntity>,
        syncedAt: Long,
    ) {
        clearEpisodes(seriesId)
        insertEpisodes(items)
        markEpisodesSynced(seriesId, syncedAt)
    }

    @Transaction
    suspend fun clearCatalogue() {
        clearMovies()
        clearSeries()
        clearAllEpisodes()
        clearMeta()
    }
}

/** The columns [CatalogueVariantIndex] needs to group movies, without loading their artwork. */
data class MovieVariantRow(
    val id: String,
    val titleKey: String?,
    val tmdbId: String?,
    val language: String?,
    val quality: String?,
    val addedAtUtcMs: Long,
    val groupKey: String?,
    val isDefault: Boolean,
)

data class MovieVariantUpdate(
    val id: String,
    val groupKey: String,
    val isDefault: Boolean,
)

/** TMDB enrichment for a movie, carried across a catalogue resync so it isn't matched again. */
data class MovieTmdbState(
    val id: String,
    val tmdbId: String?,
    val tmdbPosterUrl: String?,
    val tmdbBackdropUrl: String?,
    val tmdbOverview: String?,
    val tmdbRating: Double?,
    val tmdbGenres: String,
    val tmdbCast: String,
    val tmdbRuntime: Int?,
    val tmdbCertification: String?,
    val tmdbMatchedAtUtcMs: Long,
    val tmdbLanguage: String?,
)
