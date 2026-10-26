package com.markooverhaul.sunset.core.model

import java.util.Locale

/**
 * The panel publishes PPV as a dated live category (`PAY PER VIEW 24/10/2026`). It is a separate
 * destination in Sunset, not a Live TV filter.
 */
fun isPpvCategory(name: String): Boolean {
    val value = name.trim()
    return value.contains("PAY PER VIEW", ignoreCase = true) ||
        value.startsWith("PPV", ignoreCase = true) ||
        value.contains("PAY-PER-VIEW", ignoreCase = true)
}

/**
 * Provider names carry instructions such as
 * `MLB 26/10/26 (presiona boton update)` or `USA | ESPN (presiona para actualizar)`.
 * The parenthesised instruction is never part of the channel/category name.
 */
private val ProviderInstruction = Regex("""\(presiona.*?\)""", RegexOption.IGNORE_CASE)
private val RepeatedWhitespace = Regex("""\s+""")

/** Strips provider instructions and collapses the leftover whitespace. */
fun sanitizeProviderName(raw: String): String = raw
    .replace(ProviderInstruction, " ")
    .replace(RepeatedWhitespace, " ")
    .trim()

private val CategoryDate = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{2,4})\b""")
private val LiveSoccerCategory = Regex(
    """^\s*Live\s+Soccer\s+(.+)$""",
    RegexOption.IGNORE_CASE,
)

/**
 * Category names read like panel data (`NFL 25/10/26`). The sidebar shows the friendly form
 * (`NFL • Oct 25`) while the raw name stays available for schedule parsing and PPV detection.
 * `Live Soccer Domingo` becomes `Soccer • Domingo` so it fits the sidebar on one line.
 */
fun cleanCategoryDisplayName(raw: String): String {
    val months = java.text.DateFormatSymbols.getInstance().shortMonths
    val shortened = LiveSoccerCategory.replace(sanitizeProviderName(raw)) { match ->
        "Soccer • ${match.groupValues[1].trim()}"
    }
    return CategoryDate.replace(shortened) { match ->
        val day = match.groupValues[1].toIntOrNull()
        val month = match.groupValues[2].toIntOrNull()
        if (day == null || month == null || month !in 1..12 || day !in 1..31) {
            match.value
        } else {
            "• ${months[month - 1].trimEnd('.')} $day"
        }
    }.replace(RepeatedWhitespace, " ").trim()
}

/** Internal/admin categories that must never be offered as viewing choices. */
fun isInternalLiveCategory(raw: String): Boolean {
    val value = raw.trim()
    return value.contains("uso interno", ignoreCase = true) ||
        value.contains("banned", ignoreCase = true)
}

/**
 * Adult markers seen in the reference panel. Matching is a plain case-insensitive `contains`
 * because the panel mixes `+18`, `18+`, `ADULTOS`, `XXX`, `PLAYBOY`, `SEXMEX` and `VENUS`.
 * `Adult Swim` is a general-entertainment channel, not adult content.
 */
private val AdultMarkers = listOf(
    "+18",
    "18+",
    "adultos",
    "adult",
    "xxx",
    "playboy",
    "sexmex",
    "venus",
)

fun isAdultName(raw: String): Boolean {
    val value = raw.trim().lowercase(Locale.US)
    if (value.isEmpty()) return false
    if (value.contains("adult swim")) return false
    return AdultMarkers.any { value.contains(it) }
}

/** True when any of the channel's provider categories is an adult category. */
fun isAdultChannel(name: String, categoryNames: Iterable<String?>): Boolean =
    isAdultName(name) || categoryNames.any { it != null && isAdultName(it) }

/** Sidebar grouping: pinned filters are UI-level; provider categories fall into these buckets. */
enum class LiveCategoryGroup { LiveEvents, Genres, Countries }

private val EventDate = Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b""")
private val EventKeywords = listOf(
    "mlb",
    "nfl",
    "nba",
    "nhl",
    "lmp",
    "soccer",
    "futbol",
    "dazn",
    "la granja",
    "movistar",
    "ppv",
    "pay per view",
    "ufc",
    "wwe",
    "boxeo",
    "boxing",
    "formula 1",
    "nascar",
    "mls",
    "ncaa",
    "red zone",
)

/** Normalised country/region category names: provider-specific aliases are included. */
private val CountryCategoryNames = setOf(
    "tv mexico",
    "mexico",
    "puerto rico",
    "bolivia",
    "chile",
    "costa rica",
    "argentina",
    "cuba",
    "colombia",
    "ecuador",
    "guatemala",
    "honduras",
    "nicaragua",
    "uruguay",
    "republica dominicana",
    "panama",
    "el salvador",
    "paraguay",
    "peru",
    "venezuela",
    "canada",
    "italy",
    "italia",
    "espana",
    "usa",
    "us",
    "united states",
    "estados unidos",
    "networks usa",
    "abc usa",
    "galavisiones",
    "telemundos",
    "unimas",
    "univisiones",
    "internacionales",
)

private fun normalizeCategoryName(raw: String): String =
    java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .replace(RepeatedWhitespace, " ")
        .trim()

/**
 * Classifies a cleaned provider category so daily sports feeds do not push standard genres to the
 * bottom of the sidebar. Event feeds are matched before countries so `NFL 25/10/26` never falls
 * through; `Deportes` and `Sports` stay in Genres.
 */
fun liveCategoryGroup(raw: String): LiveCategoryGroup {
    val name = normalizeCategoryName(raw)
    if (EventDate.containsMatchIn(name) || EventKeywords.any { name.contains(it) }) {
        return LiveCategoryGroup.LiveEvents
    }
    if (name in CountryCategoryNames) return LiveCategoryGroup.Countries
    return LiveCategoryGroup.Genres
}
