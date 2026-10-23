package com.markooverhaul.sunset.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.model.ContentProvider
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
import java.util.Calendar
import javax.inject.Inject

data class GuideUiState(
    val isLoading: Boolean = true,
    val channels: List<LiveChannel> = emptyList(),
    val programmes: List<EpgProgramme> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val selectedDay: Int = 0,
    val favouritesOnly: Boolean = false,
    val error: Throwable? = null,
) {
    val visibleChannels: List<LiveChannel>
        get() = if (favouritesOnly) channels.filter { it.id in favourites } else channels
}

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val provider: ContentProvider,
    favouritesRepository: FavouritesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(GuideUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(favourites = ids) }
            }
        }
        load()
    }

    fun selectDay(index: Int) {
        _state.update { it.copy(selectedDay = index.coerceIn(0, 1)) }
    }

    fun toggleFavouritesOnly() {
        _state.update { it.copy(favouritesOnly = !it.favouritesOnly) }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val (channels, guide) = withContext(ioDispatcher) {
                    coroutineScope {
                        val channels = async { provider.getLiveChannels() }
                        val guide = async { provider.getGuide(days = 2) }
                        channels.await() to guide.await()
                    }
                }
                _state.update {
                    it.copy(isLoading = false, channels = channels, programmes = guide, error = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    companion object {
        /** Local midnight for today, or tomorrow when [dayOffset] is 1. */
        fun startOfDay(dayOffset: Int): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
    }
}
