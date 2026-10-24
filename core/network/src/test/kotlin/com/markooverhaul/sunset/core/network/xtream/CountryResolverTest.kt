package com.markooverhaul.sunset.core.network.xtream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryResolverTest {

    @Test
    fun `strips a known country prefix and keeps the channel name`() {
        val resolved = CountryResolver.resolve("USA | Travel Channel", "Entertainment")
        assertEquals("Travel Channel", resolved.name)
        assertEquals("US", resolved.countryCode)
    }

    @Test
    fun `handles prefixes without spaces and non-standard aliases`() {
        assertEquals("CA", CountryResolver.resolve("CAN| A&E HD", null).countryCode)
        assertEquals("UY", CountryResolver.resolve("URU | VTV", null).countryCode)
        assertEquals("CL", CountryResolver.resolve("CH | TVN", null).countryCode)
        assertEquals("NI", CountryResolver.resolve("NC / Canal 6", null).countryCode)
    }

    @Test
    fun `does not strip network or league tokens`() {
        val resolved = CountryResolver.resolve("TNT-NVELAS", null)
        assertEquals("TNT-NVELAS", resolved.name)
        assertNull(resolved.countryCode)

        val league = CountryResolver.resolve("NCAA FB", null)
        assertEquals("NCAA FB", league.name)
        assertNull(league.countryCode)
    }

    @Test
    fun `falls back to a country-named category`() {
        val resolved = CountryResolver.resolve("Canal 4", "Puerto Rico")
        assertEquals("Canal 4", resolved.name)
        assertEquals("PR", resolved.countryCode)
    }

    @Test
    fun `matches accented category names`() {
        assertEquals("ES", CountryResolver.resolve("Antena 3", "España").countryCode)
    }
}
