package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetSpacing
import com.markooverhaul.sunset.core.model.LiveCategoryGroup
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetChannelRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetLiveChannelTile
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Width from which the master-detail layout replaces the phone chip row. */
private val WideLayoutMinWidth = 840.dp
private const val CATEGORY_PREVIEW_DEBOUNCE_MS = 250L

@Composable
fun LiveRoute(
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LiveScreen(
        state = state,
        onPlayChannel = onPlayChannel,
        onOpenGuide = onOpenGuide,
        onSelectFilter = viewModel::selectFilter,
        onToggleFavourite = viewModel::toggleFavourite,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
fun LiveScreen(
    state: LiveUiState,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    onSelectFilter: (LiveFilter) -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val entries = rememberFilterEntries(state)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = isTv || maxWidth >= WideLayoutMinWidth
        val gutter = if (isTv) SunsetSpacing.Xxxl else SunsetSpacing.Lg

        Column(modifier = Modifier.fillMaxSize()) {
            SunsetDemoBanner()

            when {
                state.isLoading -> LiveLoading(gutter = gutter)
                state.error != null -> SunsetErrorState(
                    title = stringResource(R.string.error_network_title),
                    body = providerErrorText(state.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = gutter),
                )

                !state.hasAnyChannel -> SunsetEmptyState(
                    title = stringResource(R.string.live_empty_title),
                    body = stringResource(R.string.live_empty_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                    modifier = Modifier.padding(horizontal = gutter),
                )

                isWide -> LiveWideContent(
                    state = state,
                    entries = entries,
                    gutter = gutter,
                    onPlayChannel = onPlayChannel,
                    onOpenGuide = onOpenGuide,
                    onSelectFilter = onSelectFilter,
                    onToggleFavourite = onToggleFavourite,
                    onRetry = onRetry,
                )

                else -> LiveCompactContent(
                    state = state,
                    entries = entries,
                    gutter = gutter,
                    onPlayChannel = onPlayChannel,
                    onOpenGuide = onOpenGuide,
                    onSelectFilter = onSelectFilter,
                    onToggleFavourite = onToggleFavourite,
                    onRetry = onRetry,
                )
            }
        }
    }
}

/** Sidebar/chip model: All and Favourites first, provider categories, then the explicit adult row. */
private data class LiveFilterEntry(
    val filter: LiveFilter,
    val label: String,
    val count: Int,
    val group: LiveCategoryGroup? = null,
)

@Composable
private fun rememberFilterEntries(state: LiveUiState): List<LiveFilterEntry> {
    val allLabel = stringResource(R.string.live_filter_all)
    val favouritesLabel = stringResource(R.string.live_filter_favourites)
    val adultLabel = state.adultCategoryName ?: stringResource(R.string.live_filter_adult)
    return remember(
        state.categories,
        state.allChannelCount,
        state.favouriteChannelCount,
        state.adultChannelCount,
        state.adultCategoryName,
        allLabel,
        favouritesLabel,
        adultLabel,
    ) {
        buildList {
            add(LiveFilterEntry(LiveFilter.All, allLabel, state.allChannelCount))
            add(LiveFilterEntry(LiveFilter.Favourites, favouritesLabel, state.favouriteChannelCount))
            state.categories.forEach { category ->
                add(
                    LiveFilterEntry(
                        filter = LiveFilter.Category(category.id),
                        label = category.name,
                        count = category.count,
                        group = category.group,
                    ),
                )
            }
            if (state.adultChannelCount > 0) {
                add(LiveFilterEntry(LiveFilter.Adult, adultLabel, state.adultChannelCount))
            }
        }
    }
}

/** One rendered sidebar line: a non-focusable group label or a filter row. */
private sealed interface LiveSidebarRow {
    val key: String

    data class Header(val group: LiveCategoryGroup, val label: String) : LiveSidebarRow {
        override val key: String = "header-${group.name}"
    }

    data class Filter(val entry: LiveFilterEntry) : LiveSidebarRow {
        override val key: String = entry.filter.key
    }
}

@Composable
private fun LiveLoading(gutter: Dp) {
    Column(
        modifier = Modifier.padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
    ) {
        repeat(6) {
            SunsetSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
    }
}

@Composable
private fun LiveWideContent(
    state: LiveUiState,
    entries: List<LiveFilterEntry>,
    gutter: Dp,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    onSelectFilter: (LiveFilter) -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sidebarState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val selectedCategoryRequester = remember { FocusRequester() }
    val firstChannelRequester = remember { FocusRequester() }
    var previewFilter by remember { mutableStateOf<LiveFilter?>(null) }

    val genreLabel = stringResource(R.string.live_group_genres)
    val eventsLabel = stringResource(R.string.live_group_events)
    val countriesLabel = stringResource(R.string.live_group_countries)

    // Pinned rows first, then headers for Genres / Live Events / Countries. The provider's daily
    // sports feeds therefore no longer sit between the user and the standard categories.
    val sidebarRows = remember(entries, genreLabel, eventsLabel, countriesLabel) {
        buildList {
            entries
                .filter { it.group == null && it.filter != LiveFilter.Adult }
                .forEach { add(LiveSidebarRow.Filter(it)) }
            var currentGroup: LiveCategoryGroup? = null
            entries.filter { it.group != null }.forEach { entry ->
                val group = entry.group ?: return@forEach
                if (group != currentGroup) {
                    add(
                        LiveSidebarRow.Header(
                            group = group,
                            label = when (group) {
                                LiveCategoryGroup.Genres -> genreLabel
                                LiveCategoryGroup.LiveEvents -> eventsLabel
                                LiveCategoryGroup.Countries -> countriesLabel
                            },
                        ),
                    )
                    currentGroup = group
                }
                add(LiveSidebarRow.Filter(entry))
            }
            entries.firstOrNull { it.filter == LiveFilter.Adult }?.let {
                add(LiveSidebarRow.Filter(it))
            }
        }
    }

    val selectedEntry = entries.firstOrNull { it.filter == state.selectedFilter } ?: entries.first()
    val selectedRowIndex = sidebarRows.indexOfFirst {
        it is LiveSidebarRow.Filter && it.entry.filter == state.selectedFilter
    }.coerceAtLeast(0)

    // Focusing a category previews it after a short pause, so fast D-pad travel does not rebuild
    // the grid for every row it passes through.
    LaunchedEffect(previewFilter) {
        val target = previewFilter ?: return@LaunchedEffect
        if (target == state.selectedFilter) return@LaunchedEffect
        delay(CATEGORY_PREVIEW_DEBOUNCE_MS)
        onSelectFilter(target)
    }

    // A new filter is a new list; start it from the top even when the preview changed the data
    // without moving focus.
    LaunchedEffect(state.selectedFilter) {
        runCatching { gridState.scrollToItem(0) }
    }

    suspend fun focusSelectedCategory() {
        runCatching { sidebarState.animateScrollToItem(selectedRowIndex) }
        selectedCategoryRequester.requestFocusWhenReady()
    }

    suspend fun focusFirstChannel() {
        // Let the new filter's items compose before the requester targets index 0.
        delay(60)
        runCatching { gridState.scrollToItem(0) }
        firstChannelRequester.requestFocusWhenReady()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.27f)
                .fillMaxHeight()
                .padding(
                    start = gutter,
                    end = SunsetSpacing.Md,
                    top = SunsetSpacing.Md,
                    bottom = SunsetSpacing.Lg,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(SunsetRadius.Lg),
                    )
                    .padding(SunsetSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
            ) {
                LiveGuideButton(onClick = onOpenGuide)

                LazyColumn(
                    state = sidebarState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Xs),
                    contentPadding = PaddingValues(bottom = SunsetSpacing.Sm),
                ) {
                    items(sidebarRows, key = { it.key }) { row ->
                        when (row) {
                            is LiveSidebarRow.Header -> LiveSidebarHeader(label = row.label)
                            is LiveSidebarRow.Filter -> LiveSidebarFilterRow(
                                entry = row.entry,
                                selected = state.selectedFilter == row.entry.filter,
                                focusRequester = if (state.selectedFilter == row.entry.filter) {
                                    selectedCategoryRequester
                                } else {
                                    null
                                },
                                onFocused = { previewFilter = row.entry.filter },
                                onClick = {
                                    onSelectFilter(row.entry.filter)
                                    scope.launch { focusFirstChannel() }
                                },
                                onRight = {
                                    onSelectFilter(row.entry.filter)
                                    scope.launch { focusFirstChannel() }
                                },
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(0.73f)
                .fillMaxHeight()
                .padding(
                    start = SunsetSpacing.Md,
                    end = gutter,
                    top = SunsetSpacing.Xl,
                ),
        ) {
            LiveGridHeader(entry = selectedEntry)

            Spacer(modifier = Modifier.height(SunsetSpacing.Md))

            if (state.visibleChannels.isEmpty()) {
                SunsetEmptyState(
                    title = stringResource(R.string.live_empty_title),
                    body = stringResource(R.string.live_empty_body),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
                    verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
                    contentPadding = PaddingValues(bottom = SunsetSpacing.Xl),
                ) {
                    itemsIndexed(
                        items = state.visibleChannels,
                        key = { _, channel -> channel.id },
                    ) { index, channel ->
                        val programme = state.programmes[channel.id]?.now
                        val progress = programme?.let {
                            val span = (it.endUtcMs - it.startUtcMs).coerceAtLeast(1L)
                            ((System.currentTimeMillis() - it.startUtcMs).toFloat() / span)
                                .coerceIn(0f, 1f)
                        }
                        SunsetLiveChannelTile(
                            channel = channel,
                            nowTitle = programme?.title,
                            timeRange = programme?.let {
                                SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                            },
                            nowPlayingLabel = stringResource(R.string.live_now_playing),
                            isFavourite = channel.id in state.favourites,
                            favouriteLabel = stringResource(R.string.live_favourite),
                            unfavouriteLabel = stringResource(R.string.live_unfavourite),
                            onPlay = { onPlayChannel(channel) },
                            onToggleFavourite = { onToggleFavourite(channel) },
                            progress = progress,
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(firstChannelRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                // First column: Left returns to the selected category instead of
                                // trapping the viewer in the grid.
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionLeft &&
                                        index % 2 == 0
                                    ) {
                                        scope.launch { focusSelectedCategory() }
                                        true
                                    } else {
                                        false
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveGuideButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    val container = if (focused) SunsetColors.AccentPrimary else Color.White.copy(alpha = 0.08f)
    val contentColor = if (focused) SunsetColors.AccentOn else SunsetColors.TextPrimary
    val borderColor = if (focused) SunsetColors.FocusRing else Color.White.copy(alpha = 0.10f)
    SunsetFocusable(
        onClick = onClick,
        onFocusChanged = { focused = it },
        shape = shape,
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Inset the fill so the focus ring stays readable around it.
                .padding(3.dp)
                .background(container, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = SunsetSpacing.Sm, vertical = SunsetSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = SunsetSpacing.Sm,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Tv,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.live_open_guide),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiveSidebarHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = SunsetColors.TextTertiary,
        modifier = Modifier.padding(
            start = SunsetSpacing.Xs,
            top = SunsetSpacing.Sm,
            bottom = SunsetSpacing.Xs,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LiveSidebarFilterRow(
    entry: LiveFilterEntry,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onRight: () -> Unit,
) {
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onRight()
                    true
                } else {
                    false
                }
            },
        shape = shape,
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (selected) SunsetColors.BgSurface3 else Color.Transparent,
                    shape = shape,
                )
                .padding(horizontal = SunsetSpacing.Md, vertical = SunsetSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(
                        if (selected) SunsetColors.AccentPrimary else Color.Transparent,
                    ),
            )
            if (entry.filter == LiveFilter.Favourites) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = entry.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = SunsetColors.TextTertiary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun LiveGridHeader(entry: LiveFilterEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleLarge,
                color = SunsetColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.live_channel_count,
                    entry.count,
                    entry.count,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = stringResource(R.string.live_guide_hint),
            style = MaterialTheme.typography.labelMedium,
            color = SunsetColors.TextTertiary,
            maxLines = 1,
        )
    }
}

/**
 * Phone/tablet-portrait fallback: the same filters as a chip row and the existing dense list. The
 * 2-pane sidebar needs far more width than a phone can give it.
 */
@Composable
private fun LiveCompactContent(
    state: LiveUiState,
    entries: List<LiveFilterEntry>,
    gutter: Dp,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    onSelectFilter: (LiveFilter) -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = SunsetSpacing.Xxxl),
        verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
    ) {
        item(key = "header") {
            SunsetSectionHeader(
                title = stringResource(R.string.live_title),
                subtitle = pluralStringResource(
                    R.plurals.live_channel_count,
                    state.allChannelCount,
                    state.allChannelCount,
                ),
                seeAllLabel = stringResource(R.string.action_go_to_guide),
                onSeeAll = onOpenGuide,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        item(key = "filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
                contentPadding = PaddingValues(horizontal = gutter),
            ) {
                items(entries, key = { it.filter.key }) { entry ->
                    CategoryChip(
                        text = entry.label,
                        selected = state.selectedFilter == entry.filter,
                        onClick = { onSelectFilter(entry.filter) },
                    )
                }
            }
        }

        if (state.visibleChannels.isEmpty()) {
            item(key = "empty") {
                SunsetEmptyState(
                    title = stringResource(R.string.live_empty_title),
                    body = stringResource(R.string.live_empty_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
            }
        } else {
            items(state.visibleChannels, key = { it.id }) { channel ->
                val programme = state.programmes[channel.id]
                SunsetChannelRow(
                    channel = channel,
                    nowTitle = programme?.now?.title,
                    nextTitle = programme?.next?.title,
                    timeRange = programme?.now?.let {
                        SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                    },
                    onClick = { onPlayChannel(channel) },
                    onToggleFavourite = { onToggleFavourite(channel) },
                    isFavourite = channel.id in state.favourites,
                    favouriteLabel = stringResource(R.string.live_favourite),
                    unfavouriteLabel = stringResource(R.string.live_unfavourite),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
        }
    }
}

/** Focus requests race composition after a filter/scroll change; retry until the node exists. */
private suspend fun FocusRequester.requestFocusWhenReady(
    attempts: Int = 12,
    delayMs: Long = 40L,
): Boolean {
    repeat(attempts) {
        val focused = runCatching { requestFocus() }.isSuccess
        if (focused) return true
        delay(delayMs)
    }
    return false
}
