package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.R
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/** LIVE is always dot + label, never colour alone (spec §7.3, §10). */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(SunsetColors.SemanticLive.copy(alpha = 0.16f), RoundedCornerShape(SunsetRadius.Sm))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(SunsetColors.SemanticLive, CircleShape),
        )
        Text(
            text = stringResource(R.string.badge_live),
            style = MaterialTheme.typography.labelSmall,
            color = SunsetColors.SemanticLive,
        )
    }
}

/** Small uppercase metadata badge (NEW, HD, 4K, S2). */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SunsetColors.BgSurface3.copy(alpha = 0.92f),
    contentColor: Color = SunsetColors.TextPrimary,
) {
    Text(
        text = text,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(SunsetRadius.Sm))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
    )
}

/** Lock state is icon + label; hidden content still explains why it is hidden. */
@Composable
fun LockBadge(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(SunsetColors.BgBase.copy(alpha = 0.82f), RoundedCornerShape(SunsetRadius.Sm))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = SunsetColors.AccentPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.labelSmall,
            color = SunsetColors.TextPrimary,
        )
    }
}
