package com.markooverhaul.sunset.feature.live

import com.markooverhaul.sunset.core.model.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveUiStateTest {

    private fun channel(
        id: String,
        categoryId: String,
        adult: Boolean = false,
    ) = LiveChannel(
        id = id,
        name = "Channel $id",
        number = id.toInt(),
        categoryIds = listOf(categoryId),
        isAdult = adult,
    )

    private val state = LiveUiState(
        isLoading = false,
        categories = listOf(
            LiveSidebarCategory(id = "sports", name = "Deportes", count = 2),
        ),
        channels = listOf(
            channel("1", "sports"),
            channel("2", "sports"),
            channel("3", "adult", adult = true),
        ),
        favourites = setOf("1", "3"),
    )

    @Test
    fun `all channels excludes adult entries`() {
        val visible = state.copy(selectedFilter = LiveFilter.All).visibleChannels
        assertEquals(listOf("1", "2"), visible.map { it.id })
    }

    @Test
    fun `favourites never expose adult entries`() {
        val visible = state.copy(selectedFilter = LiveFilter.Favourites).visibleChannels
        assertEquals(listOf("1"), visible.map { it.id })
    }

    @Test
    fun `the adult row is the only way to see adult channels`() {
        val visible = state.copy(selectedFilter = LiveFilter.Adult).visibleChannels
        assertEquals(listOf("3"), visible.map { it.id })
    }

    @Test
    fun `category filters exclude adult channels`() {
        val visible = state.copy(
            selectedFilter = LiveFilter.Category("sports"),
        ).visibleChannels
        assertEquals(listOf("1", "2"), visible.map { it.id })
    }

    @Test
    fun `counts exclude adult channels except the adult total`() {
        assertEquals(2, state.allChannelCount)
        assertEquals(1, state.favouriteChannelCount)
        assertEquals(1, state.adultChannelCount)
    }
}
