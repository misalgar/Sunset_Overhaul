package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.components.LiveBadge
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R

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
 * Dense live tile for the 2-column TV grid. The card has a strict 64 dp height so four rows fit
 * above the fold, a 44 dp logo badge, a single-line title, and a status row with LIVE (or the
 * scheduled start) next to the current programme. Holding Select toggles the favourite without
 * adding a second focus stop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SunsetLiveChannelTile(
    channel: LiveChannel,
    nowTitle: String?,
    isFavourite: Boolean,
    favouriteLabel: String,
    unfavouriteLabel: String,
    onPlay: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    startsAtLabel: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    // A held Select arrives as repeated KeyDown events. The first repeat toggles the favourite and
    // the clickable's KeyUp click is swallowed, so the channel does not also start playing.
    var longPressHandled by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var titleOverflows by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    val titleStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = if (channel.name.length > 24) 14.sp else 16.sp,
        lineHeight = 18.sp,
    )
    SunsetFocusable(
        onClick = {
            if (longPressHandled) {
                longPressHandled = false
            } else {
                onPlay()
            }
        },
        onFocusChanged = { isFocused ->
            focused = isFocused
            if (!isFocused) longPressHandled = false
            onFocusChanged(isFocused)
        },
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
        val cardColor = if (focused) Color(0xFF202938) else Color(0xFF161C26)
        val borderColor = if (focused) {
            SunsetColors.FocusRing
        } else {
            Color.White.copy(alpha = 0.08f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(cardColor)
                .border(1.dp, borderColor, shape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SunsetChannelLogoBadge(
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    size = 44.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = channel.name,
                        modifier = if (focused && titleOverflows) {
                            Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        } else {
                            Modifier
                        },
                        style = titleStyle,
                        color = SunsetColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result -> titleOverflows = result.hasVisualOverflow },
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (startsAtLabel != null) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = SunsetColors.TextTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = startsAtLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SunsetColors.TextTertiary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SunsetColors.SemanticLive, CircleShape),
                            )
                            Text(
                                text = nowTitle?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.live_status_live),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (nowTitle.isNullOrBlank()) {
                                    SunsetColors.TextTertiary
                                } else {
                                    SunsetColors.TextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

/**
 * Solid white logo badge so JPEG logos with baked-in white backgrounds look uniform against the
 * deep slate cards and the Live hero.
 */
@Composable
fun SunsetChannelLogoBadge(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
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
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF161B24),
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
