package com.markooverhaul.sunset.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.SunsetPrimaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetTextField
import com.markooverhaul.sunset.core.designsystem.components.SunsetWordmark
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    demoMode: Boolean,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }
    LoginScreen(
        state = state,
        demoMode = demoMode,
        onServerChange = viewModel::onServerChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onToggleRememberMe = viewModel::toggleRememberMe,
        onTogglePasswordVisible = viewModel::togglePasswordVisible,
        onSubmit = viewModel::submit,
        onDemo = viewModel::startDemo,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    demoMode: Boolean,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleRememberMe: () -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val serverFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(isTv) {
        if (isTv) runCatching { serverFocus.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SunsetColors.BgBase,
                        SunsetColors.BgSurface,
                        SunsetColors.BgBase,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SunsetWordmark(symbolSize = 48.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.displaySmall,
                color = SunsetColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))

            SunsetTextField(
                value = state.serverUrl,
                onValueChange = onServerChange,
                label = stringResource(R.string.login_server),
                placeholder = stringResource(R.string.login_server_hint),
                modifier = Modifier.focusRequester(serverFocus),
                imeAction = ImeAction.Next,
                // The IME "Next" key must hand focus to the username field; without this the key
                // does nothing and everything is typed into the server field.
                onImeAction = { runCatching { usernameFocus.requestFocus() } },
                enabled = !state.isSubmitting,
            )
            SunsetTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.login_username),
                modifier = Modifier.focusRequester(usernameFocus),
                imeAction = ImeAction.Next,
                onImeAction = { runCatching { passwordFocus.requestFocus() } },
                enabled = !state.isSubmitting,
            )
            SunsetTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.login_password),
                modifier = Modifier.focusRequester(passwordFocus),
                trailingText = stringResource(
                    if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                ),
                onTrailingClick = onTogglePasswordVisible,
                isPassword = true,
                passwordVisible = state.passwordVisible,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = onSubmit,
                enabled = !state.isSubmitting,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleRememberMe)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = { onToggleRememberMe() },
                        colors = CheckboxDefaults.colors(checkedColor = SunsetColors.AccentPrimary),
                    )
                    Text(
                        text = stringResource(R.string.login_remember_me),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.TextSecondary,
                    )
                }
                TextButton(onClick = { showHelp = true }) {
                    Text(
                        text = stringResource(R.string.login_need_help),
                        color = SunsetColors.AccentPrimary,
                    )
                }
            }

            if (state.error != null) {
                Text(
                    text = providerErrorText(state.error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetColors.SemanticError,
                    textAlign = TextAlign.Center,
                )
            }

            if (state.serviceNotConfigured) {
                Text(
                    text = stringResource(R.string.service_not_configured_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = SunsetColors.SemanticWarning,
                    textAlign = TextAlign.Center,
                )
            }

            SunsetPrimaryButton(
                text = stringResource(R.string.login_submit),
                onClick = onSubmit,
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                icon = null,
                modifier = Modifier.fillMaxWidth(),
            )

            if (demoMode) {
                TextButton(onClick = onDemo, enabled = !state.isSubmitting) {
                    Text(
                        text = stringResource(R.string.login_demo_action),
                        color = SunsetColors.AccentPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.login_demo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetColors.TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.login_help_title)) },
            text = { Text(stringResource(R.string.login_help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            shape = RoundedCornerShape(SunsetRadius.Lg),
            containerColor = SunsetColors.BgSurface2,
        )
    }
}
