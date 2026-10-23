package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.LocalReduceMotion
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/** Layout-matched skeleton. Shimmer becomes a static tint under reduced motion (spec §7.8). */
@Composable
fun SunsetSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SunsetRadius.Md),
) {
    val reduceMotion = LocalReduceMotion.current
    val alpha = if (reduceMotion) {
        0.75f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val animated by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeleton-alpha",
        )
        animated
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(SunsetColors.BgSurface3, shape),
    )
}
