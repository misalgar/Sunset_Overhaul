package com.markooverhaul.sunset.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCategoriesTest {

    @Test
    fun `strips the provider update instruction from category names`() {
        assertEquals(
            "MLB 26/10/26",
            sanitizeProviderName("MLB 26/10/26 (presiona boton update)"),
        )
        assertEquals(
            "USA | ESPN",
            sanitizeProviderName("USA | ESPN (PRESIONA PARA ACTUALIZAR)"),
        )
        assertEquals(
            "Deportes",
            sanitizeProviderName("Deportes (presiona una vez) (presiona otra vez)"),
        )
    }

    @Test
    fun `leaves normal names untouched`() {
        assertEquals("Deportes", sanitizeProviderName("Deportes"))
        assertEquals("Peliculas", sanitizeProviderName("  Peliculas  "))
    }

    @Test
    fun `detects internal and banned categories`() {
        assertFalse(isInternalLiveCategory("Deportes"))
        assertTrue(isInternalLiveCategory("USO INTERNO"))
        assertTrue(isInternalLiveCategory("BANNED channels"))
    }

    @Test
    fun `detects adult markers without hiding Adult Swim`() {
        assertTrue(isAdultName("ADULTOS"))
        assertTrue(isAdultName("Playboy TV"))
        assertTrue(isAdultName("USA | VENUS"))
        assertTrue(isAdultName("XXX 18+"))
        assertTrue(isAdultName("SexMex"))
        assertFalse(isAdultName("Adult Swim"))
        assertFalse(isAdultName("ESPN"))
        assertFalse(isAdultName(""))
    }

    @Test
    fun `a channel inherits the adult flag from its category`() {
        assertTrue(isAdultChannel("Canal 1", listOf("ADULTOS")))
        assertFalse(isAdultChannel("Canal 1", listOf("Deportes", null)))
        assertTrue(isAdultChannel("Venus TV", emptyList()))
    }

    @Test
    fun `still recognises the dated PPV category`() {
        assertTrue(isPpvCategory("PAY PER VIEW 24/10/2026"))
        assertTrue(isPpvCategory("PPV Eventos"))
        assertFalse(isPpvCategory("Deportes"))
    }

    @Test
    fun `turns category dates into readable labels`() {
        val october = java.text.DateFormatSymbols.getInstance()
            .shortMonths[9]
            .trimEnd('.')
        assertEquals("NFL • $october 25", cleanCategoryDisplayName("NFL 25/10/26"))
        assertEquals("MLB • $october 26", cleanCategoryDisplayName(" MLB 26/10/2026 "))
        assertEquals(
            "NFL • $october 25",
            cleanCategoryDisplayName("NFL 25/10/26 (presiona boton update)"),
        )
        assertEquals("Soccer • Domingo", cleanCategoryDisplayName("Live Soccer Domingo"))
        assertEquals("Soccer • Lunes", cleanCategoryDisplayName("Live Soccer Lunes"))
        assertEquals("Canales 24/7", cleanCategoryDisplayName("Canales 24/7"))
    }

    @Test
    fun `groups daily sports feeds as live events`() {
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("MLB 26/10/26"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("NFL 25/10/26"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("NBA 25/10/26"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("NHL 25/10/26"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("LMP 25/10/26"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("Live Soccer Domingo"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("DAZN"))
        assertEquals(LiveCategoryGroup.LiveEvents, liveCategoryGroup("La Granja Vip"))
    }

    @Test
    fun `groups countries and regions`() {
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("TV Mexico"))
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("Puerto Rico"))
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("España"))
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("ITALY"))
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("ABC /USA"))
        assertEquals(LiveCategoryGroup.Countries, liveCategoryGroup("Networks USA"))
    }

    @Test
    fun `standard and sports-pack categories stay genres`() {
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Deportes"))
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Sports"))
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Cultura"))
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Noticias"))
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Canales 24/7"))
        assertEquals(LiveCategoryGroup.Genres, liveCategoryGroup("Cine"))
    }
}
