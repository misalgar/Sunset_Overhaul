package com.markooverhaul.sunset.v2.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetWordmark
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.ui.R

/**
 * Release fail-closed state. This screen must never offer a sign-in field or a mock session: there
 * is no approved provider configuration in the foundation milestone.
 */
@Composable
fun ServiceNotConfiguredScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SunsetWordmark(symbolSize = 48.dp)
            Text(
                text = stringResource(R.string.service_not_configured_title),
                style = MaterialTheme.typography.headlineMedium,
                color = SunsetColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.service_not_configured_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            SunsetSecondaryButton(
                text = stringResource(R.string.action_close),
                onClick = { (context as? Activity)?.finish() },
            )
        }
    }
}
