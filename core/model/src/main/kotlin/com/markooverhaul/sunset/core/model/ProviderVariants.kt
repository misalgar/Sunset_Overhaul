package com.markooverhaul.sunset.core.model

import java.util.Locale

/**
 * The Xtream list endpoints have no language or quality field. The panel appends tokens such as
 * `SUB`, `LAT`, `DUAL`, `ESP` and `4K` to the title instead, e.g. `Mayday (2026) LAT`.
 */
data class ProviderVariant(
    val title: String,
    val language: String? = null,
    val quality: String? = null,
)

private val LanguageTags = setOf(
    "LAT",
    "LATINO",
    "SUB",
    "DUB",
    "DUAL",
    "ESP",
    "ING",
    "CAST",
    "VOSE",
    "VOS",
)

private val QualityTags = setOf(
    "HD",
    "FHD",
    "UHD",
    "4K",
    "SD",
    "1080P",
    "720P",
)

private val TokenPunctuation = charArrayOf('[', ']', '(', ')', '{', '}', '.', ',', '-', '–', '—')

/**
 * Splits trailing language/quality tags from a provider title. Tags may be repeated or combined
 * (`Movie LAT 4K`), and may be wrapped in brackets (`Movie (2026) [LAT]`). Anything before the
 * trailing tag run is preserved verbatim.
 */
fun parseProviderVariant(rawName: String): ProviderVariant {
    val tokens = rawName.trim().split(Regex("""\s+""")).toMutableList()
    if (tokens.isEmpty()) return ProviderVariant(rawName.trim())

    val languages = mutableListOf<String>()
    val qualities = mutableListOf<String>()

    while (tokens.isNotEmpty()) {
        val cleaned = tokens.last()
            .trim(*TokenPunctuation)
            .uppercase(Locale.US)
        val parsed = when {
            cleaned in LanguageTags -> {
                if (cleaned !in languages) languages.add(0, cleaned)
                true
            }

            cleaned in QualityTags -> {
                if (cleaned !in qualities) qualities.add(0, cleaned)
                true
            }

            else -> false
        }
        if (!parsed) break
        tokens.removeAt(tokens.lastIndex)
    }

    return ProviderVariant(
        title = tokens.joinToString(" ").trim().ifBlank { rawName.trim() },
        language = languages.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        quality = qualities.takeIf { it.isNotEmpty() }?.joinToString(" · "),
    )
}

/** Badge text shown on cards/details: `LAT`, `4K`, or `LAT · 4K`. */
fun formatVariantLabel(language: String?, quality: String?): String? {
    val parts = listOfNotNull(
        language?.trim()?.takeIf { it.isNotEmpty() },
        quality?.trim()?.takeIf { it.isNotEmpty() },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
