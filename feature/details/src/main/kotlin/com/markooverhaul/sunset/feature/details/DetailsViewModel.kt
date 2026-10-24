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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface SimilarItem {
    data class MovieItem(val movie: Movie) : SimilarItem
    data class SeriesItem(val series: SeriesSummary) : SimilarItem
}

data class DetailsUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val series: SeriesDetail? = null,
    val selectedSeason: Int = 1,
    val progress: WatchProgress? = null,
    val isFavourite: Boolean = false,
    val similar: List<SimilarItem> = emptyList(),
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

    private val _state = MutableStateFlow(DetailsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            progressRepository.observe(contentId).collect { progress ->
                _state.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(isFavourite = contentId in ids) }
            }
        }
        load()
    }

    fun selectSeason(number: Int) {
        _state.update { it.copy(selectedSeason = number) }
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            favouritesRepository.toggle(contentId, contentType)
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
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
        val movie = withContext(ioDispatcher) { provider.getMovie(contentId) }
        if (movie == null) {
            _state.update { it.copy(isLoading = false, error = ProviderError.NotFound) }
            return
        }
        val recommended = withContext(ioDispatcher) {
            provider.getMovieRecommendations(movie.id)
        }
        val similar = if (recommended.isNotEmpty()) {
            recommended
        } else {
            withContext(ioDispatcher) {
                provider.getMovies(page = 1).items
                    .filter { candidate ->
                        candidate.id != movie.id && candidate.genres.any { it in movie.genres }
                    }
                    .take(10)
            }
        }
        _state.update {
            it.copy(
                isLoading = false,
                movie = movie,
                similar = similar.map(SimilarItem::MovieItem),
                error = null,
            )
        }
    }

    private suspend fun loadSeries() {
        val detail = withContext(ioDispatcher) { provider.getSeriesDetail(contentId) }
        if (detail == null) {
            _state.update { it.copy(isLoading = false, error = ProviderError.NotFound) }
            return
        }
        val recommended = withContext(ioDispatcher) {
            provider.getSeriesRecommendations(detail.summary.id)
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
        _state.update {
            it.copy(
                isLoading = false,
                series = detail,
                selectedSeason = detail.seasons.firstOrNull()?.number ?: 1,
                similar = similar.map(SimilarItem::SeriesItem),
                error = null,
            )
        }
    }

    companion object {
        const val ARG_TYPE = "type"
        const val ARG_ID = "id"
    }
}
