package com.markooverhaul.sunset.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderVariantsTest {

    @Test
    fun `splits the language tags seen in the panel catalogue`() {
        assertEquals(
            ProviderVariant("Mayday (2026)", language = "SUB"),
            parseProviderVariant("Mayday (2026) SUB"),
        )
        assertEquals(
            ProviderVariant("Mayday (2026)", language = "LAT"),
            parseProviderVariant("Mayday (2026) LAT"),
        )
        assertEquals(
            ProviderVariant("The Runner (2026)", language = "SUB"),
            parseProviderVariant("The Runner (2026) SUB"),
        )
        assertEquals(
            ProviderVariant("The Front Runner (2018)", language = "DUAL"),
            parseProviderVariant("The Front Runner (2018) DUAL"),
        )
    }

    @Test
    fun `splits combined language and quality tags`() {
        assertEquals(
            ProviderVariant("Movie (2026)", language = "LAT", quality = "4K"),
            parseProviderVariant("Movie (2026) LAT 4K"),
        )
        assertEquals(
            ProviderVariant("Movie", language = "LAT", quality = "HD"),
            parseProviderVariant("Movie [LAT] (HD)"),
        )
    }

    @Test
    fun `keeps ordinary titles untouched`() {
        assertEquals(
            ProviderVariant("Live Soccer Domingo"),
            parseProviderVariant("Live Soccer Domingo"),
        )
        assertEquals(
            ProviderVariant("The Kite Runner", language = "LAT"),
            parseProviderVariant("The Kite Runner LAT"),
        )
        assertNull(parseProviderVariant("Normal Movie").language)
        assertNull(formatVariantLabel(null, null))
    }

    @Test
    fun `formats the badge label`() {
        assertEquals("LAT", formatVariantLabel("LAT", null))
        assertEquals("4K", formatVariantLabel(null, "4K"))
        assertEquals("LAT · 4K", formatVariantLabel("LAT", "4K"))
    }
}
