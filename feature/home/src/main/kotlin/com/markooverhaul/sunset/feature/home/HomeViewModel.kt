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
                val payload = withContext(ioDispatcher) {
                    coroutineScope {
                        val movies = async { provider.getMovies(page = 1) }
                        val series = async { provider.getSeries(page = 1) }
                        val channels = async { provider.getLiveChannels() }
                        val guide = async { provider.getGuide(days = 1) }
                        HomePayload(
                            movies = movies.await().items,
                            series = series.await().items,
                            channels = channels.await(),
                            guide = guide.await(),
                        )
                    }
                }
                val now = System.currentTimeMillis()
                val nowNext = payload.channels.associate { channel ->
                    val programmes = payload.guide
                        .filter { it.channelId == channel.id }
                        .sortedBy { it.startUtcMs }
                    channel.id to ChannelNowNext(
                        now = programmes.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs },
                        next = programmes.firstOrNull { it.startUtcMs > now },
                    )
                }
                val recentMovies = payload.movies.sortedByDescending { it.addedAtUtcMs }.take(12)
                _state.update {
                    it.copy(
                        isLoading = false,
                        hero = recentMovies.firstOrNull(),
                        liveNow = payload.channels.take(10),
                        recentMovies = recentMovies,
                        recentSeries = payload.series.sortedByDescending { s -> s.addedAtUtcMs }.take(12),
                        recommended = payload.movies.sortedByDescending { m -> m.rating ?: 0.0 }.take(12),
                        nowNext = nowNext,
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

    private data class HomePayload(
        val movies: List<Movie>,
        val series: List<SeriesSummary>,
        val channels: List<LiveChannel>,
        val guide: List<EpgProgramme>,
    )
}
