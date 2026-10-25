package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.components.LiveBadge
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.LiveChannel

/** On Now live card: logo, LIVE badge, channel name, current programme and progress. */
@Composable
fun SunsetChannelCard(
    channel: LiveChannel,
    nowTitle: String?,
    timeRange: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    width: Dp = 250.dp,
) {
    val shape = RoundedCornerShape(SunsetRadius.Md)
    SunsetFocusable(
        onClick = onClick,
        modifier = modifier.width(width),
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SunsetColors.BgSurface2, shape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(SunsetColors.BgSurface3, RoundedCornerShape(SunsetRadius.Sm)),
                contentAlignment = Alignment.Center,
            ) {
                ChannelLogo(name = channel.name, logoUrl = channel.logoUrl, modifier = Modifier.width(72.dp))
                LiveBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
                if (progress != null && progress > 0f) {
                    SunsetProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                color = SunsetColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (nowTitle != null) {
                Text(
                    text = nowTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (timeRange != null) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.TextTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Dense live channel row with now/next and a favourite toggle that never leaves the screen. */
@Composable
fun SunsetChannelRow(
    channel: LiveChannel,
    nowTitle: String?,
    nextTitle: String?,
    timeRange: String?,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    isFavourite: Boolean,
    favouriteLabel: String,
    unfavouriteLabel: String,
    modifier: Modifier = Modifier,
) {
    SunsetFocusable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SunsetRadius.Sm),
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .background(SunsetColors.BgSurface2)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelLogo(name = channel.name, logoUrl = channel.logoUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (nowTitle != null) {
                    Text(
                        text = nowTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (nextTitle != null) {
                    Text(
                        text = nextTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (timeRange != null) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.labelMedium,
                    color = SunsetColors.TextTertiary,
                )
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavourite) unfavouriteLabel else favouriteLabel,
                    tint = if (isFavourite) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                )
            }
        }
    }
}

/**
 * Dense live tile for the 2-column TV grid: a framed 64 dp logo, name, current programme,
 * time + LIVE status, a bottom progress bar and a favourite indicator. Holding Select toggles the
 * favourite without adding a second focus stop.
 */
@Composable
fun SunsetLiveChannelTile(
    channel: LiveChannel,
    nowTitle: String?,
    timeRange: String?,
    nowPlayingLabel: String,
    isFavourite: Boolean,
    favouriteLabel: String,
    unfavouriteLabel: String,
    onPlay: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    // A held Select arrives as repeated KeyDown events. The first repeat toggles the favourite and
    // the clickable's KeyUp click is swallowed, so the channel does not also start playing.
    var longPressHandled by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    SunsetFocusable(
        onClick = {
            if (longPressHandled) {
                longPressHandled = false
            } else {
                onPlay()
            }
        },
        onFocusChanged = { focused -> if (!focused) longPressHandled = false },
        modifier = modifier
            .onPreviewKeyEvent { event ->
                val selectKey = event.key == Key.DirectionCenter || event.key == Key.Enter
                if (selectKey &&
                    event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.repeatCount > 0
                ) {
                    if (!longPressHandled) {
                        longPressHandled = true
                        onToggleFavourite()
                    }
                    true
                } else {
                    false
                }
            },
        shape = shape,
        scaleOnFocus = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SunsetColors.BgSurface2),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FramedChannelLogo(name = channel.name, logoUrl = channel.logoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = SunsetColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = nowTitle ?: nowPlayingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (timeRange != null) {
                            Text(
                                text = timeRange,
                                style = MaterialTheme.typography.labelSmall,
                                color = SunsetColors.TextTertiary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        LiveBadge()
                    }
                }
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavourite) unfavouriteLabel else favouriteLabel,
                    tint = if (isFavourite) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (progress != null) {
                SunsetProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                )
            }
        }
    }
}

/**
 * 64 dp logo framed on a subtle surface so transparent or mismatched artwork reads uniformly.
 */
@Composable
private fun FramedChannelLogo(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(SunsetRadius.Sm),
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = channelInitials(name),
                style = MaterialTheme.typography.titleMedium,
                color = SunsetColors.TextSecondary,
            )
        }
    }
}

private val LeadingEventTime = Regex("""^\s*\d{1,2}:\d{2}\s*""")
private val NonWordCharacters = Regex("""[^A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ]+""")

/** Initials for providers without artwork; ignores event time prefixes such as `07:00`. */
private fun channelInitials(name: String): String {
    val words = name
        .replace(LeadingEventTime, " ")
        .split(NonWordCharacters)
        .filter { word -> word.any { it.isLetter() } }
    return when {
        words.isEmpty() -> name.trim().take(2).uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}
