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
    val selectedCountry: String? = null,
    val countryPickerOpen: Boolean = false,
    val error: Throwable? = null,
) {
    val visibleChannels: List<LiveChannel>
        get() {
            val base = if (favouritesOnly) channels.filter { it.id in favourites } else channels
            return when (selectedCountry) {
                null -> base
                GuideViewModel.OTHER_COUNTRY -> base.filter { it.countryCode == null }
                else -> base.filter { it.countryCode == selectedCountry }
            }
        }
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

    fun selectCountry(countryCode: String?) {
        _state.update { it.copy(selectedCountry = countryCode, countryPickerOpen = false) }
    }

    fun openCountryPicker() {
        _state.update { it.copy(countryPickerOpen = true) }
    }

    fun dismissCountryPicker() {
        _state.update { it.copy(countryPickerOpen = false) }
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
                    it.copy(
                        isLoading = false,
                        channels = channels.filterNot { channel -> channel.isAdult },
                        programmes = guide,
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

    companion object {
        /** Sentinel for channels with no recognised country. */
        const val OTHER_COUNTRY = "__other__"

        /** Local midnight for today, or tomorrow when [dayOffset] is 1. */
        fun startOfDay(dayOffset: Int): Long = startOfDay(System.currentTimeMillis(), dayOffset)

        /**
         * Local midnight of the day that contains [referenceUtcMs], offset by [dayOffset] days.
         *
         * The reference time is a parameter so a visual test can freeze the clock and get the same
         * day boundary on every run.
         */
        fun startOfDay(referenceUtcMs: Long, dayOffset: Int): Long = Calendar.getInstance().apply {
            timeInMillis = referenceUtcMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
    }
}
