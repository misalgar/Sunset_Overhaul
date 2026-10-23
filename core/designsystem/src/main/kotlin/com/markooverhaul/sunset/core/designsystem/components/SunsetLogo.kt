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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetDisplay

/**
 * Sunset symbol: inverted amber triangle with a horizon cut and negative-space S.
 * Deliberately flat — no neon glow, scanlines or bevels (spec §7.1–7.2).
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
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.96f)
                lineTo(w * 0.04f, h * 0.12f)
                lineTo(w * 0.96f, h * 0.12f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(SunsetColors.AccentPrimary, Color(0xFFFF7A1A)),
                ),
            )
            drawRect(
                color = SunsetColors.BgBase,
                topLeft = Offset(w * 0.18f, h * 0.63f),
                size = Size(w * 0.64f, h * 0.07f),
            )
        }
        Text(
            text = "S",
            color = SunsetColors.BgBase,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
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
