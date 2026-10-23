package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/**
 * Primary pill action, amber with dark text; 48 dp minimum target (spec §10).
 * Focus/hover brightens the fill and adds the TV focus ring so D-pad selection is unmistakable.
 */
@Composable
fun SunsetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.PlayArrow,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(SunsetRadius.Pill)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val highlighted = isFocused || isHovered
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .sunsetFocusRing(isFocused, shape, gap = 2.dp),
        enabled = enabled && !loading,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isPressed -> SunsetColors.AccentPressed
                highlighted -> SunsetColors.AccentHover
                else -> SunsetColors.AccentPrimary
            },
            contentColor = SunsetColors.AccentOn,
            disabledContainerColor = SunsetColors.BgSurface3,
            disabledContentColor = SunsetColors.TextTertiary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = SunsetColors.AccentOn,
                    strokeWidth = 2.dp,
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Secondary action: 12 dp rounded rectangle, quiet outline. Focus/hover fills the surface and
 * switches the outline to the focus colour (spec §7.9: focus is never colour-only).
 */
@Composable
fun SunsetSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(SunsetRadius.Md)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val highlighted = isFocused || isHovered
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .sunsetFocusRing(isFocused, shape, gap = 2.dp),
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (highlighted) SunsetColors.BgSurface3 else Color.Transparent,
            contentColor = if (highlighted) SunsetColors.AccentPrimary else SunsetColors.TextPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = SunsetColors.TextTertiary,
        ),
        border = BorderStroke(1.dp, SunsetColors.BorderSubtle),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

/** 48 dp icon action with a mandatory accessible label and a visible focus ring on TV. */
@Composable
fun SunsetIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SunsetColors.TextPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .sunsetFocusRing(isFocused, CircleShape, gap = 2.dp),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) SunsetColors.AccentPrimary else tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
