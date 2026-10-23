package com.markooverhaul.sunset.feature.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
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

/** Profiles are modelled from v1.0 but the picker ships in v1.2; this screen says so plainly. */
@Composable
fun ProfilesScreen(
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
                title = stringResource(R.string.profiles_title),
                onBack = onBack,
            )
            Text(
                text = stringResource(R.string.settings_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextTertiary,
                modifier = Modifier.padding(horizontal = gutter),
            )
            SunsetEmptyState(
                title = stringResource(R.string.profiles_placeholder_title),
                body = stringResource(R.string.profiles_placeholder_body),
                actionLabel = stringResource(R.string.profiles_back),
                onAction = onBack,
                icon = Icons.Rounded.Person,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
