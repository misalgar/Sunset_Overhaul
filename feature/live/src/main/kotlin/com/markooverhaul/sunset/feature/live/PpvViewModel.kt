package com.markooverhaul.sunset.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.LivePlaybackQueue
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.isPpvCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PpvUiState(
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

    /** The provider's dated category name, e.g. `PAY PER VIEW 24/10/2026`. */
    val scheduleLabel: String?
        get() = categories
            .firstOrNull { selectedCategoryId == null || it.id == selectedCategoryId }
            ?.name
}

@HiltViewModel
class PpvViewModel @Inject constructor(
    private val provider: ContentProvider,
    private val favouritesRepository: FavouritesRepository,
    private val liveQueue: LivePlaybackQueue,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(PpvUiState())
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

    /** PPV zaps through the event list the viewer picked from, not the whole Live TV lineup. */
    fun onChannelPlayed(channel: LiveChannel) {
        liveQueue.setQueue(_state.value.visibleChannels.map { it.id }, _state.value.selectedCategoryId)
        liveQueue.markPlayed(channel.id)
    }

    fun refresh() {
        viewModelScope.launch {
            withContext(ioDispatcher) { runCatching { provider.refreshLive() } }
            load()
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
                val ppvCategories = payload.first.filter { isPpvCategory(it.name) }
                val ppvIds = ppvCategories.map { it.id }.toSet()
                val ppvChannels = payload.second.filter { channel ->
                    channel.categoryIds.any { it in ppvIds }
                }
                val now = System.currentTimeMillis()
                val guideByChannel = payload.third.groupBy { it.channelId }
                val programmes = ppvChannels.associate { channel ->
                    val forChannel = guideByChannel[channel.id].orEmpty().sortedBy { it.startUtcMs }
                    channel.id to LiveProgramme(
                        now = forChannel.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs },
                        next = forChannel.firstOrNull { it.startUtcMs > now },
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        categories = ppvCategories,
                        channels = ppvChannels,
                        programmes = programmes,
                        selectedCategoryId = it.selectedCategoryId
                            ?.takeIf { id -> ppvCategories.any { category -> category.id == id } },
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
