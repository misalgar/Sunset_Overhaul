package com.markooverhaul.sunset.core.network

/**
 * Endpoint paths observed in the legacy APK (spec Appendix B).
 *
 * These are the ONLY known API facts. There is no base URL, no request/response schema, no auth
 * header format and no pagination contract. Do not invent them. This object exists so the future
 * adapter has a single, reviewable list; none of these values may be called until provider access
 * is explicitly approved.
 */
object EzServerEndpoints {
    const val CREATE_TOKEN = "/token/createtoken?userid="
    const val LIVE_CATEGORIES = "/server/get_channel_category?token="
    const val LIVE_CHANNELS = "/server/get_channel_list?token="
    const val EPG = "/server/get_epg_info?token="
    const val MOVIE_CATEGORIES = "/server/get_movie_category?token="
    const val MOVIES = "/server/get_movie_list?token="
    const val SERIES_CATEGORIES = "/server/get_series_category?token="
    const val SERIES = "/server/get_series?token="
    const val SERIES_PROFILE = "/server/get_series_profile?token="
    const val SERIES_EPISODES = "/server/get_series_episode?token="
    const val USER_DAYS_LEFT = "/server/query_user_days_left?token="
    const val SERVER_HTTP_PORT = "/server/inquery_server_httpport?token="
}

/**
 * Provider configuration is intentionally empty.
 *
 * Populate this only from an approved, non-production configuration (test line or written API
 * documentation). Never commit a production URL, credentials or tokens. When [baseUrl] is null the
 * provider reports [com.markooverhaul.sunset.core.model.ProviderStatus.NotConfigured].
 */
data class EzServerConfig(
    val baseUrl: String? = null,
    val allowCleartextHosts: List<String> = emptyList(),
)
