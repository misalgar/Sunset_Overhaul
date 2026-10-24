package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markooverhaul.sunset.core.designsystem.components.SunsetPrimaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetDisplay
import com.markooverhaul.sunset.core.ui.R

/**
 * Cinematic hero: 16:9 backdrop, dual scrims, eyebrow, title, metadata, synopsis and two CTAs.
 * Text always sits on a scrim, never directly on busy artwork (spec §7.7, §9.3).
 */
@Composable
fun SunsetHeroBillboard(
    eyebrow: String,
    title: String,
    synopsis: String?,
    meta: List<String>,
    imageUrl: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButtonModifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    height: Dp = 360.dp,
    pageCount: Int = 0,
    currentPage: Int = 0,
) {
    val isTv = LocalIsTelevision.current
    // A short hero (small 720p/1080p TV viewports) drops lines instead of overflowing above the
    // billboard, which used to push the title under the top navigation bar.
    val ultraCompact = isTv && height < 330.dp
    val compact = isTv && height < 370.dp
    val titleLines = when {
        !isTv -> 2
        height >= 440.dp -> 3
        else -> 2
    }
    val synopsisLines = when {
        !isTv -> 2
        ultraCompact -> 0
        compact -> 1
        else -> 2
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(artworkGradient(title)),
    ) {
        SunsetArtwork(
            title = title,
            imageUrl = imageUrl,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        // Top scrim keeps the transparent nav legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.35f to Color.Transparent,
                        0.72f to SunsetColors.BgBase.copy(alpha = 0.86f),
                        1f to SunsetColors.BgBase,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to SunsetColors.BgBase.copy(alpha = 0.92f),
                        0.55f to SunsetColors.BgBase.copy(alpha = 0.35f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = if (isTv) 760.dp else 560.dp)
                .padding(
                    start = if (isTv) 48.dp else 20.dp,
                    end = 24.dp,
                    bottom = when {
                        !isTv -> 24.dp
                        ultraCompact -> 16.dp
                        compact -> 20.dp
                        else -> 32.dp
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(
                when {
                    ultraCompact -> 4.dp
                    compact -> 6.dp
                    else -> 10.dp
                },
            ),
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SunsetColors.AccentPrimary,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = title,
                style = if (isTv) SunsetDisplay.HeroLarge else SunsetDisplay.HeroPhone,
                color = SunsetColors.TextPrimary,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!synopsis.isNullOrBlank() && synopsisLines > 0) {
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetColors.TextSecondary,
                    maxLines = synopsisLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                SunsetPrimaryButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    modifier = primaryButtonModifier,
                )
                if (secondaryLabel != null && onSecondary != null) {
                    SunsetSecondaryButton(text = secondaryLabel, onClick = onSecondary)
                }
            }
            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) SunsetColors.AccentPrimary
                                    else SunsetColors.TextTertiary,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultHeroPrimaryLabel(isResume: Boolean): String =
    if (isResume) stringResource(R.string.action_resume) else stringResource(R.string.action_play)

@Composable
fun HeroEyebrow(typeLabel: String, qualifier: String): String =
    "$typeLabel · $qualifier".uppercase()
