package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

data class SunsetNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

data class SunsetNavAction(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Large-screen navigation: 64 dp (72 dp TV), transparent or solid, 5 items + icon actions.
 * The shell overlays this above content; all items are D-pad reachable.
 */
@Composable
fun SunsetTopBar(
    items: List<SunsetNavItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    actions: List<SunsetNavAction>,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    isTelevision: Boolean = false,
    solid: Boolean = true,
    brand: @Composable () -> Unit = { SunsetWordmark() },
) {
    val background = if (solid) SunsetColors.BgSurface else SunsetColors.ScrimClear
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isTelevision) 72.dp else 64.dp)
            .background(background)
            .padding(horizontal = if (isTelevision) 48.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        brand()
        Spacer(modifier = Modifier.width(if (isTelevision) 40.dp else 24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                TopNavItem(
                    item = item,
                    selected = item.key == selectedKey,
                    onClick = { onSelect(item.key) },
                    isTelevision = isTelevision,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            actions.forEach { action ->
                SunsetIconButton(
                    icon = action.icon,
                    contentDescription = action.label,
                    onClick = { onAction(action.key) },
                    tint = SunsetColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun TopNavItem(
    item: SunsetNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    isTelevision: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val textColor by animateColorAsState(
        targetValue = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextSecondary,
        label = "top-nav-color",
    )
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .sunsetFocusRing(focused, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isTelevision) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) 24.dp else 0.dp)
                .background(SunsetColors.AccentPrimary, RoundedCornerShape(1.dp)),
        )
    }
}

/**
 * Phone navigation: 64 dp + system inset, five items, amber active state (spec §8.3).
 */
@Composable
fun SunsetBottomBar(
    items: List<SunsetNavItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SunsetColors.BgSurface)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(SunsetRadius.Md)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp)
                    .clip(shape)
                    .onFocusChanged { focused = it.isFocused }
                    .sunsetFocusRing(focused, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(item.key) },
                    )
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
