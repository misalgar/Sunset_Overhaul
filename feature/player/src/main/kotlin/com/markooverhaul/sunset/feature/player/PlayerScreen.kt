package com.markooverhaul.sunset.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.SunsetPrimaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton
import com.markooverhaul.sunset.core.designsystem.components.sunsetFocusRing
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.player.PlaybackFailure
import com.markooverhaul.sunset.core.player.PlaybackState
import com.markooverhaul.sunset.core.player.PlaybackSurface
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import kotlinx.coroutines.delay

@Composable
fun PlayerRoute(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.engine.state.collectAsStateWithLifecycle()

    PlayerScreen(
        state = state,
        playback = playback,
        engine = viewModel.engine,
        onExit = onExit,
        onTogglePlayPause = viewModel::togglePlayPause,
        onSeekBy = viewModel::seekBy,
        onSeekTo = viewModel::seekTo,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Full-screen media surface. On television the D-pad is handled at the root: centre toggles
 * playback, left/right seek 10 seconds, and the system back gesture exits predictably.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    playback: PlaybackState,
    engine: com.markooverhaul.sunset.core.player.PlaybackEngine?,
    onExit: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val focusRequester = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(true) }
    val error = state.resolveError != null || playback.error != null
    val preparing = !error && (state.isResolving || playback.isPreparing)
    var errorAction by remember { mutableStateOf(PlayerErrorAction.Retry) }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // The player root stays focused while the error overlay is visible so the D-pad reliably
    // switches between Retry and Exit. The overlay buttons are visual/touch targets only.
    LaunchedEffect(error) {
        if (error) {
            errorAction = PlayerErrorAction.Retry
            delay(100)
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(playback.isPlaying, controlsVisible) {
        if (controlsVisible && playback.isPlaying && !error) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    fun handleSeek(deltaMs: Long) {
        controlsVisible = true
        onSeekBy(deltaMs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                // Error overlay controls own the D-pad. The focused root switches the selected
                // action and activates it, independent of the native player view.
                if (error) {
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    return@onPreviewKeyEvent when (event.key) {
                        Key.DirectionLeft -> {
                            errorAction = PlayerErrorAction.Retry
                            true
                        }

                        Key.DirectionRight -> {
                            errorAction = PlayerErrorAction.Exit
                            true
                        }

                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (errorAction == PlayerErrorAction.Retry) onRetry() else onExit()
                            true
                        }

                        else -> false
                    }
                }
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        if (event.type == KeyEventType.KeyUp) {
                            controlsVisible = true
                            onTogglePlayPause()
                            true
                        } else {
                            true
                        }
                    }

                    Key.DirectionLeft -> {
                        if (event.type == KeyEventType.KeyDown) handleSeek(-SEEK_STEP_MS)
                        true
                    }

                    Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) handleSeek(SEEK_STEP_MS)
                        true
                    }

                    else -> false
                }
            }
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            },
    ) {
        if (engine != null && !error) {
            PlaybackSurface(
                engine = engine,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // When the controls are hidden, keep the placeholder marker on screen. The controls
        // themselves repeat it in the title block so it is never mistaken for provider video.
        // The marker only applies to the bundled demo clip; a real stream is never labelled demo.
        if (!controlsVisible && !error && playback.isPlaceholder) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = if (isTv) 48.dp else 16.dp, vertical = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(SunsetRadius.Sm),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.player_placeholder),
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            error -> PlayerErrorOverlay(
                message = if (state.resolveError != null) {
                    providerErrorText(state.resolveError)
                } else {
                    stringResource(R.string.player_error_body)
                },
                failure = playback.error,
                live = state.live,
                scheduledStartUtcMs = state.scheduledStartUtcMs,
                selectedAction = errorAction,
                onRetry = onRetry,
                onExit = onExit,
            )

            preparing -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = SunsetColors.AccentPrimary)
                Text(
                    text = stringResource(R.string.player_preparing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetColors.TextSecondary,
                )
            }
        }

        if (!error) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                PlayerControls(
                    title = state.title,
                    subtitle = state.subtitle,
                    live = state.live,
                    playback = playback,
                    isTv = isTv,
                    onExit = onExit,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekBy = ::handleSeek,
                    onSeekTo = onSeekTo,
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    subtitle: String?,
    live: Boolean,
    playback: PlaybackState,
    isTv: Boolean,
    onExit: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SunsetColors.ScrimLeft,
                        Color.Transparent,
                        SunsetColors.ScrimLeft,
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTv) 32.dp else 8.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onExit, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.player_back),
                    tint = SunsetColors.TextPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (playback.isPlaceholder) {
                    Text(
                        text = stringResource(R.string.player_placeholder),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (live) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SunsetColors.SemanticLive, CircleShape),
                    )
                    Text(
                        text = stringResource(R.string.player_live),
                        style = MaterialTheme.typography.labelLarge,
                        color = SunsetColors.TextPrimary,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 32.dp else 20.dp),
        ) {
            if (!live) {
                PlayerControlButton(
                    icon = Icons.Rounded.Replay10,
                    description = stringResource(R.string.player_seek_back),
                    onClick = { onSeekBy(-SEEK_STEP_MS) },
                )
            }
            PlayerControlButton(
                icon = if (playback.ended) {
                    Icons.Rounded.PlayArrow
                } else if (playback.isPlaying) {
                    Icons.Rounded.Pause
                } else {
                    Icons.Rounded.PlayArrow
                },
                description = stringResource(
                    if (playback.isPlaying) R.string.player_pause else R.string.player_play,
                ),
                onClick = onTogglePlayPause,
                size = if (isTv) 84.dp else 68.dp,
            )
            if (!live) {
                PlayerControlButton(
                    icon = Icons.Rounded.Forward10,
                    description = stringResource(R.string.player_seek_forward),
                    onClick = { onSeekBy(SEEK_STEP_MS) },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 48.dp else 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (live) {
                Text(
                    text = stringResource(
                        if (playback.isPlaceholder) R.string.player_live_hint
                        else R.string.player_live_seek_hint,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextSecondary,
                )
            } else {
                val duration = playback.durationMs.coerceAtLeast(0L)
                Slider(
                    value = if (duration > 0L) playback.positionMs.coerceIn(0L, duration).toFloat() else 0f,
                    onValueChange = { value -> onSeekTo(value.toLong()) },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    enabled = duration > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = SunsetColors.AccentPrimary,
                        activeTrackColor = SunsetColors.AccentPrimary,
                        inactiveTrackColor = SunsetColors.TextTertiary.copy(alpha = 0.4f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = SunsetFormat.clock(playback.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                    )
                    Text(
                        text = SunsetFormat.clock(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                    )
                }
            }
            Text(
                text = if (playback.ended) {
                    stringResource(R.string.player_ended)
                } else {
                    stringResource(R.string.player_controls_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(size * 0.28f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SunsetColors.TextPrimary,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PlayerErrorOverlay(
    message: String,
    failure: PlaybackFailure?,
    live: Boolean,
    scheduledStartUtcMs: Long?,
    selectedAction: PlayerErrorAction,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.player_error_title),
                style = MaterialTheme.typography.headlineMedium,
                color = SunsetColors.TextPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextSecondary,
            )
            if (failure != null) {
                Text(
                    text = stringResource(
                        when (failure) {
                            PlaybackFailure.NETWORK -> R.string.error_network_body
                            PlaybackFailure.AUTH -> if (live) {
                                R.string.error_channel_unavailable
                            } else {
                                R.string.error_playback_auth
                            }
                            PlaybackFailure.UNSUPPORTED_FORMAT -> R.string.error_playback_unsupported
                            PlaybackFailure.DECODER -> R.string.error_playback_unsupported
                            PlaybackFailure.SERVER -> R.string.error_generic_body
                            PlaybackFailure.UNKNOWN -> R.string.error_unknown
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextTertiary,
                )
            }
            val scheduledStart = scheduledStartUtcMs
            if (scheduledStart != null && scheduledStart > System.currentTimeMillis()) {
                Text(
                    text = stringResource(
                        R.string.player_event_scheduled,
                        SunsetFormat.time(context, scheduledStart),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextTertiary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val retryShape = RoundedCornerShape(SunsetRadius.Pill)
                val exitShape = RoundedCornerShape(SunsetRadius.Md)
                SunsetPrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    icon = null,
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .sunsetFocusRing(
                            focused = selectedAction == PlayerErrorAction.Retry,
                            shape = retryShape,
                            gap = 2.dp,
                        ),
                )
                SunsetSecondaryButton(
                    text = stringResource(R.string.player_back),
                    onClick = onExit,
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .sunsetFocusRing(
                            focused = selectedAction == PlayerErrorAction.Exit,
                            shape = exitShape,
                            gap = 2.dp,
                        ),
                )
            }
        }
    }
}

private enum class PlayerErrorAction { Retry, Exit }

private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_AUTO_HIDE_MS = 4_000L
