package com.markooverhaul.sunset.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.ProgressRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChannelNowNext(
    val now: EpgProgramme? = null,
    val next: EpgProgramme? = null,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val hero: Movie? = null,
    val liveNow: List<LiveChannel> = emptyList(),
    val recentMovies: List<Movie> = emptyList(),
    val recentSeries: List<SeriesSummary> = emptyList(),
    val recommended: List<Movie> = emptyList(),
    val continueWatching: List<WatchProgress> = emptyList(),
    val favouriteIds: Set<String> = emptySet(),
    val nowNext: Map<String, ChannelNowNext> = emptyMap(),
    val error: Throwable? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && hero == null &&
            liveNow.isEmpty() && recentMovies.isEmpty() && recentSeries.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val provider: ContentProvider,
    private val progressRepository: ProgressRepository,
    favouritesRepository: FavouritesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            progressRepository.continueWatching.collect { progress ->
                _state.update { it.copy(continueWatching = progress) }
            }
        }
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(favouriteIds = ids) }
            }
        }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fast path: live data renders immediately.
                val (channels, guide) = withContext(ioDispatcher) {
                    coroutineScope {
                        val channels = async { provider.getLiveChannels() }
                        val guide = async { provider.getGuide(days = 1) }
                        channels.await() to guide.await()
                    }
                }
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        isLoading = false,
                        liveNow = channels.take(10),
                        nowNext = nowNextOf(channels, guide, now),
                        error = null,
                    )
                }

                // Slow path: the VOD and series catalogues are large and cache-backed; Home should
                // not stay blank while they download on first launch.
                val (movies, series) = withContext(ioDispatcher) {
                    coroutineScope {
                        val movies = async { provider.getMovies(page = 1) }
                        val series = async { provider.getSeries(page = 1) }
                        movies.await().items to series.await().items
                    }
                }
                val recentMovies = movies.sortedByDescending { it.addedAtUtcMs }.take(12)
                _state.update {
                    it.copy(
                        hero = recentMovies.firstOrNull() ?: it.hero,
                        recentMovies = recentMovies,
                        recentSeries = series.sortedByDescending { s -> s.addedAtUtcMs }.take(12),
                        recommended = movies.sortedByDescending { m -> m.rating ?: 0.0 }.take(12),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    private fun nowNextOf(
        channels: List<LiveChannel>,
        guide: List<EpgProgramme>,
        now: Long,
    ): Map<String, ChannelNowNext> {
        // Group once: the guide can hold tens of thousands of programmes and filtering per channel
        // would be O(channels x programmes) on every refresh.
        val byChannel = guide.groupBy { it.channelId }
        return channels.associate { channel ->
            val programmes = byChannel[channel.id].orEmpty().sortedBy { it.startUtcMs }
            channel.id to ChannelNowNext(
                now = programmes.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs },
                next = programmes.firstOrNull { it.startUtcMs > now },
            )
        }
    }

}
