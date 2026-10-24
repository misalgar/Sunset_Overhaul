package com.markooverhaul.sunset.core.network.tmdb

import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import com.markooverhaul.sunset.core.network.xtream.XtreamApi

data class TmdbCredentials(
    val readToken: String?,
    val apiKey: String?,
)

/**
 * Minimal TMDB v3 client used to enrich provider metadata. All failures are soft: the caller keeps
 * the provider values when TMDB is unavailable, unconfigured or rate-limited.
 */
@Singleton
class TmdbApi @Inject constructor(
    private val credentials: TmdbCredentials,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean =
        !credentials.readToken.isNullOrBlank() || !credentials.apiKey.isNullOrBlank()

    val language: String =
        if (Locale.getDefault().language.equals("es", ignoreCase = true)) "es-MX" else "en-US"

    suspend fun searchMovie(title: String, year: Int?): List<TmdbSearchItem> =
        search("search/movie", title, year, "year")

    suspend fun searchSeries(title: String, year: Int?): List<TmdbSearchItem> =
        search("search/tv", title, year, "first_air_date_year")

    suspend fun movieDetail(id: Long): TmdbMovieDetail? =
        get("movie/$id", mapOf("append_to_response" to "credits,release_dates"))

    suspend fun seriesDetail(id: Long): TmdbTvDetail? =
        get("tv/$id", mapOf("append_to_response" to "credits,content_ratings"))

    suspend fun seasonDetail(seriesId: Long, season: Int): TmdbSeasonDetail? =
        get("tv/$seriesId/season/$season", emptyMap())

    fun posterUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w500$it" }

    fun backdropUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w780$it" }

    fun stillUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w300$it" }

    private suspend fun search(
        path: String,
        title: String,
        year: Int?,
        yearParam: String,
    ): List<TmdbSearchItem> = withContext(Dispatchers.IO) {
        val params = buildMap {
            put("query", title)
            put("include_adult", "false")
            if (year != null && year > 1900) put(yearParam, year.toString())
        }
        get<TmdbSearchResponse>(path, params)?.results.orEmpty()
    }

    private suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String>,
    ): T? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val builder = "https://api.themoviedb.org/3/"
            .toHttpUrl()
            .newBuilder()
            .addPathSegments(path.trimStart('/'))
            .addQueryParameter("language", language)
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        if (credentials.readToken.isNullOrBlank()) {
            builder.addQueryParameter("api_key", credentials.apiKey.orEmpty())
        }
        val request = Request.Builder()
            .url(builder.build())
            .header("Accept", "application/json")
            .apply {
                if (!credentials.readToken.isNullOrBlank()) {
                    header("Authorization", "Bearer ${credentials.readToken}")
                }
            }
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: return@withContext null
                runCatching { XtreamApi.json.decodeFromString<T>(body) }.getOrNull()
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun certification(releaseDates: TmdbReleaseDates?): String? {
        val results = releaseDates?.results.orEmpty()
        val preferred = listOf("US", "ES", "MX")
        for (country in preferred) {
            val value = results
                .firstOrNull { it.country == country }
                ?.dates
                ?.firstOrNull { it.certification.isNotBlank() }
                ?.certification
            if (!value.isNullOrBlank()) return value
        }
        return results.firstNotNullOfOrNull { country ->
            country.dates.firstOrNull { it.certification.isNotBlank() }?.certification
        }
    }

    fun certification(contentRatings: TmdbContentRatings?): String? {
        val results = contentRatings?.results.orEmpty()
        for (country in listOf("US", "ES", "MX")) {
            val value = results.firstOrNull { it.country == country }?.rating
            if (!value.isNullOrBlank()) return value
        }
        return results.firstOrNull { it.rating.isNotBlank() }?.rating
    }
}

@Serializable
data class TmdbSearchResponse(
    val results: List<TmdbSearchItem> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class TmdbSearchItem(
    val id: Long = 0,
    val title: String = "",
    val name: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("first_air_date") val firstAirDate: String = "",
    val popularity: Double = 0.0,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("poster_path") val posterPath: String? = null,
) {
    val displayTitle: String get() = title.ifBlank { name }
    val displayOriginal: String get() = originalTitle.ifBlank { originalName }
    val year: Int? get() = (releaseDate.ifBlank { firstAirDate }).take(4).toIntOrNull()
}

@Serializable
data class TmdbMovieDetail(
    val id: Long = 0,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String = "",
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    @SerialName("release_dates") val releaseDates: TmdbReleaseDates? = null,
)

@Serializable
data class TmdbTvDetail(
    val id: Long = 0,
    val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("first_air_date") val firstAirDate: String = "",
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    @SerialName("content_ratings") val contentRatings: TmdbContentRatings? = null,
)

@Serializable
data class TmdbGenre(val name: String = "")

@Serializable
data class TmdbCredits(
    val cast: List<TmdbCast> = emptyList(),
    val crew: List<TmdbCrew> = emptyList(),
)

@Serializable
data class TmdbCast(
    val name: String = "",
    val order: Int = 0,
)

@Serializable
data class TmdbCrew(
    val name: String = "",
    val job: String = "",
)

@Serializable
data class TmdbReleaseDates(
    val results: List<TmdbReleaseCountry> = emptyList(),
)

@Serializable
data class TmdbReleaseCountry(
    @SerialName("iso_3166_1") val country: String = "",
    @SerialName("release_dates") val dates: List<TmdbReleaseDate> = emptyList(),
)

@Serializable
data class TmdbReleaseDate(val certification: String = "")

@Serializable
data class TmdbContentRatings(
    val results: List<TmdbContentRating> = emptyList(),
)

@Serializable
data class TmdbContentRating(
    @SerialName("iso_3166_1") val country: String = "",
    val rating: String = "",
)

@Serializable
data class TmdbSeasonDetail(
    val episodes: List<TmdbEpisode> = emptyList(),
)

@Serializable
data class TmdbEpisode(
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val name: String = "",
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
)
