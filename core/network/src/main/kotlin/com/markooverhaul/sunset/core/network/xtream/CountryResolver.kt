package com.markooverhaul.sunset.core.network.xtream

import java.text.Normalizer
import java.util.Locale

/**
 * The panel encodes the country at the start of many channel names (`USA | Travel Channel`,
 * `CAN| A&E HD`). Some leading tokens are networks or leagues (`NCAA`, `NBA`, `TNT`), so only a
 * curated alias set is treated as a country. Country-named categories are a fallback for channels
 * whose name has no prefix.
 */
object CountryResolver {

    data class Resolved(val name: String, val countryCode: String?)

    private val prefix = Regex("""^\s*([A-Za-z]{2,5})\s*[|:/\-]\s*""")

    /** Tokens observed on the reference panel; aliases are non-standard and panel-specific. */
    private val tokenAliases = mapOf(
        "USA" to "US",
        "US" to "US",
        "MX" to "MX",
        "PR" to "PR",
        "AR" to "AR",
        "ARG" to "AR",
        "PE" to "PE",
        "CL" to "CL",
        "CH" to "CL",
        "CO" to "CO",
        "PY" to "PY",
        "HN" to "HN",
        "HON" to "HN",
        "EC" to "EC",
        "CR" to "CR",
        "SV" to "SV",
        "PA" to "PA",
        "GT" to "GT",
        "URU" to "UY",
        "CU" to "CU",
        "VE" to "VE",
        "BO" to "BO",
        "ITA" to "IT",
        "CAN" to "CA",
        "CA" to "CA",
        "RD" to "DO",
        "ES" to "ES",
        "NC" to "NI",
        "UNI" to "US",
    )

    private val categoryAliases = mapOf(
        "tv mexico" to "MX",
        "puerto rico" to "PR",
        "bolivia" to "BO",
        "chile" to "CL",
        "costa rica" to "CR",
        "argentina" to "AR",
        "cuba" to "CU",
        "colombia" to "CO",
        "ecuador" to "EC",
        "guatemala" to "GT",
        "honduras" to "HN",
        "nicaragua" to "NI",
        "uruguay" to "UY",
        "republica dominicana" to "DO",
        "panama" to "PA",
        "el salvador" to "SV",
        "paraguay" to "PY",
        "peru" to "PE",
        "venezuela" to "VE",
        "canada" to "CA",
        "italy" to "IT",
        "espana" to "ES",
        "networks usa" to "US",
        "abc /usa" to "US",
        "galavisiones" to "US",
        "telemundos" to "US",
        "unimas" to "US",
        "univisiones" to "US",
    )

    fun resolve(rawName: String, categoryName: String?): Resolved {
        val match = prefix.find(rawName)
        val token = match?.groupValues?.get(1)?.uppercase(Locale.US)
        val code = token?.let { tokenAliases[it] } ?: categoryName?.let {
            categoryAliases[normalize(it)]
        }
        val cleaned = if (match != null && code != null) {
            rawName.removeRange(match.range).trim().ifBlank { rawName }
        } else {
            rawName.trim()
        }
        return Resolved(name = cleaned, countryCode = code)
    }

    fun displayName(code: String): String =
        Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.US)
            .trim()
}
