package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetDisplay

/**
 * Sunset symbol: an inverted amber triangle with a negative-space sun. Flat and legible at TV
 * distance — no tiny text, cut-lines or bevels (spec §7.1–7.2).
 */
@Composable
fun SunsetSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val triangle = Path().apply {
                moveTo(w * 0.5f, h * 0.96f)
                lineTo(w * 0.04f, h * 0.12f)
                lineTo(w * 0.96f, h * 0.12f)
                close()
            }
            val sun = Path().apply {
                addOval(
                    Rect(
                        left = w * 0.35f,
                        top = h * 0.31f,
                        right = w * 0.65f,
                        bottom = h * 0.61f,
                    ),
                )
            }
            drawPath(
                path = Path.combine(PathOperation.Difference, triangle, sun),
                brush = Brush.verticalGradient(
                    listOf(SunsetColors.AccentPrimary, Color(0xFFFF7A1A)),
                ),
            )
        }
    }
}

/** Symbol + SUNSET wordmark, used on splash, login and the large-screen nav bar. */
@Composable
fun SunsetWordmark(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 32.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SunsetSymbol(size = symbolSize)
        Text(
            text = "SUNSET",
            style = SunsetDisplay.Wordmark,
            color = SunsetColors.TextPrimary,
        )
    }
}
