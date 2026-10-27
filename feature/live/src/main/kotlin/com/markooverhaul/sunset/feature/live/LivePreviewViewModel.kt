package com.markooverhaul.sunset.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.common.IoDispatcher
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.StreamRequest
import com.markooverhaul.sunset.core.player.PlaybackEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the muted player behind the Live hero. It is a separate engine from the full-screen player,
 * so navigating into a channel never shares a decoder with the preview.
 */
@HiltViewModel
class LivePreviewViewModel @Inject constructor(
    private val provider: ContentProvider,
    val engine: PlaybackEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var currentId: String? = null
    private var prepareJob: Job? = null

    /** Starts (or keeps) a muted preview for [channel]; null stops it. */
    fun preview(channel: LiveChannel?) {
        if (channel == null) {
            stop()
            return
        }
        if (channel.id == currentId) return
        currentId = channel.id
        prepareJob?.cancel()
        prepareJob = viewModelScope.launch {
            try {
                engine.setMuted(true)
                val source = withContext(ioDispatcher) {
                    provider.resolveStream(
                        StreamRequest(
                            contentType = ContentType.LIVE,
                            contentId = channel.id,
                            startPositionMs = 0L,
                            live = true,
                        ),
                    )
                }
                // prepare() creates a fresh player; mute again and never unmute a preview.
                engine.setMuted(true)
                engine.prepare(source, 0L, live = true)
                engine.setMuted(true)
                engine.play()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // A preview failure is silent: the static artwork stays on screen.
                currentId = null
            }
        }
    }

    /** Pauses the preview but keeps the engine, so the next focus can reuse it. */
    fun stop() {
        prepareJob?.cancel()
        prepareJob = null
        currentId = null
        engine.pause()
    }

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }
}
