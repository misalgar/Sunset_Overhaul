package com.markooverhaul.sunset.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.data.repository.ReduceMotionSetting
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetBackHeader
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetSectionCard
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenParental: () -> Unit,
    onOpenProfiles: () -> Unit,
    onSignedOut: () -> Unit,
    demoMode: Boolean,
    versionName: String,
    versionCode: Int,
    updateStatus: String,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    SettingsScreen(
        state = state,
        demoMode = demoMode,
        versionLabel = stringResource(R.string.about_version_format, versionName, versionCode),
        updateStatus = updateStatus,
        onBack = onBack,
        onAutoplayChange = viewModel::setAutoplayNext,
        onSubtitlesChange = viewModel::setSubtitlesEnabled,
        onReduceMotionChange = viewModel::setReduceMotion,
        onLanguageChange = viewModel::setLanguage,
        onSignOut = viewModel::signOut,
        onOpenParental = onOpenParental,
        onOpenProfiles = onOpenProfiles,
        onOpenAbout = onOpenAbout,
        onCheckForUpdates = onCheckForUpdates,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    demoMode: Boolean,
    versionLabel: String,
    updateStatus: String,
    onBack: () -> Unit,
    onAutoplayChange: (Boolean) -> Unit,
    onSubtitlesChange: (Boolean) -> Unit,
    onReduceMotionChange: (ReduceMotionSetting) -> Unit,
    onLanguageChange: (String?) -> Unit,
    onSignOut: () -> Unit,
    onOpenParental: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenAbout: () -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    val settings = state.settings

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                SunsetBackHeader(
                    title = stringResource(R.string.settings_title),
                    onBack = onBack,
                )
            }

            if (demoMode) {
                item(key = "demo-banner") {
                    SunsetDemoBanner(modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            item(key = "account-title") {
                SettingsSectionTitle(
                    title = stringResource(R.string.settings_account),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "account") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        SettingsInfoRow(
                            title = stringResource(R.string.settings_signed_in_as),
                            value = state.session?.username ?: stringResource(R.string.demo_user),
                        )
                        SettingsInfoRow(
                            title = stringResource(R.string.settings_plan_status),
                            value = state.account?.status
                                ?: if (demoMode) {
                                    stringResource(R.string.settings_demo_plan)
                                } else {
                                    stringResource(R.string.settings_placeholder)
                                },
                        )
                        state.accountError?.let { error ->
                            SettingsInfoRow(
                                title = stringResource(R.string.error_generic_title),
                                value = providerErrorText(error),
                                valueColor = SunsetColors.SemanticError,
                            )
                        }
                        SettingsActionRow(
                            title = stringResource(R.string.action_sign_out),
                            onClick = { showSignOutDialog = true },
                            titleColor = SunsetColors.SemanticError,
                        )
                    }
                }
            }

            item(key = "playback-title") {
                SettingsSectionTitle(
                    title = stringResource(R.string.settings_playback),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "playback") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_autoplay),
                            supporting = stringResource(R.string.settings_autoplay_hint),
                            checked = settings.autoplayNext,
                            onCheckedChange = onAutoplayChange,
                        )
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_subtitles),
                            checked = settings.subtitlesEnabled,
                            onCheckedChange = onSubtitlesChange,
                        )
                        SettingsInfoRow(
                            title = stringResource(R.string.settings_player_engine),
                            value = stringResource(R.string.settings_player_engine_media3),
                        )
                        SettingsInfoRow(
                            title = stringResource(R.string.settings_audio_language),
                            value = settings.audioLanguage ?: stringResource(R.string.settings_language_system),
                        )
                        SettingsPlaceholderRow(title = stringResource(R.string.settings_aspect_ratio))
                        SettingsPlaceholderRow(title = stringResource(R.string.settings_pip))
                    }
                }
            }

            item(key = "appearance-title") {
                SettingsSectionTitle(
                    title = stringResource(R.string.settings_appearance),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "appearance") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        SettingsChipGroup(
                            title = stringResource(R.string.settings_language),
                            hint = stringResource(R.string.settings_language_restart),
                        ) {
                            CategoryChip(
                                text = stringResource(R.string.settings_language_system),
                                selected = settings.languageTag == null,
                                onClick = { onLanguageChange(null) },
                            )
                            CategoryChip(
                                text = stringResource(R.string.settings_language_english),
                                selected = settings.languageTag == "en",
                                onClick = { onLanguageChange("en") },
                            )
                            CategoryChip(
                                text = stringResource(R.string.settings_language_spanish),
                                selected = settings.languageTag == "es",
                                onClick = { onLanguageChange("es") },
                            )
                        }
                        SettingsChipGroup(
                            title = stringResource(R.string.settings_reduce_motion),
                            hint = stringResource(R.string.settings_reduce_motion_hint),
                        ) {
                            CategoryChip(
                                text = stringResource(R.string.settings_reduce_motion_system),
                                selected = settings.reduceMotion == ReduceMotionSetting.SYSTEM,
                                onClick = { onReduceMotionChange(ReduceMotionSetting.SYSTEM) },
                            )
                            CategoryChip(
                                text = stringResource(R.string.settings_reduce_motion_on),
                                selected = settings.reduceMotion == ReduceMotionSetting.ON,
                                onClick = { onReduceMotionChange(ReduceMotionSetting.ON) },
                            )
                            CategoryChip(
                                text = stringResource(R.string.settings_reduce_motion_off),
                                selected = settings.reduceMotion == ReduceMotionSetting.OFF,
                                onClick = { onReduceMotionChange(ReduceMotionSetting.OFF) },
                            )
                        }
                    }
                }
            }

            item(key = "links-title") {
                SettingsSectionTitle(
                    title = stringResource(R.string.nav_more),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "links") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        SettingsNavRow(
                            title = stringResource(R.string.settings_parental),
                            subtitle = stringResource(R.string.settings_parental_off),
                            onClick = onOpenParental,
                        )
                        SettingsNavRow(
                            title = stringResource(R.string.nav_profiles),
                            onClick = onOpenProfiles,
                        )
                        SettingsNavRow(
                            title = stringResource(R.string.update_check),
                            subtitle = updateStatus,
                            onClick = onCheckForUpdates,
                        )
                        SettingsNavRow(
                            title = stringResource(R.string.settings_about),
                            subtitle = versionLabel,
                            onClick = onOpenAbout,
                        )
                    }
                }
            }

            item(key = "footnote") {
                Text(
                    text = stringResource(R.string.settings_demo_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gutter, vertical = 8.dp),
                )
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.settings_sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        color = SunsetColors.SemanticError,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(SunsetRadius.Lg),
            containerColor = SunsetColors.BgSurface2,
        )
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    SunsetSectionHeader(
        title = title,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
    valueColor: Color = SunsetColors.TextSecondary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextPrimary,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextTertiary,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SunsetColors.AccentOn,
                checkedTrackColor = SunsetColors.AccentPrimary,
                uncheckedThumbColor = SunsetColors.TextTertiary,
                uncheckedTrackColor = SunsetColors.BgSurface3,
                uncheckedBorderColor = SunsetColors.BorderSubtle,
            ),
        )
    }
}

@Composable
private fun SettingsPlaceholderRow(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetColors.TextSecondary,
        )
        Text(
            text = stringResource(R.string.settings_placeholder),
            style = MaterialTheme.typography.bodySmall,
            color = SunsetColors.TextTertiary,
        )
    }
}

@Composable
private fun SettingsChipGroup(
    title: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetColors.TextPrimary,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = SunsetColors.TextTertiary,
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SunsetRadius.Sm),
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetColors.TextPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SunsetColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    titleColor: Color = SunsetColors.TextPrimary,
) {
    Spacer(modifier = Modifier.height(1.dp))
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SunsetRadius.Sm),
        scaleOnFocus = false,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = titleColor,
            )
        }
    }
}
