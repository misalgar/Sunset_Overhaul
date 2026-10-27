package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.player.PlaybackEngine
import com.markooverhaul.sunset.core.player.PlaybackSurface
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetChannelLogoBadge
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import java.util.Locale

private val HeroBackground = Color(0xFF141923)

/**
 * Guide-style hero above the channel grid. Compact metadata on the left includes the category
 * breadcrumb (which replaces the old header row), and a 16:9 muted preview bleeds to the right
 * edge so the video is as large as the card allows.
 *
 * The still image shows immediately; the video starts only once [previewChannel] matches the
 * focused channel (the view model applies the 1.5 s rest delay).
 */
@Composable
fun SunsetHeroPreviewHeader(
    focusedChannel: LiveChannel?,
    previewChannel: LiveChannel?,
    programme: LiveProgramme?,
    previewEngine: PlaybackEngine?,
    nowMs: Long,
    selectedCategoryTitle: String,
    channelCount: Int,
    isGridFocused: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 176.dp,
) {
    val context = LocalContext.current
    val now = programme?.now
    val channelCountLabel = pluralStringResource(
        R.plurals.live_channel_count,
        channelCount,
        channelCount,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(HeroBackground)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // Breadcrumb replaces the removed "All channels / 976 channels" header row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${selectedCategoryTitle.uppercase(Locale.getDefault())} • $channelCountLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.AccentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isGridFocused) {
                    Text(
                        text = stringResource(R.string.live_hero_categories_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetColors.TextTertiary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (focusedChannel == null) {
                Text(
                    text = stringResource(R.string.live_hero_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SunsetColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    SunsetChannelLogoBadge(
                        name = focusedChannel.name,
                        logoUrl = focusedChannel.logoUrl,
                        size = 44.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = focusedChannel.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SunsetColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val timeRange = now?.let {
                            SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                        }
                        if (now?.title?.isNotBlank() == true) {
                            // Keep the time range visible even when a long title needs truncating.
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = now.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SunsetColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (timeRange != null) {
                                    Text(
                                        text = timeRange,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SunsetColors.TextTertiary,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.live_status_live),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SunsetColors.TextSecondary,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = now?.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.live_hero_watch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Edge-to-edge 16:9 preview: 176 dp tall, so ~313 dp wide with no outer padding.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 9f)
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

            // Horizontal blend so the metadata card flows into the video, like Freeview/Netflix.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(HeroBackground, Color.Transparent),
                            startX = 0f,
                            endX = 80f,
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    ),
            )

            if (now != null) {
                val span = (now.endUtcMs - now.startUtcMs).coerceAtLeast(1L)
                val progress = ((nowMs - now.startUtcMs).toFloat() / span).coerceIn(0f, 1f)
                val minutesLeft = ((now.endUtcMs - nowMs) / 60_000L).coerceAtLeast(0L).toInt()
                Text(
                    text = stringResource(R.string.live_minutes_left, minutesLeft),
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.18f)),
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
