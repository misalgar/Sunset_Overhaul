package com.markooverhaul.sunset.feature.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.paging.MoviePagingSource
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoadingCategories: Boolean = true,
    val error: Throwable? = null,
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val provider: ContentProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(MoviesUiState())
    val state = _state.asStateFlow()

    private val categoryFilter = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val movies: Flow<PagingData<Movie>> = categoryFilter
        .flatMapLatest { categoryId ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false,
                    prefetchDistance = 2,
                ),
            ) {
                MoviePagingSource(provider, categoryId)
            }.flow
        }
        .cachedIn(viewModelScope)

    init {
        loadCategories()
    }

    fun selectCategory(categoryId: String?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
        categoryFilter.value = categoryId
    }

    fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCategories = true, error = null) }
            try {
                val categories = withContext(ioDispatcher) { provider.getMovieCategories() }
                _state.update { it.copy(categories = categories, isLoadingCategories = false) }
            } catch (error: Throwable) {
                _state.update { it.copy(isLoadingCategories = false, error = error) }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 8
    }
}
