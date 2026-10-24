package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors

/** Deterministic, offline-safe placeholder gradient. Artwork is never stretched. */
fun artworkGradient(seed: String): Brush {
    val hash = seed.hashCode()
    val hue = (28 + (hash % 34)).toFloat().let { if (it < 0) it + 360 else it } // warm ambers/browns
    val secondaryHue = (hue + 24f) % 360f
    val top = Color.hsl(hue, 0.52f, 0.27f)
    val bottom = Color.hsl(secondaryHue, 0.38f, 0.10f)
    return Brush.linearGradient(listOf(top, bottom, SunsetColors.BgBase))
}

/**
 * Poster/backdrop primitive. Provider artwork loads through Coil when a URL exists; otherwise a
 * deterministic gradient plus title monogram prevents layout shift and keeps the demo offline.
 */
@Composable
fun SunsetArtwork(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    monogramSize: androidx.compose.ui.unit.TextUnit = 40.sp,
) {
    val gradient = remember(title) { artworkGradient(title) }
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = title.take(2).uppercase(),
                color = SunsetColors.TextPrimary.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold,
                fontSize = monogramSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

/** Small 1:1 channel logo tile with a monogram fallback. */
@Composable
fun ChannelLogo(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 6f))
            .background(SunsetColors.BgSurface3),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.84f),
            )
        } else {
            Text(
                text = name.take(2).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = SunsetColors.TextSecondary,
            )
        }
    }
}
