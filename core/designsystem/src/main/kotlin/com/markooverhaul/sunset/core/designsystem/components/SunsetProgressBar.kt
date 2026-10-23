package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors

/** Resume/completion bar: 3 dp track, amber fill, exposed to accessibility services. */
@Composable
fun SunsetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                if (label != null) contentDescription = label
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = SunsetColors.BgSurface3,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = radius,
        )
        if (clamped > 0f) {
            drawRoundRect(
                color = SunsetColors.AccentPrimary,
                topLeft = Offset.Zero,
                size = Size(size.width * clamped, size.height),
                cornerRadius = radius,
            )
        }
    }
}
