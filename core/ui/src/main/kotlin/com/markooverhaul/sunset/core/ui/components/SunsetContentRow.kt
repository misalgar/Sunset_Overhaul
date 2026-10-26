package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    firstItemFocusRequester: FocusRequester? = null,
    content: @Composable (T, Modifier) -> Unit,
) {
    if (items.isEmpty()) return
    val isTv = LocalIsTelevision.current
    val state = rememberLazyListState()
    // Focus the card the viewer can actually see in this row. After horizontal scrolling, item 0
    // may not be composed, which used to make Up/Down unable to focus the row at all.
    val firstVisibleIndex by remember {
        derivedStateOf { state.firstVisibleItemIndex }
    }
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
            itemsIndexed(items = items, key = { _, item -> key(item) }) { index, item ->
                val itemModifier = if (
                    index == firstVisibleIndex &&
                    firstItemFocusRequester != null
                ) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }
                content(item, itemModifier)
            }
        }
    }
}
