package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetIconButton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.ui.R

/** Horizontal category filters with an All chip. Shared by Live, Movies and Series. */
@Composable
fun SunsetFilterRow(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    gutter: Dp = 16.dp,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = gutter, vertical = 4.dp),
    ) {
        item(key = "all") {
            CategoryChip(
                text = stringResource(R.string.filter_all),
                selected = selectedId == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                text = category.name,
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
            )
        }
    }
}

/**
 * True while the synthetic demo catalogue is signed in. Provided once by the app shell so every
 * screen can hide demo-only chrome when a real panel is active.
 */
val LocalDemoMode = androidx.compose.runtime.compositionLocalOf { false }

/** Quiet, dismissible-free banner that makes the demo/offline mode unmistakable. */
@Composable
fun SunsetDemoBanner(modifier: Modifier = Modifier) {
    if (!LocalDemoMode.current) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SunsetColors.BgSurface2)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = SunsetColors.AccentPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.demo_mode_banner),
            style = MaterialTheme.typography.bodySmall,
            color = SunsetColors.TextSecondary,
        )
    }
}

/** Back + title header used by details and secondary destinations on every form factor. */
@Composable
fun SunsetBackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val isTv = LocalIsTelevision.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isTv) 72.dp else 56.dp)
            .padding(horizontal = if (isTv) 32.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SunsetIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = SunsetColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Rounded section surface for settings groups and episode lists. */
@Composable
fun SunsetSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SunsetColors.BgSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(SunsetRadius.Md),
            ),
    ) {
        content()
    }
}
