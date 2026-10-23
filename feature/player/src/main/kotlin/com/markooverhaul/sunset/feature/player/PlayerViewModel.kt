package com.markooverhaul.sunset.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.data.repository.ProgressRepository
import com.markooverhaul.sunset.core.data.repository.SettingsRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.model.WatchProgress
import com.markooverhaul.sunset.core.player.PlaybackEngine
import com.markooverhaul.sunset.core.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val subtitle: String? = null,
    val live: Boolean = false,
    val isResolving: Boolean = true,
    val resolveError: Throwable? = null,
    val resumeFromMs: Long = 0L,
)

/**
 * Owns stream resolution, resume position, autoplay-next and progress persistence.
 *
 * The engine is a separate, unscoped Hilt binding so each player screen owns its own ExoPlayer
 * instance and releases it in [onCleared]. Stream references from the demo provider resolve to the
 * bundled raw sample, so no network connection is needed.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val provider: ContentProvider,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    val engine: PlaybackEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val contentType: ContentType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>(ARG_TYPE).orEmpty())
    }.getOrDefault(ContentType.MOVIE)

    private val contentId: String = savedStateHandle.get<String>(ARG_ID).orEmpty()
    private val displayTitle: String = savedStateHandle.get<String>(ARG_TITLE).orEmpty()

    private var episodeId: String? =
        savedStateHandle.get<String>(ARG_EPISODE_ID)?.takeIf { it.isNotBlank() }
    private var episodeTitle: String? =
        savedStateHandle.get<String>(ARG_EPISODE_TITLE)?.takeIf { it.isNotBlank() }

    private val live: Boolean =
        savedStateHandle.get<Boolean>(ARG_LIVE) ?: (contentType == ContentType.LIVE)

    private val _state = MutableStateFlow(
        PlayerUiState(
            title = displayTitle,
            subtitle = episodeTitle,
            live = live,
        ),
    )
    val state = _state.asStateFlow()

    /** Latest engine snapshot, kept for the final save in [onCleared]. */
    @Volatile
    private var latest: PlaybackState = PlaybackState()

    private var autoplayNext: Boolean = true
    private var advancing = false
    private var started = false
    private var wasEnded = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> autoplayNext = settings.autoplayNext }
        }
        viewModelScope.launch {
            engine.state.collect { playback ->
                latest = playback
                if (playback.ended && !wasEnded && !advancing) maybeAutoplayNext()
                wasEnded = playback.ended
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                saveProgress(latest)
            }
        }
        viewModelScope.launch { start() }
    }

    fun retry() {
        viewModelScope.launch { start() }
    }

    fun togglePlayPause() {
        val playback = latest
        when {
            playback.error != null -> retry()
            playback.ended -> {
                engine.seekTo(0L)
                engine.play()
            }
            playback.isPlaying -> engine.pause()
            else -> engine.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        if (live) return
        engine.seekTo((latest.positionMs + deltaMs).coerceAtLeast(0L))
    }

    fun seekTo(positionMs: Long) {
        if (live) return
        engine.seekTo(positionMs)
    }

    private suspend fun start() {
        _state.update { it.copy(isResolving = true, resolveError = null) }
        try {
            val startPosition = if (live) 0L else withContext(ioDispatcher) { resumePosition() }
            _state.update { it.copy(resumeFromMs = startPosition) }
            val source = withContext(ioDispatcher) {
                provider.resolveStream(
                    StreamRequest(
                        contentType = contentType,
                        contentId = contentId,
                        episodeId = episodeId,
                        startPositionMs = startPosition,
                        live = live,
                    ),
                )
            }
            engine.prepare(source, startPosition, live)
            started = true
            _state.update { it.copy(isResolving = false) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update { it.copy(isResolving = false, resolveError = error) }
        }
    }

    private suspend fun resumePosition(): Long {
        val saved = progressRepository.get(contentId) ?: return 0L
        if (saved.completed) return 0L
        if (saved.fraction < WatchProgress.MIN_RESUME_FRACTION) return 0L
        if (saved.fraction >= WatchProgress.COMPLETE_FRACTION) return 0L
        // Series progress is stored per series, so only resume when it belongs to this episode.
        if (contentType == ContentType.SERIES && saved.episodeId != null && saved.episodeId != episodeId) {
            return 0L
        }
        return saved.positionMs
    }

    private fun maybeAutoplayNext() {
        if (!started || !autoplayNext) return
        if (contentType != ContentType.SERIES || episodeId == null) return
        viewModelScope.launch {
            advancing = true
            try {
                val detail = withContext(ioDispatcher) { provider.getSeriesDetail(contentId) } ?: return@launch
                val episodes = detail.seasons.flatMap { it.episodes }
                val currentIndex = episodes.indexOfFirst { it.id == episodeId }
                val next = episodes.getOrNull(currentIndex + 1) ?: return@launch
                saveProgress(latest)
                episodeId = next.id
                episodeTitle = next.title
                _state.update { it.copy(subtitle = next.title, resumeFromMs = 0L) }
                val source = withContext(ioDispatcher) {
                    provider.resolveStream(
                        StreamRequest(
                            contentType = ContentType.SERIES,
                            contentId = contentId,
                            episodeId = next.id,
                            startPositionMs = 0L,
                            live = false,
                        ),
                    )
                }
                engine.prepare(source, 0L, false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Autoplay is best-effort; leaving the player on the finished frame is acceptable.
            } finally {
                advancing = false
            }
        }
    }

    private suspend fun saveProgress(playback: PlaybackState) {
        if (live || playback.durationMs <= 0L) return
        runCatching {
            progressRepository.save(
                contentId = contentId,
                contentType = contentType,
                title = displayTitle.ifBlank { _state.value.title },
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                episodeId = episodeId,
            )
        }
    }

    override fun onCleared() {
        val snapshot = latest
        if (!live && snapshot.durationMs > 0L) {
            // viewModelScope is already cancelled here, so persist the final position on a small
            // fire-and-forget scope. A lost write is harmless: the 5 s ticker has a recent copy.
            val repository = progressRepository
            val id = contentId
            val type = contentType
            val name = displayTitle.ifBlank { _state.value.title }
            val episode = episodeId
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    repository.save(
                        contentId = id,
                        contentType = type,
                        title = name,
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        episodeId = episode,
                    )
                }
            }
        }
        engine.release()
        super.onCleared()
    }

    companion object {
        const val ARG_TYPE = "type"
        const val ARG_ID = "id"
        const val ARG_EPISODE_ID = "episodeId"
        const val ARG_TITLE = "title"
        const val ARG_EPISODE_TITLE = "episodeTitle"
        const val ARG_LIVE = "live"

        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
    }
}
