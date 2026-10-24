package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markooverhaul.sunset.core.designsystem.components.LockBadge
import com.markooverhaul.sunset.core.designsystem.components.StatusBadge
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.components.sunsetFocusRing
import com.markooverhaul.sunset.core.designsystem.components.sunsetFocusScale
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/**
 * 2:3 poster card with badges, progress, lock state and a focus ring. Never stretches artwork;
 * missing images get a deterministic gradient (spec §7.7, §10).
 */
@Composable
fun SunsetPosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    year: Int? = null,
    badge: String? = null,
    progress: Float? = null,
    isLocked: Boolean = false,
    lockedLabel: String? = null,
    width: Dp = 156.dp,
    subtitle: String? = null,
) {
    val shape = RoundedCornerShape(SunsetRadius.Md)
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // The whole card (artwork + caption) is one focus target so LazyColumn/Grid bring-into-view
    // reveals the title and metadata too. The ring stays on the artwork so it never covers text.
    Column(
        modifier = modifier
            .width(width)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .sunsetFocusScale(focused)
                .sunsetFocusRing(focused, shape),
        ) {
            SunsetArtwork(
                title = title,
                imageUrl = imageUrl,
                shape = shape,
                contentDescription = title,
                modifier = Modifier.matchParentSize(),
            )
            if (badge != null) {
                StatusBadge(
                    text = badge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
            if (isLocked && lockedLabel != null) {
                LockBadge(
                    contentDescription = lockedLabel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
            if (progress != null && progress > 0f) {
                SunsetProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = SunsetColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = subtitle ?: year?.toString()
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 16:9 continue-watching/next-up card with progress and episode label. */
@Composable
fun SunsetLandscapeCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    progress: Float? = null,
    width: Dp = 280.dp,
) {
    val shape = RoundedCornerShape(SunsetRadius.Md)
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // Caption is part of the focus target so the title stays visible when the rail scrolls.
    Column(
        modifier = modifier
            .width(width)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .sunsetFocusScale(focused)
                .sunsetFocusRing(focused, shape),
        ) {
            SunsetArtwork(
                title = title,
                imageUrl = imageUrl,
                shape = shape,
                contentDescription = title,
                monogramSize = 28.sp,
                modifier = Modifier.matchParentSize(),
            )
            if (progress != null && progress > 0f) {
                SunsetProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = SunsetColors.AccentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = SunsetColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
