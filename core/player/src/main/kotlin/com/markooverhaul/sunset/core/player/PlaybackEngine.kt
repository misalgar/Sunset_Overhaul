package com.markooverhaul.sunset.core.player

import com.markooverhaul.sunset.core.model.StreamSource
import kotlinx.coroutines.flow.StateFlow

/** How video is fitted into the surface. Persisted globally by Settings in a later milestone. */
enum class AspectMode { FIT, FILL, ZOOM, RATIO_16_9, RATIO_4_3 }

data class PlaybackState(
    val isPreparing: Boolean = true,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val ended: Boolean = false,
    val isLive: Boolean = false,
    /** True only when the bundled demo clip is playing, so the "not provider content" marker
     *  never covers a real stream. */
    val isPlaceholder: Boolean = false,
    val aspectMode: AspectMode = AspectMode.FIT,
    val speed: Float = 1f,
    val error: PlaybackFailure? = null,
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** Engine-level failure classification. UI copy lives in feature:player, never here. */
enum class PlaybackFailure {
    NETWORK,
    AUTH,
    UNSUPPORTED_FORMAT,
    DECODER,
    SERVER,
    UNKNOWN,
}

/**
 * Playback boundary from spec §13.1, reduced to what the foundation milestone implements.
 *
 * Media3 is the only engine shipped. VLC can be added behind this interface without touching
 * player UI. Methods are main-thread safe; [prepare] suspends until the player is configured.
 */
interface PlaybackEngine {
    val state: StateFlow<PlaybackState>

    suspend fun prepare(source: StreamSource, startPositionMs: Long, live: Boolean)

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setAspect(mode: AspectMode)
    fun selectAudio(trackId: String?)
    fun selectSubtitle(trackId: String?)
    fun release()
}
