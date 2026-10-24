package com.markooverhaul.sunset.v2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetSectionCard

@Composable
fun MoreScreen(
    onOpenGuide: () -> Unit,
    onOpenPpv: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyList: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "banner") { SunsetDemoBanner() }
            item(key = "header") {
                SunsetSectionHeader(
                    title = stringResource(R.string.more_sheet_title),
                    subtitle = stringResource(R.string.more_subtitle),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "links") {
                SunsetSectionCard(modifier = Modifier.padding(horizontal = gutter)) {
                    Column {
                        MoreRow(
                            icon = Icons.Rounded.DateRange,
                            title = stringResource(R.string.nav_guide),
                            onClick = onOpenGuide,
                        )
                        MoreRow(
                            icon = Icons.Rounded.PlayArrow,
                            title = stringResource(R.string.nav_ppv),
                            onClick = onOpenPpv,
                        )
                        MoreRow(
                            icon = Icons.Rounded.Search,
                            title = stringResource(R.string.nav_search),
                            onClick = onOpenSearch,
                        )
                        MoreRow(
                            icon = Icons.Rounded.Favorite,
                            title = stringResource(R.string.nav_my_list),
                            onClick = onOpenMyList,
                        )
                        MoreRow(
                            icon = Icons.Rounded.Settings,
                            title = stringResource(R.string.nav_settings),
                            onClick = onOpenSettings,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SunsetColors.BgSurface2, RoundedCornerShape(SunsetRadius.Sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SunsetColors.AccentPrimary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SunsetColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SunsetColors.TextTertiary,
            )
        }
    }
}
