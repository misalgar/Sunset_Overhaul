package com.markooverhaul.sunset.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionPreferenceTest {

    private fun row(
        id: String,
        title: String = "Mayday",
        year: Int? = 2026,
        tmdb: String? = null,
        language: String? = null,
        quality: String? = null,
        added: Long = 0L,
    ) = VariantCandidate(
        id = id,
        titleKey = movieTitleKey(title, year),
        tmdbId = tmdb,
        language = language,
        quality = quality,
        addedAtUtcMs = added,
    )

    private fun List<VariantState>.defaultIds() = filter { it.isDefault }.map { it.id }.toSet()

    @Test
    fun `picks the preferred language as the default`() {
        val rows = listOf(
            row("sub", language = "SUB"),
            row("lat", language = "LAT"),
            row("esp", language = "ESP"),
        )

        assertEquals(setOf("lat"), assignVariantGroups(rows, VersionPreference.LATINO).defaultIds())
        assertEquals(setOf("esp"), assignVariantGroups(rows, VersionPreference.SPANISH).defaultIds())
        assertEquals(setOf("sub"), assignVariantGroups(rows, VersionPreference.SUBTITLED).defaultIds())
    }

    @Test
    fun `falls back to the next preferred language when the first is missing`() {
        val rows = listOf(row("esp", language = "ESP"), row("sub", language = "SUB"))

        assertEquals(setOf("esp"), assignVariantGroups(rows, VersionPreference.LATINO).defaultIds())
    }

    @Test
    fun `unknown languages rank after every preferred one`() {
        val rows = listOf(row("plain"), row("dual", language = "DUAL"))

        assertEquals(setOf("dual"), assignVariantGroups(rows, VersionPreference.LATINO).defaultIds())
    }

    @Test
    fun `breaks language ties by quality then newest`() {
        val rows = listOf(
            row("hd", language = "LAT", quality = "HD", added = 300),
            row("4k", language = "LAT", quality = "4K", added = 100),
            row("old", language = "LAT", quality = "4K", added = 50),
        )

        assertEquals(setOf("4k"), assignVariantGroups(rows, VersionPreference.LATINO).defaultIds())
    }

    @Test
    fun `variants of one title share a group and a single default`() {
        val rows = listOf(
            row("lat", language = "LAT", tmdb = "100"),
            row("sub", language = "SUB", tmdb = "100"),
        )

        val states = assignVariantGroups(rows, VersionPreference.LATINO)

        assertEquals(setOf("tmdb:100"), states.map { it.groupKey }.toSet())
        assertEquals(setOf("lat"), states.defaultIds())
    }

    @Test
    fun `an unmatched variant joins the group of its matched sibling`() {
        val rows = listOf(
            row("lat", language = "LAT", tmdb = "100"),
            row("sub", language = "SUB", tmdb = null),
        )

        val states = assignVariantGroups(rows, VersionPreference.LATINO)

        assertEquals(setOf("tmdb:100"), states.map { it.groupKey }.toSet())
    }

    @Test
    fun `different tmdb ids with the same title and year stay separate`() {
        val rows = listOf(
            row("remake", language = "LAT", tmdb = "100"),
            row("original", language = "LAT", tmdb = "200"),
            row("unknown", language = "SUB", tmdb = null),
        )

        val states = assignVariantGroups(rows, VersionPreference.LATINO).associateBy { it.id }

        assertEquals("tmdb:100", states.getValue("remake").groupKey)
        assertEquals("tmdb:200", states.getValue("original").groupKey)
        assertEquals(movieTitleKey("Mayday", 2026), states.getValue("unknown").groupKey)
        assertTrue(states.getValue("remake").isDefault)
        assertTrue(states.getValue("original").isDefault)
        assertTrue(states.getValue("unknown").isDefault)
    }

    @Test
    fun `titles with different years never merge`() {
        val rows = listOf(row("a", year = 2019), row("b", year = 2020))

        val states = assignVariantGroups(rows, VersionPreference.LATINO)

        assertEquals(2, states.map { it.groupKey }.toSet().size)
        assertEquals(setOf("a", "b"), states.defaultIds())
    }

    @Test
    fun `title keys ignore accents, case and punctuation`() {
        assertEquals(
            movieTitleKey("El Niño: The Film", 2020),
            movieTitleKey("el nino the film", 2020),
        )
    }

    @Test
    fun `legacy language aliases rank like their canonical tag`() {
        assertEquals(0, VersionPreference.LATINO.languageRank("LATINO"))
        assertEquals(2, VersionPreference.LATINO.languageRank("VOSE"))
        assertEquals(0, VersionPreference.LATINO.languageRank("SUB · LAT"))
    }

    @Test
    fun `quality rank orders 4K above FHD above HD`() {
        assertEquals(3, qualityRank("4K"))
        assertEquals(3, qualityRank("UHD"))
        assertEquals(2, qualityRank("FHD"))
        assertEquals(1, qualityRank("HD"))
        assertEquals(0, qualityRank(null))
    }
}
