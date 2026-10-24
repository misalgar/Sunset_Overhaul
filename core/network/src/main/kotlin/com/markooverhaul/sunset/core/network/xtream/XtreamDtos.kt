package com.markooverhaul.sunset.core.network.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `player_api.php` without an action: account and server information. */
@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfo? = null,
)

@Serializable
data class XtreamUserInfo(
    val username: String = "",
    @Serializable(with = FlexibleInt::class) val auth: Int = 0,
    val status: String = "",
    @SerialName("exp_date") @Serializable(with = FlexibleString::class) val expDate: String = "",
    @SerialName("max_connections") @Serializable(with = FlexibleString::class) val maxConnections: String = "",
    @SerialName("allowed_output_formats")
    val allowedFormats: List<String> = emptyList(),
    @SerialName("is_trial") @Serializable(with = FlexibleString::class) val isTrial: String = "",
)

@Serializable
data class XtreamServerInfo(
    @SerialName("time_now") val timeNow: String = "",
    val timezone: String = "",
    @SerialName("url") val url: String = "",
    val port: String = "",
)

@Serializable
data class XtreamCategoryDto(
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val id: String = "",
    @SerialName("category_name") val name: String = "",
    @SerialName("parent_id") @Serializable(with = FlexibleInt::class) val parentId: Int = 0,
)

@Serializable
data class XtreamLiveStreamDto(
    @SerialName("num") @Serializable(with = FlexibleInt::class) val num: Int = 0,
    val name: String = "",
    @SerialName("stream_id") @Serializable(with = FlexibleString::class) val streamId: String = "",
    @SerialName("stream_icon") @Serializable(with = FlexibleString::class) val streamIcon: String = "",
    @SerialName("epg_channel_id") @Serializable(with = FlexibleString::class) val epgChannelId: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
    @SerialName("tv_archive") @Serializable(with = FlexibleInt::class) val tvArchive: Int = 0,
    @SerialName("tv_archive_duration")
    @Serializable(with = FlexibleInt::class) val tvArchiveDuration: Int = 0,
)

@Serializable
data class XtreamVodStreamDto(
    @SerialName("num") @Serializable(with = FlexibleInt::class) val num: Int = 0,
    val name: String = "",
    @SerialName("stream_id") @Serializable(with = FlexibleString::class) val streamId: String = "",
    @SerialName("stream_icon") @Serializable(with = FlexibleString::class) val streamIcon: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
    @Serializable(with = FlexibleString::class) val added: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
    @SerialName("container_extension")
    @Serializable(with = FlexibleString::class) val containerExtension: String = "",
)

@Serializable
data class XtreamSeriesDto(
    @SerialName("num") @Serializable(with = FlexibleInt::class) val num: Int = 0,
    val name: String = "",
    @SerialName("series_id") @Serializable(with = FlexibleString::class) val seriesId: String = "",
    @Serializable(with = FlexibleString::class) val cover: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    @SerialName("releaseDate") @Serializable(with = FlexibleString::class) val releaseDate: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
)

/** `get_vod_info` response: editorial metadata plus the playable data. */
@Serializable
data class XtreamVodInfoResponse(
    val info: XtreamVodInfo? = null,
    @SerialName("movie_data") val movieData: XtreamVodStreamDto? = null,
)

@Serializable
data class XtreamVodInfo(
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    @Serializable(with = FlexibleString::class) val duration: String = "",
    @SerialName("releasedate") @Serializable(with = FlexibleString::class) val releaseDate: String = "",
    @SerialName("movie_image") @Serializable(with = FlexibleString::class) val movieImage: String = "",
    @SerialName("backdrop_path") val backdrop: List<String> = emptyList(),
    @Serializable(with = FlexibleString::class) val rating: String = "",
)

/** `get_series_info` response: show metadata plus a season-number to episodes map. */
@Serializable
data class XtreamSeriesInfoResponse(
    val info: XtreamSeriesInfo? = null,
    val episodes: Map<String, List<XtreamEpisodeDto>> = emptyMap(),
)

@Serializable
data class XtreamSeriesInfo(
    val name: String = "",
    @Serializable(with = FlexibleString::class) val cover: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    @SerialName("releaseDate") @Serializable(with = FlexibleString::class) val releaseDate: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
    @SerialName("backdrop_path") val backdrop: List<String> = emptyList(),
)

@Serializable
data class XtreamEpisodeDto(
    @Serializable(with = FlexibleString::class) val id: String = "",
    @SerialName("episode_num") @Serializable(with = FlexibleInt::class) val episodeNum: Int = 0,
    val title: String = "",
    @SerialName("container_extension")
    @Serializable(with = FlexibleString::class) val containerExtension: String = "",
    val info: XtreamEpisodeInfo? = null,
)

@Serializable
data class XtreamEpisodeInfo(
    val plot: String = "",
    @Serializable(with = FlexibleString::class) val duration: String = "",
    @SerialName("releasedate") @Serializable(with = FlexibleString::class) val releaseDate: String = "",
    @SerialName("movie_image") @Serializable(with = FlexibleString::class) val image: String = "",
)

/** Credentials encoded into the session token; the token is encrypted at rest by the session store. */
@Serializable
data class XtreamCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)
