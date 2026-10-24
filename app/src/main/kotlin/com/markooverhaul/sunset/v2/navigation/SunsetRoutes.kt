package com.markooverhaul.sunset.v2.navigation

import android.net.Uri
import com.markooverhaul.sunset.core.model.ContentType

/** Navigation contracts shared by the shell and the feature callbacks. */
object SunsetRoutes {
    const val HOME = "home"
    const val LIVE = "live"
    const val PPV = "ppv"
    const val GUIDE = "guide"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val MY_LIST = "mylist"
    const val MORE = "more"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PARENTAL = "parental"
    const val PROFILES = "profiles"
    const val LOGIN = "login"

    /** Route patterns registered with [androidx.navigation.NavHost]. */
    const val SEARCH_PATTERN = "search?q={q}"
    const val DETAILS_PATTERN = "details/{type}/{id}"
    const val PLAYER_PATTERN =
        "player?type={type}&id={id}&episodeId={episodeId}&title={title}&episodeTitle={episodeTitle}&live={live}"

    /** Deep-link prefixes; the manifest accepts `sunset://open/...`. */
    const val DEEP_LINK_DETAILS = "sunset://open/details/{type}/{id}"
    const val DEEP_LINK_SEARCH = "sunset://open/search?q={q}"

    fun search(query: String? = null): String = "search?q=${Uri.encode(query.orEmpty())}"

    fun details(type: ContentType, id: String): String =
        "details/${type.name}/${Uri.encode(id)}"

    fun player(
        type: ContentType,
        id: String,
        title: String,
        episodeId: String? = null,
        episodeTitle: String? = null,
        live: Boolean = false,
    ): String = buildString {
        append("player?type=").append(type.name)
        append("&id=").append(Uri.encode(id))
        append("&episodeId=").append(Uri.encode(episodeId.orEmpty()))
        append("&title=").append(Uri.encode(title))
        append("&episodeTitle=").append(Uri.encode(episodeTitle.orEmpty()))
        append("&live=").append(live)
    }
}
