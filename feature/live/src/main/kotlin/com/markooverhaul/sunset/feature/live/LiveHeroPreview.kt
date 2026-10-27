package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.player.PlaybackEngine
import com.markooverhaul.sunset.core.player.PlaybackSurface
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.util.SunsetFormat

/**
 * Guide-style hero above the channel grid: focused channel metadata on the left and a 16:9 preview
 * on the right. The still image shows immediately; the muted video only starts once
 * [previewChannel] matches the focused channel (the view model applies the rest delay).
 */
@Composable
fun SunsetHeroPreviewHeader(
    focusedChannel: LiveChannel?,
    previewChannel: LiveChannel?,
    programme: LiveProgramme?,
    previewEngine: PlaybackEngine?,
    nowMs: Long,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    val context = LocalContext.current
    val now = programme?.now
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                color = Color(0xFF10141B),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            if (focusedChannel == null) {
                Text(
                    text = stringResource(R.string.live_hero_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SunsetColors.TextTertiary,
                )
            } else {
                Text(
                    text = focusedChannel.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SunsetColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val timeRange = now?.let {
                    SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                }
                val programmeLine = listOfNotNull(
                    now?.title?.takeIf { it.isNotBlank() },
                    timeRange,
                ).joinToString("  ·  ")
                if (programmeLine.isNotBlank()) {
                    Text(
                        text = programmeLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                now?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
        ) {
            val playing = focusedChannel != null &&
                previewChannel?.id == focusedChannel.id &&
                previewEngine != null
            when {
                focusedChannel == null -> Unit

                playing -> PlaybackSurface(
                    engine = previewEngine,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> {
                    val image = now?.iconUrl?.takeIf { it.isNotBlank() }
                        ?: focusedChannel.logoUrl?.takeIf { it.isNotBlank() }
                    if (image != null) {
                        AsyncImage(
                            model = image,
                            contentDescription = null,
                            contentScale = if (now?.iconUrl != null) {
                                ContentScale.Crop
                            } else {
                                ContentScale.Fit
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (now?.iconUrl != null) 0.dp else 28.dp),
                        )
                    }
                }
            }

            // Bottom gradient with a slim progress line for the live programme.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
            if (now != null) {
                val span = (now.endUtcMs - now.startUtcMs).coerceAtLeast(1L)
                val progress = ((nowMs - now.startUtcMs).toFloat() / span).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(SunsetColors.AccentPrimary),
                    )
                }
            }
        }
    }
}
