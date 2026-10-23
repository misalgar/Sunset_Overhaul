package com.markooverhaul.sunset.core.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Binds an engine to a Compose surface without leaking Media3 types into feature modules.
 * Controllers are supplied by the player screen so touch and D-pad behaviour stay consistent.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackSurface(
    engine: PlaybackEngine,
    modifier: Modifier = Modifier,
) {
    val state by engine.state.collectAsState()
    val player = if (engine is Media3PlaybackEngine) {
        engine.playerFlow.collectAsState().value
    } else {
        null
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setBackgroundColor(android.graphics.Color.BLACK)
                keepScreenOn = true
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = when (state.aspectMode) {
                AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                AspectMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                AspectMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
    )
}
