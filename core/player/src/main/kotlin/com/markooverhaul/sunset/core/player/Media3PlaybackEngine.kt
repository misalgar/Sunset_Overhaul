package com.markooverhaul.sunset.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.markooverhaul.sunset.core.model.StreamFormat
import com.markooverhaul.sunset.core.model.StreamSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Media3 playback engine.
 *
 * Demo streams (`demo://…`) resolve to the bundled `raw/sample_placeholder.mp4`, so playback is
 * fully offline. Real provider streams are loaded by URI; headers and DRM are intentionally not
 * wired until provider access is approved.
 *
 * One instance is created per player screen and released in `ViewModel.onCleared()`.
 */
class Media3PlaybackEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Observed by [PlaybackSurface] to attach the surface once the engine is ready. */
    internal val playerFlow = MutableStateFlow<ExoPlayer?>(null)

    private var live = false
    private var muted = false
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = playerFlow.value
            _state.update { current ->
                current.copy(
                    isPreparing = when {
                        playbackState == Player.STATE_READY -> false
                        playbackState == Player.STATE_ENDED -> false
                        else -> current.isPreparing
                    },
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isPlaying = player?.isPlaying == true,
                    ended = playbackState == Player.STATE_ENDED,
                    durationMs = knownDuration(player),
                    bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update {
                it.copy(
                    isPlaying = isPlaying,
                    isPreparing = if (isPlaying) false else it.isPreparing,
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isPreparing = false,
                    error = classify(error),
                )
            }
        }
    }

    override suspend fun prepare(source: StreamSource, startPositionMs: Long, live: Boolean) =
        withContext(Dispatchers.Main.immediate) {
            this@Media3PlaybackEngine.live = live
            releasePlayerOnly()

            val player = ExoPlayer.Builder(context).build().also {
                it.addListener(listener)
                it.volume = if (muted) 0f else 1f
                playerFlow.value = it
            }

            val mediaItem = MediaItem.Builder()
                .setUri(resolveUri(source))
                .apply {
                    if (source.format == StreamFormat.HLS) setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                .build()

            _state.value = PlaybackState(
                isPreparing = true,
                isLive = live,
                isPlaceholder = source.url.startsWith(DEMO_SCHEME),
                durationMs = 0L,
            )
            // Host only: the URL path carries the account credentials and must never be logged.
            Log.i(
                "SunsetPlayer",
                "prepare scheme=${mediaItem.localConfiguration?.uri?.scheme} " +
                    "host=${mediaItem.localConfiguration?.uri?.host} " +
                    "port=${mediaItem.localConfiguration?.uri?.port} live=$live",
            )
            player.setMediaItem(mediaItem, if (live) 0L else startPositionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = true
            startPositionTicker()
        }

    override fun play() {
        playerFlow.value?.playWhenReady = true
    }

    override fun pause() {
        playerFlow.value?.playWhenReady = false
    }

    override fun seekTo(positionMs: Long) {
        if (live) return
        val player = playerFlow.value ?: return
        val duration = knownDuration(player)
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        _state.update { it.copy(positionMs = target, ended = false) }
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2f)
        playerFlow.value?.setPlaybackSpeed(clamped)
        _state.update { it.copy(speed = clamped) }
    }

    override fun setAspect(mode: AspectMode) {
        _state.update { it.copy(aspectMode = mode) }
    }

    /** Track selection is a foundation-milestone placeholder; the sample has no alternate tracks. */
    override fun selectAudio(trackId: String?) = Unit

    override fun selectSubtitle(trackId: String?) = Unit

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        playerFlow.value?.volume = if (muted) 0f else 1f
    }

    override fun release() {
        positionJob?.cancel()
        releasePlayerOnly()
        scope.cancel()
    }

    private fun releasePlayerOnly() {
        positionJob?.cancel()
        playerFlow.value?.let { player ->
            player.removeListener(listener)
            player.release()
        }
        playerFlow.value = null
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val player = playerFlow.value
                if (player != null) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = knownDuration(player),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                            isPlaying = player.isPlaying,
                        )
                    }
                }
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun resolveUri(source: StreamSource): Uri =
        if (source.url.startsWith(DEMO_SCHEME)) {
            // android.resource is resolved locally by Media3; no network access.
            Uri.parse("android.resource://${context.packageName}/$SAMPLE_RESOURCE_ID")
        } else {
            Uri.parse(source.url)
        }

    private fun knownDuration(player: ExoPlayer?): Long {
        val duration = player?.duration ?: C.TIME_UNSET
        return if (duration == C.TIME_UNSET || duration < 0L) 0L else duration
    }

    private fun classify(error: PlaybackException): PlaybackFailure = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> PlaybackFailure.NETWORK

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        -> if (httpStatusCode(error) in AUTH_STATUS_CODES) {
            PlaybackFailure.AUTH
        } else {
            PlaybackFailure.SERVER
        }

        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        -> PlaybackFailure.UNSUPPORTED_FORMAT

        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        -> PlaybackFailure.DECODER

        else -> PlaybackFailure.UNKNOWN
    }

    @OptIn(UnstableApi::class)
    private fun httpStatusCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }

    private companion object {
        const val DEMO_SCHEME = "demo://"
        val AUTH_STATUS_CODES = setOf(401, 403)

        /** Kept in sync with the bundled asset. Generated locally; see docs/IMPLEMENTATION_PLAN.md. */
        val SAMPLE_RESOURCE_ID: Int = com.markooverhaul.sunset.core.player.R.raw.sample_placeholder

        const val POSITION_POLL_MS = 500L
    }
}
