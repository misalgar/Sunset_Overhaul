package com.markooverhaul.sunset.v2.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.SunsetPrimaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetSpacing
import com.markooverhaul.sunset.core.ui.R
import kotlin.math.roundToInt

/**
 * App-level update prompt. It checks once per day when the signed-in shell appears and renders over
 * whatever screen is active; the Settings row can trigger a manual check at any time.
 */
@Composable
fun UpdateHost(
    enabled: Boolean,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(enabled) {
        if (enabled) viewModel.checkIfDue()
    }

    val manifest = state.available
    if (manifest == null && !state.downloading && !state.needsPermission) return

    UpdateDialog(
        state = state,
        manifest = manifest,
        onInstall = viewModel::install,
        onDismiss = viewModel::dismiss,
        onOpenSettings = viewModel::openInstallSettings,
    )
}

@Composable
private fun UpdateDialog(
    state: UpdateState,
    manifest: UpdateManifest?,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val mandatory = manifest?.mandatory == true
    val locked = state.downloading || mandatory
    val primaryFocus = remember { FocusRequester() }
    val percent = (state.progress * 100f).roundToInt()

    LaunchedEffect(state.downloading, state.needsPermission, state.error, manifest?.versionCode) {
        if (!state.downloading) {
            runCatching { primaryFocus.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = { if (!locked) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !locked,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(SunsetRadius.Lg))
                .background(SunsetColors.BgSurface2)
                .padding(SunsetSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
        ) {
            Text(
                text = stringResource(R.string.update_title),
                style = MaterialTheme.typography.headlineSmall,
                color = SunsetColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            if (manifest != null) {
                val version = manifest.versionName.ifBlank { manifest.versionCode.toString() }
                Text(
                    text = stringResource(R.string.update_version, version),
                    style = MaterialTheme.typography.titleMedium,
                    color = SunsetColors.AccentPrimary,
                )
                if (manifest.notes.isNotBlank()) {
                    Text(
                        text = manifest.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (mandatory) {
                    Text(
                        text = stringResource(R.string.update_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                    )
                }
            }

            if (state.needsPermission) {
                Text(
                    text = stringResource(R.string.update_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            if (state.downloading) {
                SunsetProgressBar(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.update_downloading, percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = SunsetColors.TextSecondary,
                )
            }

            state.error?.let { error ->
                Text(
                    text = updateErrorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.SemanticError,
                    textAlign = TextAlign.Center,
                )
            }

            SunsetPrimaryButton(
                text = when {
                    state.downloading -> stringResource(R.string.update_downloading, percent)
                    state.needsPermission -> stringResource(R.string.update_open_settings)
                    state.error != null -> stringResource(R.string.action_retry)
                    else -> stringResource(R.string.update_install)
                },
                onClick = when {
                    state.needsPermission -> onOpenSettings
                    else -> onInstall
                },
                icon = null,
                enabled = !state.downloading,
                modifier = Modifier.focusRequester(primaryFocus),
            )

            if (!locked) {
                SunsetSecondaryButton(
                    text = stringResource(R.string.update_later),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun updateErrorText(error: UpdateError): String = stringResource(
    when (error) {
        UpdateError.CheckFailed -> R.string.update_error_check
        UpdateError.DownloadFailed -> R.string.update_error_download
        UpdateError.HashMismatch -> R.string.update_error_hash
        UpdateError.SignatureMismatch -> R.string.update_error_signature
        UpdateError.InvalidPackage -> R.string.update_error_invalid
        UpdateError.InstallFailed -> R.string.update_error_install
    },
)
