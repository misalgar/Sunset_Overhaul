package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.LiveBadge
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.LiveChannel

/** On Now live card: logo, LIVE badge, channel number/name, current programme and progress. */
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
    Column(
        modifier = modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SunsetFocusable(onClick = onClick, shape = shape) {
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
                    text = "${channel.number}  ${channel.name}",
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
            Text(
                text = channel.number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = SunsetColors.TextTertiary,
                modifier = Modifier.width(36.dp),
            )
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
