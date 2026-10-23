package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision

/**
 * Horizontal rail with header, peek and stable keys. Renders nothing when empty so the screen never
 * shows a blank row (spec §9.3 acceptance).
 */
@Composable
fun <T> SunsetContentRow(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    seeAllLabel: String? = null,
    onSeeAll: (() -> Unit)? = null,
    gutter: Dp = 16.dp,
    itemSpacing: Dp = 16.dp,
    content: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    val isTv = LocalIsTelevision.current
    val state = rememberLazyListState()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SunsetSectionHeader(
            title = title,
            seeAllLabel = seeAllLabel,
            onSeeAll = onSeeAll,
            modifier = Modifier.padding(horizontal = gutter),
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            // Trailing padding leaves a visible peek of the next card (spec §7.5).
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter + if (isTv) 96.dp else 48.dp,
            ),
        ) {
            items(items = items, key = { key(it) }) { item ->
                content(item)
            }
        }
    }
}
