package com.markooverhaul.sunset.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.Favourite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MyListFilter { ALL, MOVIES, SERIES, LIVE }

sealed interface SavedItem {
    val id: String
    val type: ContentType
    val title: String
    val subtitle: String?
    val imageUrl: String?

    data class MovieItem(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
    ) : SavedItem {
        override val type: ContentType = ContentType.MOVIE
    }

    data class SeriesItem(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
    ) : SavedItem {
        override val type: ContentType = ContentType.SERIES
    }

    data class LiveItem(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
    ) : SavedItem {
        override val type: ContentType = ContentType.LIVE
    }
}

data class MyListUiState(
    val isLoading: Boolean = true,
    val items: List<SavedItem> = emptyList(),
    val filter: MyListFilter = MyListFilter.ALL,
    val error: Throwable? = null,
) {
    val visibleItems: List<SavedItem>
        get() = when (filter) {
            MyListFilter.ALL -> items
            MyListFilter.MOVIES -> items.filter { it.type == ContentType.MOVIE }
            MyListFilter.SERIES -> items.filter { it.type == ContentType.SERIES }
            MyListFilter.LIVE -> items.filter { it.type == ContentType.LIVE }
        }
}

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
    private val provider: ContentProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(MyListUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favouritesRepository.favourites.collect { favourites ->
                try {
                    val items = withContext(ioDispatcher) { favourites.mapNotNull { resolve(it) } }
                    _state.update { it.copy(isLoading = false, items = items, error = null) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _state.update { it.copy(isLoading = false, error = error) }
                }
            }
        }
    }

    fun selectFilter(filter: MyListFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun remove(item: SavedItem) {
        viewModelScope.launch {
            favouritesRepository.remove(item.id, item.type)
        }
    }

    private suspend fun resolve(favourite: Favourite): SavedItem? = when (favourite.contentType) {
        ContentType.MOVIE -> provider.getMovie(favourite.contentId)?.let { movie ->
            SavedItem.MovieItem(
                id = movie.id,
                title = movie.title,
                subtitle = movie.genres.firstOrNull() ?: movie.year?.toString(),
                imageUrl = movie.posterUrl,
            )
        }

        ContentType.SERIES -> provider.getSeriesDetail(favourite.contentId)?.summary?.let { series ->
            SavedItem.SeriesItem(
                id = series.id,
                title = series.title,
                subtitle = series.genres.firstOrNull() ?: series.year?.toString(),
                imageUrl = series.posterUrl,
            )
        }

        ContentType.LIVE -> provider.getLiveChannels()
            .firstOrNull { it.id == favourite.contentId }
            ?.let { channel ->
                SavedItem.LiveItem(
                    id = channel.id,
                    title = channel.name,
                    subtitle = null,
                    imageUrl = channel.logoUrl,
                )
            }

        ContentType.EPISODE -> null
    }
}
