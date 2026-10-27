package com.markooverhaul.sunset.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
import kotlinx.coroutines.delay

/** The three inputs, so TV focus can tell which one is being edited. */
private enum class LoginField { Server, Username, Password }

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
    val keyboardController = LocalSoftwareKeyboardController.current
    val serverFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val rememberFocus = remember { FocusRequester() }
    val showPasswordFocus = remember { FocusRequester() }
    val helpFocus = remember { FocusRequester() }
    val signInFocus = remember { FocusRequester() }
    val demoFocus = remember { FocusRequester() }

    // TV: the fields are plain focus stops, so moving up/down never opens the keyboard. The IME
    // only appears after an explicit Select on the focused field, and Back closes it again.
    var editingField by remember { mutableStateOf<LoginField?>(null) }
    // Which field actually holds Compose focus. The platform drops Compose focus when the TV
    // keyboard closes, and on this device that is the only reliable "Back was pressed" signal:
    // the Fire TV IME consumes Back and never reports IME insets.
    var focusedField by remember { mutableStateOf<LoginField?>(null) }
    val view = LocalView.current

    // A text field swallows the arrow keys for the caret, so the form has its own focus order.
    val focusOrder = remember {
        listOf(
            serverFocus,
            usernameFocus,
            passwordFocus,
            rememberFocus,
            showPasswordFocus,
            helpFocus,
            signInFocus,
            demoFocus,
        )
    }

    fun focusRequesterFor(field: LoginField): FocusRequester = when (field) {
        LoginField.Server -> serverFocus
        LoginField.Username -> usernameFocus
        LoginField.Password -> passwordFocus
    }

    fun startEditing(field: LoginField) {
        if (!isTv) return
        editingField = field
    }

    fun stopEditing() {
        editingField = null
        keyboardController?.hide()
    }

    fun isFocusStopEnabled(stop: FocusRequester): Boolean = when (stop) {
        signInFocus -> state.canSubmit
        demoFocus -> !state.isSubmitting
        else -> true
    }

    /** Moves focus one step up or down the form, skipping stops that cannot take focus. */
    fun moveFocus(current: FocusRequester, delta: Int) {
        var index = focusOrder.indexOf(current)
        if (index < 0) return
        while (true) {
            index += delta
            val next = focusOrder.getOrNull(index) ?: return
            // A disabled button is not a focus stop; without this Up from the demo link dead-ends
            // on the disabled Sign in button and the D-pad looks broken.
            if (!isFocusStopEnabled(next)) continue
            runCatching { next.requestFocus() }
            return
        }
    }

    // On a TV the system IME consumes Back before the app sees it, and it does not report IME
    // insets, so there is no "keyboard closed" callback. Up/Down are the signal: while the keyboard
    // is open it owns the D-pad, so an arrow that reaches the form means the keyboard is closed.
    // Leaving edit mode here is what stops the field from re-opening the keyboard on the way back.
    fun tvFocusNav(current: FocusRequester): Modifier = Modifier
        .onPreviewKeyEvent { event ->
            if (!isTv) return@onPreviewKeyEvent false
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionDown -> {
                    stopEditing()
                    moveFocus(current, 1)
                    true
                }

                Key.DirectionUp -> {
                    stopEditing()
                    moveFocus(current, -1)
                    true
                }

                else -> false
            }
        }

    // TV: Select (or a tap) on a focused field is the only thing that opens the keyboard.
    fun tvFieldModifier(field: LoginField): Modifier = Modifier
        .then(tvFocusNav(focusRequesterFor(field)))
        .onFocusChanged { focusState ->
            // Order-independent: the outgoing field may report "lost" before or after the incoming
            // field reports "gained", so never clear a field that is not the current one.
            if (focusState.isFocused) {
                focusedField = field
            } else if (focusedField == field) {
                focusedField = null
            }
        }
        .onPreviewKeyEvent { event ->
            if (!isTv) return@onPreviewKeyEvent false
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val activate = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter ||
                event.key == Key.Spacebar
            if (activate && editingField != field) {
                startEditing(field)
                true
            } else {
                false
            }
        }
        .pointerInput(isTv) {
            if (!isTv) return@pointerInput
            detectTapGestures { startEditing(field) }
        }

    // The field becomes editable first, then it takes the IME.
    LaunchedEffect(editingField) {
        val field = editingField ?: return@LaunchedEffect
        if (!isTv) return@LaunchedEffect
        delay(60)
        runCatching { focusRequesterFor(field).requestFocus() }
        keyboardController?.show()
    }

    // The Fire TV IME swallows Back and reports no IME insets, so "the keyboard closed" has no
    // callback. Closing it does drop Compose focus, though, so an edited field that loses focus
    // means the viewer dismissed the keyboard: leave edit mode and put the ring back on the field.
    // Without this the field stayed editable and the IME re-opened as soon as focus returned to it.
    // Keyed on focus only: keying on `editingField` would cancel this effect the moment
    // `stopEditing()` clears it, and the focus restore below would never run.
    LaunchedEffect(focusedField) {
        val editing = editingField ?: return@LaunchedEffect
        if (!isTv || focusedField == editing) return@LaunchedEffect
        // IME Next/Previous re-requests focus on the new field within a frame or two, so a
        // mismatch that is still there after this wait is a dismissal, not a field change. (The
        // platform can restore focus to any other field, not just to "nothing", so field identity
        // cannot be used to tell the two apart.)
        delay(400)
        if (editingField != editing || focusedField == editing) return@LaunchedEffect
        stopEditing()
        // Let the field recompose as read-only before it takes focus: focusing a still-editable
        // field is itself a keyboard-show request, which would undo the dismissal we just handled.
        withFrameNanos { }
        withFrameNanos { }
        keyboardController?.hide()
        // The platform also loses the Compose focus target with the keyboard, so keep asking until
        // the field takes it back; a single request can be dropped while the IME window closes.
        repeat(10) {
            if (isTv && !view.isFocused) runCatching { view.requestFocusFromTouch() }
            runCatching { focusRequesterFor(editing).requestFocus() }
            delay(60)
            if (focusedField == editing) return@LaunchedEffect
        }
    }

    // Back closes the keyboard but keeps focus on the field, so the remote still works.
    BackHandler(enabled = isTv && editingField != null) {
        stopEditing()
    }

    LaunchedEffect(isTv) {
        if (isTv) runCatching { serverFocus.requestFocus() }
    }

    BoxWithConstraints(
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
            )
            // Keep the form above the on-screen keyboard instead of letting it cover the fields.
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        // On a TV the form is too tall for one column, so the brand sits beside it.
        val wide = maxWidth >= 700.dp
        val brand: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
        }
        val form: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    // The form is taller than a 1080p TV with the keyboard open, so it must scroll.
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

            SunsetTextField(
                value = state.serverUrl,
                onValueChange = onServerChange,
                label = stringResource(R.string.login_server),
                placeholder = stringResource(R.string.login_server_hint),
                modifier = Modifier
                    .focusRequester(serverFocus)
                    .then(tvFieldModifier(LoginField.Server)),
                imeAction = ImeAction.Next,
                // The IME "Next" key must hand focus to the username field; without this the key
                // does nothing and everything is typed into the server field.
                onImeAction = {
                    if (isTv) {
                        startEditing(LoginField.Username)
                    } else {
                        runCatching { usernameFocus.requestFocus() }
                    }
                },
                readOnly = isTv && editingField != LoginField.Server,
                enabled = !state.isSubmitting,
            )
            SunsetTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.login_username),
                modifier = Modifier
                    .focusRequester(usernameFocus)
                    .then(tvFieldModifier(LoginField.Username)),
                imeAction = ImeAction.Next,
                onImeAction = {
                    if (isTv) {
                        startEditing(LoginField.Password)
                    } else {
                        runCatching { passwordFocus.requestFocus() }
                    }
                },
                readOnly = isTv && editingField != LoginField.Username,
                enabled = !state.isSubmitting,
            )
            SunsetTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.login_password),
                modifier = Modifier
                    .focusRequester(passwordFocus)
                    .then(tvFieldModifier(LoginField.Password)),
                isPassword = true,
                passwordVisible = state.passwordVisible,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (isTv) stopEditing()
                    onSubmit()
                },
                readOnly = isTv && editingField != LoginField.Password,
                enabled = !state.isSubmitting,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(rememberFocus)
                        .then(tvFocusNav(rememberFocus))
                        .clickable(onClick = onToggleRememberMe)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = { onToggleRememberMe() },
                        // The row is the focus stop, so the checkbox must not take focus itself.
                        modifier = Modifier.focusProperties { canFocus = false },
                        colors = CheckboxDefaults.colors(checkedColor = SunsetColors.AccentPrimary),
                    )
                    Text(
                        text = stringResource(R.string.login_remember_me),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.TextSecondary,
                    )
                }
                TextButton(
                    onClick = onTogglePasswordVisible,
                    modifier = Modifier
                        .focusRequester(showPasswordFocus)
                        .then(tvFocusNav(showPasswordFocus)),
                ) {
                    Text(
                        text = stringResource(
                            if (state.passwordVisible) {
                                R.string.login_hide_password_short
                            } else {
                                R.string.login_show_password_short
                            },
                        ),
                        color = SunsetColors.AccentPrimary,
                    )
                }
            }

            TextButton(
                onClick = { showHelp = true },
                modifier = Modifier
                    .focusRequester(helpFocus)
                    .then(tvFocusNav(helpFocus)),
            ) {
                Text(
                    text = stringResource(R.string.login_need_help),
                    color = SunsetColors.AccentPrimary,
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(signInFocus)
                    .then(tvFocusNav(signInFocus)),
            )

            if (demoMode) {
                TextButton(
                    onClick = onDemo,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .focusRequester(demoFocus)
                        .then(tvFocusNav(demoFocus)),
                ) {
                    Text(
                        text = stringResource(R.string.login_demo_action),
                        color = SunsetColors.AccentPrimary,
                    )
                }
                if (!wide) {
                    Text(
                        text = stringResource(R.string.login_demo_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            }
        }

        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    brand()
                }
                Column(modifier = Modifier.weight(1f)) {
                    form()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                brand()
                form()
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
