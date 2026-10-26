package com.markooverhaul.sunset.core.model

import java.text.Normalizer
import java.util.Locale

/**
 * The viewer's preferred version of a title. Each preset lists the provider language tags, best
 * first; DUAL (both audio tracks) always comes last.
 */
enum class VersionPreference(val languageOrder: List<String>) {
    LATINO(listOf("LAT", "ESP", "SUB", "DUAL")),
    SPANISH(listOf("ESP", "LAT", "SUB", "DUAL")),
    SUBTITLED(listOf("SUB", "LAT", "ESP", "DUAL")),
}

/** One playable version of a movie, shown as a chip on the details screen. */
data class MovieVariant(
    val id: String,
    val language: String? = null,
    val quality: String? = null,
) {
    val label: String? get() = formatVariantLabel(language, quality)
}

/** The catalogue fields that decide grouping and the default version. */
data class VariantCandidate(
    val id: String,
    val titleKey: String,
    val tmdbId: String?,
    val language: String?,
    val quality: String?,
    val addedAtUtcMs: Long,
)

data class VariantState(
    val id: String,
    val groupKey: String,
    val isDefault: Boolean,
)

private const val TMDB_GROUP_PREFIX = "tmdb:"

/** Normalised title plus year. Rows with the same key are candidates for one title. */
fun movieTitleKey(title: String, year: Int?): String =
    "${normalizeTitleForMatch(title)}|${year ?: ""}"

/**
 * Assigns every row to a group and marks one default per group.
 *
 * Rows that share a title key form a bucket. When the bucket has exactly one TMDB id, all of its
 * rows (including ones TMDB has not matched yet) join that TMDB group, so the grouping does not
 * depend on which variant happened to be opened first. When the TMDB ids disagree, the bucket
 * holds different films: matched rows split by TMDB id and unmatched rows stay on their own.
 */
fun assignVariantGroups(
    candidates: List<VariantCandidate>,
    preference: VersionPreference,
): List<VariantState> {
    val groupKeyById = HashMap<String, String>(candidates.size)
    for (bucket in candidates.groupBy { it.titleKey }.values) {
        val tmdbIds = bucket.mapNotNullTo(mutableSetOf()) { it.tmdbId }
        for (row in bucket) {
            groupKeyById[row.id] = when {
                tmdbIds.size == 1 -> "$TMDB_GROUP_PREFIX${tmdbIds.first()}"
                row.tmdbId != null -> "$TMDB_GROUP_PREFIX${row.tmdbId}"
                else -> row.titleKey
            }
        }
    }
    val order = preference.variantOrder()
    val defaults = candidates
        .groupBy { groupKeyById.getValue(it.id) }
        .values
        .mapTo(HashSet(candidates.size)) { group -> group.minWith(order).id }
    return candidates.map { row ->
        VariantState(
            id = row.id,
            groupKey = groupKeyById.getValue(row.id),
            isDefault = row.id in defaults,
        )
    }
}

/** Best version first: preferred language, then higher quality, then newest, then id. */
fun VersionPreference.variantOrder(): Comparator<VariantCandidate> =
    compareBy<VariantCandidate>(
        { languageRank(it.language) },
        { -qualityRank(it.quality) },
        { -it.addedAtUtcMs },
        { it.id },
    )

/** Lower is better. A title with no recognised language ranks after every preferred language. */
fun VersionPreference.languageRank(language: String?): Int {
    val fallback = languageOrder.size
    return language.orEmpty()
        .split(" · ")
        .map { tag ->
            val index = languageOrder.indexOf(canonicalLanguageTag(tag))
            if (index < 0) fallback else index
        }
        .minOrNull() ?: fallback
}

/** Higher is better: 4K/UHD 3, FHD 2, HD 1, anything else 0. */
fun qualityRank(quality: String?): Int {
    val value = quality.orEmpty().uppercase(Locale.US)
    return when {
        "4K" in value || "UHD" in value -> 3
        "FHD" in value || "1080P" in value -> 2
        "HD" in value || "720P" in value -> 1
        else -> 0
    }
}

private fun canonicalLanguageTag(tag: String): String =
    when (val upper = tag.trim().uppercase(Locale.US)) {
        "LATINO" -> "LAT"
        "VOSE", "VOS" -> "SUB"
        else -> upper
    }

/**
 * Lowercase, accent-free words with articles and common stop words removed, so the same title
 * written with different punctuation or accents produces the same key.
 */
fun normalizeTitleForMatch(value: String): String {
    val tokens = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .split(' ')
        .filter { it.isNotBlank() && it !in TITLE_STOP_WORDS }
    return tokens.joinToString(" ")
}

private val TITLE_STOP_WORDS = setOf(
    "and", "the", "of", "a", "an", "y", "e", "de", "del", "la", "el",
    "los", "las", "le", "les", "et", "und",
)
