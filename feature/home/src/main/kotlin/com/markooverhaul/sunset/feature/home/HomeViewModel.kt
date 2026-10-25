package com.markooverhaul.sunset.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.ProgressRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
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

data class HomeRailItem(
    val id: String,
    val type: ContentType,
    val title: String,
    val posterUrl: String?,
    val year: Int?,
    val subtitle: String?,
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
    val becauseTitle: String? = null,
    val becauseItems: List<HomeRailItem> = emptyList(),
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
    private var becauseJob: Job? = null
    private var becauseAnchorId: String? = null

    init {
        viewModelScope.launch {
            progressRepository.continueWatching.collect { progress ->
                val enriched = withContext(ioDispatcher) {
                    progress.map { item ->
                        val artwork = runCatching {
                            when (item.contentType) {
                                ContentType.MOVIE -> {
                                    val movie = provider.getMovieSummary(item.contentId)
                                    movie?.backdropUrl ?: movie?.posterUrl
                                }

                                ContentType.SERIES, ContentType.EPISODE -> {
                                    val series = provider.getSeriesSummary(item.contentId)
                                    series?.backdropUrl ?: series?.posterUrl
                                }

                                ContentType.LIVE -> null
                            }
                        }.getOrNull()
                        item.copy(artworkUrl = artwork)
                    }
                }
                _state.update { it.copy(continueWatching = enriched) }
                enriched.firstOrNull()?.let(::loadBecauseYouWatched)
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
                val now = System.currentTimeMillis()
                coroutineScope {
                    val channelsDeferred = async(ioDispatcher) { provider.getLiveChannels() }
                    val guideDeferred = async(ioDispatcher) { provider.getGuide(days = 1) }
                    val moviesDeferred = async(ioDispatcher) { provider.getMovies(page = 1) }
                    val seriesDeferred = async(ioDispatcher) { provider.getSeries(page = 1) }

                    // Live channel rows are available first; the guide and the Room catalogue then
                    // fill their slots independently, so a slow XMLTV parse never hides the hero.
                    val channels = channelsDeferred.await()
                    val visibleChannels = channels.filterNot { it.isAdult }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            liveNow = visibleChannels.take(10),
                            error = null,
                        )
                    }

                    launch {
                        val movies = runCatching { moviesDeferred.await().items }
                            .getOrDefault(emptyList())
                        val series = runCatching { seriesDeferred.await().items }
                            .getOrDefault(emptyList())
                        val recentMovies = movies.sortedByDescending { it.addedAtUtcMs }.take(12)
                        _state.update {
                            it.copy(
                                hero = recentMovies.firstOrNull() ?: it.hero,
                                recentMovies = recentMovies,
                                recentSeries = series.sortedByDescending { s -> s.addedAtUtcMs }
                                    .take(12),
                                recommended = movies.sortedByDescending { m -> m.rating ?: 0.0 }
                                    .take(12),
                            )
                        }
                        _state.value.continueWatching.firstOrNull()?.let(::loadBecauseYouWatched)
                    }

                    launch {
                        val guide = runCatching { guideDeferred.await() }.getOrDefault(emptyList())
                        _state.update { it.copy(nowNext = nowNextOf(visibleChannels, guide, now)) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    private fun loadBecauseYouWatched(progress: WatchProgress) {
        if (becauseAnchorId == progress.contentId) return
        becauseAnchorId = progress.contentId
        becauseJob?.cancel()
        becauseJob = viewModelScope.launch {
            val items = withContext(ioDispatcher) {
                when (progress.contentType) {
                    ContentType.MOVIE -> provider.getMovieRecommendations(progress.contentId)
                        .map { movie ->
                            HomeRailItem(
                                id = movie.id,
                                type = ContentType.MOVIE,
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                year = movie.year,
                                subtitle = movie.genres.firstOrNull(),
                            )
                        }

                    ContentType.SERIES, ContentType.EPISODE ->
                        provider.getSeriesRecommendations(progress.contentId)
                            .map { series ->
                                HomeRailItem(
                                    id = series.id,
                                    type = ContentType.SERIES,
                                    title = series.title,
                                    posterUrl = series.posterUrl,
                                    year = series.year,
                                    subtitle = series.genres.firstOrNull(),
                                )
                            }

                    ContentType.LIVE -> emptyList()
                }
            }
            _state.update {
                it.copy(
                    becauseTitle = progress.title,
                    becauseItems = items,
                )
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
