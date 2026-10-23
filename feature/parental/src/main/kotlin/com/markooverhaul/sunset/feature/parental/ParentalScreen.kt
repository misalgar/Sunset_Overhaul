package com.markooverhaul.sunset.feature.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetBackHeader

/**
 * Honest placeholder. The foundation milestone does not implement PIN setup, category locks or
 * session unlock, so this screen never claims that protection is active.
 */
@Composable
fun ParentalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SunsetBackHeader(
                title = stringResource(R.string.parental_title),
                onBack = onBack,
            )
            Text(
                text = stringResource(R.string.settings_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextTertiary,
                modifier = Modifier.padding(horizontal = gutter),
            )
            SunsetEmptyState(
                title = stringResource(R.string.parental_placeholder_title),
                body = stringResource(R.string.parental_placeholder_body),
                actionLabel = stringResource(R.string.parental_back),
                onAction = onBack,
                icon = Icons.Rounded.Lock,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
