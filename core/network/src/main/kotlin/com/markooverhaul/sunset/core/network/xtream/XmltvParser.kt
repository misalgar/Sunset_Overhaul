package com.markooverhaul.sunset.core.network.xtream

import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Streaming-safe XMLTV reader.
 *
 * `xmltv.php` on the reference panel is ~3.2 MB. The parser walks the document once and keeps only
 * the fields the guide needs, so no DOM is built and the whole file is never materialised as
 * objects beyond the programme list.
 */
object XmltvParser {

    data class Programme(
        val channelId: String,
        val title: String,
        val description: String?,
        val startUtcMs: Long,
        val endUtcMs: Long,
        val category: String?,
        val rating: String?,
        val iconUrl: String?,
    )

    fun parse(xml: String): List<Programme> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        val programmes = ArrayList<Programme>(4096)

        var channelId: String? = null
        var start: Long? = null
        var stop: Long? = null
        var title: String? = null
        var description: String? = null
        var category: String? = null
        var rating: String? = null
        var iconUrl: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "programme" -> {
                        channelId = parser.getAttributeValue(null, "channel")
                        start = parseXmltvTime(parser.getAttributeValue(null, "start"))
                        stop = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                        title = null
                        description = null
                        category = null
                        rating = null
                        iconUrl = null
                    }

                    "title" -> title = parser.readText()
                    "desc" -> description = parser.readText()
                    "category" -> category = parser.readText()
                    "value" -> rating = parser.readText()
                    "icon" -> if (iconUrl.isNullOrBlank()) {
                        iconUrl = parser.getAttributeValue(null, "src")
                    }
                }

                XmlPullParser.END_TAG -> if (parser.name.equals("programme", ignoreCase = true)) {
                    val programmeTitle = title?.trim().orEmpty()
                    val from = start
                    val to = stop
                    val channel = channelId
                    if (channel != null && programmeTitle.isNotEmpty() && from != null && to != null && to > from) {
                        programmes += Programme(
                            channelId = channel,
                            title = programmeTitle,
                            description = description?.trim()?.takeIf { it.isNotEmpty() },
                            startUtcMs = from,
                            endUtcMs = to,
                            category = category?.trim()?.takeIf { it.isNotEmpty() },
                            rating = rating?.trim()?.takeIf { it.isNotEmpty() },
                            iconUrl = iconUrl?.trim()?.takeIf { it.isNotEmpty() },
                        )
                    }
                    channelId = null
                    start = null
                    stop = null
                }
            }
            event = parser.next()
        }
        return programmes
    }

    /** Reads an element's text and leaves the parser on its END_TAG. */
    private fun XmlPullParser.readText(): String? = runCatching { nextText() }.getOrNull()

    /**
     * XMLTV times are `yyyyMMddHHmmss +HHMM` (the offset is part of the value). Feeds that omit the
     * offset are treated as UTC.
     */
    fun parseXmltvTime(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.length < 14) return null
        val body = value.substring(0, 14)
        val zone = value.substring(14).trim()
        if (!body.all { it.isDigit() }) return null
        val input = if (zone.isEmpty()) body else "$body $zone"
        val pattern = if (zone.isEmpty()) "yyyyMMddHHmmss" else "yyyyMMddHHmmss Z"
        val format = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = if (zone.isEmpty()) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        }
        return runCatching { format.parse(input)?.time }.getOrNull()
    }
}
