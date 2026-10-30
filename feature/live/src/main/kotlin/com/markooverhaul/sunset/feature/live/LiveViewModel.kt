package com.markooverhaul.sunset.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.LivePlaybackQueue
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveCategoryGroup
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.isPpvCategory
import com.markooverhaul.sunset.core.model.liveCategoryGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LiveProgramme(
    val now: EpgProgramme? = null,
    val next: EpgProgramme? = null,
)

/** One row of the TV sidebar (or a phone filter chip): a category plus its live channel count. */
data class LiveSidebarCategory(
    val id: String,
    val name: String,
    val count: Int,
    val group: LiveCategoryGroup = LiveCategoryGroup.Genres,
)

/** The filter currently driving the channel grid. */
sealed interface LiveFilter {
    data object All : LiveFilter
    data object Favourites : LiveFilter
    data object Adult : LiveFilter
    data class Category(val id: String) : LiveFilter

    val key: String
        get() = when (this) {
            All -> "all"
            Favourites -> "favourites"
            Adult -> "adult"
            is Category -> "category:$id"
        }
}

data class LiveUiState(
    val isLoading: Boolean = true,
    /** Provider live categories with a non-zero, non-adult channel count, in provider order. */
    val categories: List<LiveSidebarCategory> = emptyList(),
    /** Every live channel, including adult ones. Adult filtering happens in [visibleChannels]. */
    val channels: List<LiveChannel> = emptyList(),
    val programmes: Map<String, LiveProgramme> = emptyMap(),
    val favourites: Set<String> = emptySet(),
    val selectedFilter: LiveFilter = LiveFilter.All,
    /** Cleaned name of the provider's adult category, when it publishes one. */
    val adultCategoryName: String? = null,
    val error: Throwable? = null,
) {
    val allChannelCount: Int get() = channels.count { !it.isAdult }

    val favouriteChannelCount: Int get() = channels.count { !it.isAdult && it.id in favourites }

    val adultChannelCount: Int get() = channels.count { it.isAdult }

    /**
     * Adult channels are only ever exposed by the explicit [LiveFilter.Adult] row; every other
     * filter (including Favourites) excludes them.
     */
    val visibleChannels: List<LiveChannel>
        get() = when (val filter = selectedFilter) {
            LiveFilter.All -> channels.filterNot { it.isAdult }
            LiveFilter.Favourites -> channels.filter { !it.isAdult && it.id in favourites }
            LiveFilter.Adult -> channels.filter { it.isAdult }
            is LiveFilter.Category -> channels.filter {
                !it.isAdult && filter.id in it.categoryIds
            }
        }

    val hasAnyChannel: Boolean get() = channels.isNotEmpty()
}

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val provider: ContentProvider,
    private val favouritesRepository: FavouritesRepository,
    private val liveQueue: LivePlaybackQueue,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveUiState())
    val state = _state.asStateFlow()

    /** Instant focus, used for the hero's text. Null until the viewer focuses a channel. */
    private val _focusedChannel = MutableStateFlow<LiveChannel?>(null)
    val focusedChannel = _focusedChannel.asStateFlow()

    /** Delayed focus, used for video. Stays null until the viewer rests on a channel. */
    private val _previewChannel = MutableStateFlow<LiveChannel?>(null)
    val previewChannel = _previewChannel.asStateFlow()

    private var previewDebounceJob: Job? = null
    private var loadJob: Job? = null

    /** Set by the player so the Live screen can put focus back on the watched channel. */
    val lastPlayedChannelId = liveQueue.lastPlayedChannelId

    init {
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(favourites = ids) }
            }
        }
        load()
    }

    fun selectFilter(filter: LiveFilter) {
        _state.update { it.copy(selectedFilter = filter) }
        clearFocus()
    }

    fun toggleFavourite(channel: LiveChannel) {
        viewModelScope.launch {
            favouritesRepository.toggle(channel.id, ContentType.LIVE)
        }
    }

    /** Remembers the list the viewer picked the channel from, so the player can zap up/down. */
    fun onChannelPlayed(channel: LiveChannel) {
        val categoryId = (_state.value.selectedFilter as? LiveFilter.Category)?.id
        liveQueue.setQueue(_state.value.visibleChannels.map { it.id }, categoryId)
        liveQueue.markPlayed(channel.id)
    }

    /**
     * Focus moves instantly, but the preview engine only starts after the viewer rests on a
     * channel, so fast D-pad travel does not thrash the decoder or the network.
     */
    fun onChannelFocused(channel: LiveChannel) {
        if (_focusedChannel.value?.id == channel.id) return
        _focusedChannel.value = channel
        _previewChannel.value = null
        previewDebounceJob?.cancel()
        previewDebounceJob = viewModelScope.launch {
            delay(PREVIEW_REST_MS)
            _previewChannel.value = channel
        }
    }

    /** Called when focus leaves the grid, so the hero resets and the preview stops. */
    fun clearFocus() {
        previewDebounceJob?.cancel()
        previewDebounceJob = null
        _focusedChannel.value = null
        _previewChannel.value = null
    }

    fun load() {
        // Retry must not race the previous attempt: a slow older response could overwrite the new one.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
                // PPV has its own destination; keep its dated categories and channels out of Live TV.
                val ppvCategoryIds = payload.first
                    .filter { isPpvCategory(it.name) }
                    .map { it.id }
                    .toSet()
                val liveCategories = payload.first.filterNot { it.id in ppvCategoryIds }
                val liveChannels = payload.second.filter { channel ->
                    channel.categoryIds.none { it in ppvCategoryIds }
                }
                // Adult categories are folded into the single explicit Adultos row. Categories are
                // grouped so daily sports feeds do not bury the standard genres.
                val visibleCategories = liveCategories
                    .filterNot { it.isAdult }
                    .mapNotNull { category ->
                        val count = liveChannels.count { channel ->
                            !channel.isAdult && category.id in channel.categoryIds
                        }
                        category.takeIf { count > 0 }?.let {
                            LiveSidebarCategory(
                                id = it.id,
                                name = it.name,
                                count = count,
                                group = liveCategoryGroup(it.name),
                            )
                        }
                    }
                    .sortedBy { it.group.ordinal }
                // Group once; the guide can hold tens of thousands of programmes.
                val guideByChannel = payload.third.groupBy { it.channelId }
                val programmes = liveChannels.associate { channel ->
                    val forChannel = guideByChannel[channel.id].orEmpty().sortedBy { it.startUtcMs }
                    channel.id to LiveProgramme(
                        now = forChannel.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs },
                        next = forChannel.firstOrNull { it.startUtcMs > now },
                    )
                }
                _state.update {
                    val hasAdult = liveChannels.any { channel -> channel.isAdult }
                    val adultName = liveCategories.firstOrNull { category -> category.isAdult }?.name
                    val nextFilter = when (val filter = it.selectedFilter) {
                        is LiveFilter.Category -> filter.takeIf { current ->
                            visibleCategories.any { category -> category.id == current.id }
                        } ?: LiveFilter.All

                        LiveFilter.Adult -> if (hasAdult) LiveFilter.Adult else LiveFilter.All
                        else -> filter
                    }
                    it.copy(
                        isLoading = false,
                        categories = visibleCategories,
                        channels = liveChannels,
                        programmes = programmes,
                        selectedFilter = nextFilter,
                        adultCategoryName = adultName,
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

    private companion object {
        /** Time the viewer must rest on a channel before the video preview starts. */
        const val PREVIEW_REST_MS = 1_500L
    }
}
