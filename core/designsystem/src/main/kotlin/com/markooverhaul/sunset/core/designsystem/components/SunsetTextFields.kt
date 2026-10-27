package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

@Composable
fun SunsetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    /** Spoken label for the trailing action when the visible text is abbreviated. */
    trailingContentDescription: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    supportingText: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    /** TV: the field stays a plain focus stop and never opens the IME until asked to. */
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = { Text(text = label, style = MaterialTheme.typography.bodyLarge) },
        placeholder = placeholder?.let {
            { Text(text = it, color = SunsetColors.TextTertiary) }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(imageVector = it, contentDescription = null, tint = SunsetColors.TextSecondary)
            }
        },
        trailingIcon = if (trailingText != null && onTrailingClick != null) {
            {
                IconButton(
                    onClick = onTrailingClick,
                    modifier = Modifier.semantics {
                        contentDescription = trailingContentDescription ?: trailingText
                    },
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelLarge,
                        color = SunsetColors.AccentPrimary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }, onNext = { onImeAction() }),
        isError = isError,
        supportingText = supportingText?.let {
            { Text(text = it, color = SunsetColors.SemanticError) }
        },
        singleLine = true,
        shape = RoundedCornerShape(SunsetRadius.Sm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SunsetColors.TextPrimary,
            unfocusedTextColor = SunsetColors.TextPrimary,
            focusedContainerColor = SunsetColors.BgSurface2,
            unfocusedContainerColor = SunsetColors.BgSurface2,
            disabledContainerColor = SunsetColors.BgSurface2,
            cursorColor = SunsetColors.AccentPrimary,
            focusedBorderColor = SunsetColors.AccentPrimary,
            unfocusedBorderColor = SunsetColors.BorderSubtle,
            focusedLabelColor = SunsetColors.AccentPrimary,
            unfocusedLabelColor = SunsetColors.TextSecondary,
        ),
    )
}

/** Search entry with clear affordance; debounce belongs in the feature, not the component. */
@Composable
fun SunsetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = SunsetColors.TextTertiary) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = SunsetColors.TextSecondary)
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = clearDescription,
                        tint = SunsetColors.TextSecondary,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(SunsetRadius.Sm),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Some TV keyboards send Next/Done/Go instead of Search. All of them submit the query so
        // the remote can never get stuck re-opening the keyboard on a focus traversal action.
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() },
            onNext = { onSearch() },
            onDone = { onSearch() },
            onGo = { onSearch() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SunsetColors.TextPrimary,
            unfocusedTextColor = SunsetColors.TextPrimary,
            focusedContainerColor = SunsetColors.BgSurface2,
            unfocusedContainerColor = SunsetColors.BgSurface2,
            cursorColor = SunsetColors.AccentPrimary,
            focusedBorderColor = SunsetColors.AccentPrimary,
            unfocusedBorderColor = SunsetColors.BorderSubtle,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
