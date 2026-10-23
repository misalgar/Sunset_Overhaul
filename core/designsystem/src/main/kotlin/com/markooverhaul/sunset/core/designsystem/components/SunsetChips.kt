package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/** Filter chip: 36 dp tall, amber fill when selected, focus ring on TV. */
@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SunsetRadius.Pill)
    SunsetFocusable(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        shape = shape,
        scaleOnFocus = false,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (selected) SunsetColors.AccentPrimary else SunsetColors.BgSurface2,
                    shape = shape,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SunsetColors.AccentOn else SunsetColors.TextSecondary,
            )
        }
    }
}

/** Compact text pill used for metadata (genre, certification). */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SunsetColors.TextSecondary,
) {
    Text(
        text = text,
        modifier = modifier
            .background(SunsetColors.BgSurface3, RoundedCornerShape(SunsetRadius.Sm))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}
