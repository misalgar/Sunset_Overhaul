package com.markooverhaul.sunset.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetWordmark
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetBackHeader
import com.markooverhaul.sunset.core.ui.components.SunsetSectionCard

/**
 * About screen. Version and build metadata are supplied by `:app`; this feature module must not
 * depend on its own generated BuildConfig for values that describe the installed application.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    versionName: String,
    versionCode: Int,
    buildType: String,
    demoMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val buildLabel = stringResource(
        if (demoMode) R.string.about_build_debug else R.string.about_build_release,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                SunsetBackHeader(
                    title = stringResource(R.string.about_title),
                    onBack = onBack,
                )
            }

            item(key = "identity") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SunsetWordmark(symbolSize = 40.dp)
                        Text(
                            text = stringResource(R.string.about_version_format, versionName, versionCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = SunsetColors.TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.about_build_format, buildLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = SunsetColors.TextTertiary,
                        )
                        Text(
                            text = stringResource(R.string.settings_about_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SunsetColors.TextSecondary,
                        )
                    }
                }
            }

            item(key = "provider-title") {
                SunsetSectionHeader(
                    title = stringResource(R.string.about_provider_status),
                    modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                )
            }
            item(key = "provider") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                if (demoMode) R.string.about_provider_demo else R.string.about_provider_blocked,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (demoMode) SunsetColors.SemanticSuccess else SunsetColors.SemanticWarning,
                        )
                        Text(
                            text = stringResource(R.string.about_no_calls_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SunsetColors.TextSecondary,
                        )
                        Text(
                            text = stringResource(R.string.about_build_format, buildType),
                            style = MaterialTheme.typography.bodySmall,
                            color = SunsetColors.TextTertiary,
                        )
                    }
                }
            }

            item(key = "legal-title") {
                SunsetSectionHeader(
                    title = stringResource(R.string.about_legal),
                    modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                )
            }
            item(key = "legal") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        AboutTextBlock(
                            title = stringResource(R.string.settings_tmdb),
                            body = stringResource(R.string.settings_tmdb_body),
                        )
                        AboutTextBlock(
                            title = stringResource(R.string.settings_privacy),
                            body = stringResource(R.string.settings_privacy_body),
                        )
                        AboutTextBlock(
                            title = stringResource(R.string.settings_licences),
                            body = stringResource(R.string.about_licences_body),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutTextBlock(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = SunsetColors.TextPrimary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetColors.TextSecondary,
        )
    }
}
