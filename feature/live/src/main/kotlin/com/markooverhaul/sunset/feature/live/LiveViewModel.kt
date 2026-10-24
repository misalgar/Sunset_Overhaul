package com.markooverhaul.sunset.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LiveProgramme(
    val now: EpgProgramme? = null,
    val next: EpgProgramme? = null,
)

data class LiveUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val channels: List<LiveChannel> = emptyList(),
    val programmes: Map<String, LiveProgramme> = emptyMap(),
    val favourites: Set<String> = emptySet(),
    val selectedCategoryId: String? = null,
    val favouritesOnly: Boolean = false,
    val error: Throwable? = null,
) {
    val visibleChannels: List<LiveChannel>
        get() {
            val byCategory = if (selectedCategoryId == null) {
                channels
            } else {
                channels.filter { selectedCategoryId in it.categoryIds }
            }
            return if (favouritesOnly) byCategory.filter { it.id in favourites } else byCategory
        }
}

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val provider: ContentProvider,
    private val favouritesRepository: FavouritesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(favourites = ids) }
            }
        }
        load()
    }

    fun selectCategory(categoryId: String?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun toggleFavouritesOnly() {
        _state.update { it.copy(favouritesOnly = !it.favouritesOnly) }
    }

    fun toggleFavourite(channel: LiveChannel) {
        viewModelScope.launch {
            favouritesRepository.toggle(channel.id, ContentType.LIVE)
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val payload = withContext(ioDispatcher) {
                    coroutineScope {
                        val categories = async { provider.getLiveCategories() }
                        val channels = async { provider.getLiveChannels() }
                        val guide = async { provider.getGuide(days = 1) }
                        Triple(categories.await(), channels.await(), guide.await())
                    }
                }
                val now = System.currentTimeMillis()
                // Group once; the guide can hold tens of thousands of programmes.
                val guideByChannel = payload.third.groupBy { it.channelId }
                val programmes = payload.second.associate { channel ->
                    val forChannel = guideByChannel[channel.id].orEmpty().sortedBy { it.startUtcMs }
                    channel.id to LiveProgramme(
                        now = forChannel.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs },
                        next = forChannel.firstOrNull { it.startUtcMs > now },
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        categories = payload.first,
                        channels = payload.second,
                        programmes = programmes,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }
}
