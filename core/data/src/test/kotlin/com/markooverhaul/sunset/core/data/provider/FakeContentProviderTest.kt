package com.markooverhaul.sunset.core.data.provider

import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.StreamRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FakeContentProviderTest {

    private val provider = FakeContentProvider()

    @Test
    fun `login rejects blank credentials`() = runTest {
        try {
            provider.login(" ", "")
            fail("Expected InvalidCredentials")
        } catch (expected: ProviderError.InvalidCredentials) {
            // expected
        }
    }

    @Test
    fun `login returns a demo session`() = runTest {
        val session = provider.login("demo", "demo")

        assertTrue(session.isDemo)
        assertEquals("demo", session.username)
        assertTrue(session.token.startsWith(FakeContentProvider.DEMO_TOKEN_PREFIX))
    }

    @Test
    fun `restoreSession rejects non demo tokens`() = runTest {
        try {
            provider.restoreSession("real-provider-token")
            fail("Expected SessionExpired")
        } catch (expected: ProviderError.SessionExpired) {
            // expected
        }
    }

    @Test
    fun `movie paging reports the last page`() = runTest {
        val first = provider.getMovies(categoryId = null, page = 1)
        val second = provider.getMovies(categoryId = null, page = 2)

        assertEquals(FakeContentProvider.PAGE_SIZE, first.items.size)
        assertTrue(first.hasMore)
        assertEquals(2, first.totalPages)
        assertFalse(second.hasMore)
        assertEquals(2, second.page)
        assertTrue(second.items.isNotEmpty())
    }

    @Test
    fun `movie category filter uses genre names`() = runTest {
        val drama = provider.getMovies(categoryId = "movie-drama", page = 1)

        assertTrue(drama.items.isNotEmpty())
        assertTrue(drama.items.all { movie -> "Drama" in movie.genres })
    }

    @Test
    fun `search ignores queries shorter than two characters`() = runTest {
        assertTrue(provider.search("a").isEmpty)
    }

    @Test
    fun `search finds live channels and series`() = runTest {
        val live = provider.search("sunset")
        val series = provider.search("harbour")

        assertTrue(live.live.any { it.name == "Sunset News" })
        assertTrue(series.series.any { it.title == "Harbour Lights" })
    }

    @Test
    fun `resolveStream returns the offline demo url for known content`() = runTest {
        val source = provider.resolveStream(
            StreamRequest(contentType = ContentType.MOVIE, contentId = "mv-amber-horizon"),
        )

        assertEquals(FakeContentProvider.DEMO_STREAM_URL, source.url)
    }

    @Test
    fun `resolveStream fails for unknown content`() = runTest {
        try {
            provider.resolveStream(
                StreamRequest(contentType = ContentType.MOVIE, contentId = "mv-does-not-exist"),
            )
            fail("Expected NotFound")
        } catch (expected: ProviderError.NotFound) {
            // expected
        }
    }

    @Test
    fun `guide covers every channel for the requested days`() = runTest {
        val oneDay = provider.getGuide(days = 1)
        val twoDays = provider.getGuide(days = 2)

        assertEquals(10, oneDay.map { it.channelId }.distinct().size)
        assertTrue(twoDays.size > oneDay.size)
        assertEquals(twoDays.size, twoDays.map { it.id }.distinct().size)
    }
}
