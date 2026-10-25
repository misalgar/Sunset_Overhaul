package com.markooverhaul.sunset.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.RecentSearch
import com.markooverhaul.sunset.core.data.repository.FavouritesRepository
import com.markooverhaul.sunset.core.data.repository.SearchHistoryRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.SearchResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SearchFilter { All, Live, Movies, Series }

data class SearchUiState(
    val query: String = "",
    val results: SearchResults? = null,
    val recent: List<RecentSearch> = emptyList(),
    val favouriteIds: Set<String> = emptySet(),
    val filter: SearchFilter = SearchFilter.All,
    val isSearching: Boolean = false,
    val error: Throwable? = null,
) {
    val hasQuery: Boolean get() = query.trim().length >= MIN_QUERY_LENGTH
    val showPrompt: Boolean get() = !hasQuery && results == null && !isSearching

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val provider: ContentProvider,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val favouritesRepository: FavouritesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            searchHistoryRepository.recent.collect { recent ->
                _state.update { it.copy(recent = recent) }
            }
        }
        viewModelScope.launch {
            favouritesRepository.contentIds.collect { ids ->
                _state.update { it.copy(favouriteIds = ids) }
            }
        }
        val deepLinkQuery = savedStateHandle.get<String>(ARG_QUERY).orEmpty()
        if (deepLinkQuery.isNotBlank()) {
            _state.update { it.copy(query = deepLinkQuery) }
            search(deepLinkQuery, record = false)
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.trim().length < SearchUiState.MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query, record = false)
        }
    }

    fun submit() {
        val query = _state.value.query
        if (query.trim().length >= SearchUiState.MIN_QUERY_LENGTH) {
            search(query, record = true)
        }
    }

    fun search(query: String, record: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearch(query, record)
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = null, isSearching = false, error = null) }
    }

    fun clearRecent() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }

    fun setFilter(filter: SearchFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun toggleChannelFavourite(channel: LiveChannel) {
        viewModelScope.launch { favouritesRepository.toggle(channel.id, ContentType.LIVE) }
    }

    private suspend fun runSearch(rawQuery: String, record: Boolean) {
        val query = rawQuery.trim()
        if (query.length < SearchUiState.MIN_QUERY_LENGTH) return
        _state.update { it.copy(isSearching = true, error = null) }
        try {
            val results = withContext(ioDispatcher) { provider.search(query) }
            _state.update { it.copy(results = results, isSearching = false) }
            if (record) searchHistoryRepository.record(query)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update { it.copy(isSearching = false, error = error) }
        }
    }

    companion object {
        const val ARG_QUERY = "q"
        private const val DEBOUNCE_MS = 300L
    }
}
