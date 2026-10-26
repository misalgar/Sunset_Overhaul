package com.markooverhaul.sunset.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.ProgressRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface SimilarItem {
    data class MovieItem(val movie: Movie) : SimilarItem
    data class SeriesItem(val series: SeriesSummary) : SimilarItem
}

sealed interface SimilarState {
    data object Loading : SimilarState
    data object Empty : SimilarState
    data class Loaded(val items: List<SimilarItem>) : SimilarState
}

data class DetailsUiState(
    val isLoading: Boolean = true,
    val isLoadingDetail: Boolean = false,
    val movie: Movie? = null,
    val series: SeriesDetail? = null,
    val selectedSeason: Int = 1,
    val progress: WatchProgress? = null,
    val isFavourite: Boolean = false,
    val similar: SimilarState = SimilarState.Loading,
    val error: Throwable? = null,
) {
    val title: String
        get() = movie?.title ?: series?.summary?.title.orEmpty()

    val resumeEpisodeId: String?
        get() = progress?.episodeId
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val provider: ContentProvider,
    private val progressRepository: ProgressRepository,
    private val favouritesRepository: FavouritesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val contentType: ContentType = savedStateHandle.get<String>(ARG_TYPE)
        ?.let { runCatching { ContentType.valueOf(it) }.getOrNull() }
        ?: ContentType.MOVIE
    private val contentId: String = savedStateHandle.get<String>(ARG_ID).orEmpty()

    /** The id being shown. Movies can switch between versions; progress and favourites follow it. */
    private val activeId = MutableStateFlow(contentId)

    private val _state = MutableStateFlow(DetailsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            activeId.flatMapLatest { progressRepository.observe(it) }.collect { progress ->
                _state.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            combine(activeId, favouritesRepository.contentIds) { id, ids -> id in ids }
                .collect { isFavourite ->
                    _state.update { it.copy(isFavourite = isFavourite) }
                }
        }
        load()
    }

    /** Shows another version of the same movie; Play, progress and favourites use that version. */
    fun selectVariant(id: String) {
        if (id == activeId.value) return
        activeId.value = id
        viewModelScope.launch {
            val movie = withContext(ioDispatcher) {
                runCatching { provider.getMovie(id) }.getOrNull()
            } ?: return@launch
            if (activeId.value == id) {
                _state.update { it.copy(movie = movie) }
            }
        }
    }

    fun selectSeason(number: Int) {
        _state.update { it.copy(selectedSeason = number) }
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            favouritesRepository.toggle(activeId.value, contentType)
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, isLoadingDetail = false, error = null, similar = SimilarState.Loading)
            }
            try {
                when (contentType) {
                    ContentType.MOVIE -> loadMovie()
                    ContentType.SERIES -> loadSeries()
                    else -> _state.update {
                        it.copy(isLoading = false, error = ProviderError.NotFound)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    private suspend fun loadMovie() {
        // Fast path: paint the title/artwork from Room while provider detail and TMDB load.
        val summary = withContext(ioDispatcher) {
            runCatching { provider.getMovieSummary(activeId.value) }.getOrNull()
        }
        if (summary != null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingDetail = true,
                    movie = summary,
                    similar = SimilarState.Loading,
                    error = null,
                )
            }
        }

        val movie = withContext(ioDispatcher) { provider.getMovie(activeId.value) }
        if (movie == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingDetail = false,
                    error = ProviderError.NotFound,
                    similar = SimilarState.Empty,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isLoading = false,
                isLoadingDetail = false,
                movie = movie,
                error = null,
            )
        }
        viewModelScope.launch {
            val recommended = withContext(ioDispatcher) {
                runCatching { provider.getMovieRecommendations(movie.id) }
                    .getOrDefault(emptyList())
            }
            val similar = if (recommended.isNotEmpty()) {
                recommended
            } else {
                withContext(ioDispatcher) {
                    provider.getMovies(page = 1).items
                        .filter { candidate ->
                            candidate.id != movie.id &&
                                candidate.genres.any { it in movie.genres }
                        }
                        .take(10)
                }
            }
            if (_state.value.movie?.id == movie.id) {
                val items = similar.map(SimilarItem::MovieItem)
                _state.update {
                    it.copy(
                        similar = if (items.isEmpty()) {
                            SimilarState.Empty
                        } else {
                            SimilarState.Loaded(items)
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadSeries() {
        // Fast path: the Room summary paints the hero while the provider detail and enrichment load.
        val summary = withContext(ioDispatcher) {
            runCatching { provider.getSeriesSummary(contentId) }.getOrNull()
        }
        if (summary != null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingDetail = true,
                    series = SeriesDetail(summary = summary),
                    similar = SimilarState.Loading,
                    error = null,
                )
            }
        }

        val detail = withContext(ioDispatcher) { provider.getSeriesDetail(contentId) }
        if (detail == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingDetail = false,
                    error = ProviderError.NotFound,
                    similar = SimilarState.Empty,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isLoading = false,
                isLoadingDetail = false,
                series = detail,
                selectedSeason = detail.seasons.firstOrNull()?.number
                    ?: it.selectedSeason,
                error = null,
            )
        }

        viewModelScope.launch {
            val recommended = withContext(ioDispatcher) {
                runCatching { provider.getSeriesRecommendations(detail.summary.id) }
                    .getOrDefault(emptyList())
            }
            val similar = if (recommended.isNotEmpty()) {
                recommended
            } else {
                val genres = detail.summary.genres
                withContext(ioDispatcher) {
                    provider.getSeries(page = 1).items
                        .filter { candidate ->
                            candidate.id != detail.summary.id &&
                                candidate.genres.any { it in genres }
                        }
                        .take(10)
                }
            }
            if (_state.value.series?.summary?.id == detail.summary.id) {
                val items = similar.map(SimilarItem::SeriesItem)
                _state.update {
                    it.copy(
                        similar = if (items.isEmpty()) {
                            SimilarState.Empty
                        } else {
                            SimilarState.Loaded(items)
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_TYPE = "type"
        const val ARG_ID = "id"
    }
}
