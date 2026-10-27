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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.markooverhaul.sunset.core.player.PlaybackEngine
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.LocalContentEntryFocus
import com.markooverhaul.sunset.core.ui.components.SunsetChannelRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetLiveChannelTile
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Width from which the master-detail layout replaces the phone chip row. */
private val WideLayoutMinWidth = 840.dp
private val SidebarWidth = 240.dp
private const val CATEGORY_PREVIEW_DEBOUNCE_MS = 250L

@Composable
fun LiveRoute(
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
    previewViewModel: LivePreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusedChannel by viewModel.focusedChannel.collectAsStateWithLifecycle()
    val previewChannel by viewModel.previewChannel.collectAsStateWithLifecycle()
    val lastPlayedChannelId by viewModel.lastPlayedChannelId.collectAsStateWithLifecycle()

    // Drives the muted preview engine from the rested-on channel, and never while the screen is
    // hidden behind the full-screen player.
    LaunchedEffect(previewChannel?.id) {
        previewViewModel.preview(previewChannel)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        previewViewModel.stop()
    }

    LiveScreen(
        state = state,
        onPlayChannel = { channel ->
            viewModel.onChannelPlayed(channel)
            onPlayChannel(channel)
        },
        onOpenGuide = onOpenGuide,
        onSelectFilter = viewModel::selectFilter,
        onToggleFavourite = viewModel::toggleFavourite,
        onRetry = viewModel::load,
        focusedChannel = focusedChannel,
        previewChannel = previewChannel,
        previewEngine = previewViewModel.engine,
        lastPlayedChannelId = lastPlayedChannelId,
        onChannelFocused = viewModel::onChannelFocused,
        onClearFocus = viewModel::clearFocus,
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
    focusedChannel: LiveChannel? = null,
    previewChannel: LiveChannel? = null,
    previewEngine: PlaybackEngine? = null,
    lastPlayedChannelId: String? = null,
    onChannelFocused: (LiveChannel) -> Unit = {},
    onClearFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val entries = rememberFilterEntries(state)
    // Event feeds switch from "Starts …" to LIVE on their own; a minute tick keeps labels honest.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

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
                    nowMs = nowMs,
                    focusedChannel = focusedChannel,
                    previewChannel = previewChannel,
                    previewEngine = previewEngine,
                    lastPlayedChannelId = lastPlayedChannelId,
                    onChannelFocused = onChannelFocused,
                    onClearFocus = onClearFocus,
                    onPlayChannel = onPlayChannel,
                    onOpenGuide = onOpenGuide,
                    onSelectFilter = onSelectFilter,
                    onToggleFavourite = onToggleFavourite,
                    onRetry = onRetry,
                    modifier = modifier,
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
    nowMs: Long,
    focusedChannel: LiveChannel?,
    previewChannel: LiveChannel?,
    previewEngine: PlaybackEngine?,
    lastPlayedChannelId: String?,
    onChannelFocused: (LiveChannel) -> Unit,
    onClearFocus: () -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenGuide: () -> Unit,
    onSelectFilter: (LiveFilter) -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val sidebarState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val selectedCategoryRequester = remember { FocusRequester() }
    val firstChannelRequester = remember { FocusRequester() }
    val entryFocus = LocalContentEntryFocus.current
    var previewFilter by remember { mutableStateOf<LiveFilter?>(null) }
    var isGridFocused by remember { mutableStateOf(false) }

    val genreLabel = stringResource(R.string.live_group_genres)
    val eventsLabel = stringResource(R.string.live_group_events)
    val countriesLabel = stringResource(R.string.live_group_countries)

    // All/Favourites stay pinned above the scrolling category list so Down from the top nav always
    // has an "All channels" target. Live Events is the first group (see LiveCategoryGroup order).
    val pinnedEntries = entries.filter { it.group == null && it.filter != LiveFilter.Adult }
    val categoryRows = remember(entries, genreLabel, eventsLabel, countriesLabel) {
        buildList {
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

    // Back from the player lands here with the watched channel remembered: scroll it into view and
    // take focus, so the viewer resumes exactly where they were in the category.
    var restoreTargetId by remember { mutableStateOf(lastPlayedChannelId) }
    val restoreRequester = remember { FocusRequester() }
    LaunchedEffect(state.visibleChannels, restoreTargetId) {
        val target = restoreTargetId ?: return@LaunchedEffect
        val index = state.visibleChannels.indexOfFirst { it.id == target }
        if (index < 0) {
            restoreTargetId = null
            return@LaunchedEffect
        }
        delay(80)
        runCatching { gridState.scrollToItem(index) }
        delay(60)
        runCatching { restoreRequester.requestFocus() }
        restoreTargetId = null
    }

    suspend fun focusSelectedCategory() {
        if (state.selectedFilter == LiveFilter.All) {
            (entryFocus ?: selectedCategoryRequester).requestFocusWhenReady()
            return
        }
        val index = categoryRows.indexOfFirst {
            it is LiveSidebarRow.Filter && it.entry.filter == state.selectedFilter
        }
        if (index >= 0) {
            // Only scroll when the selected row is actually off-screen. animateScrollToItem always
            // snaps the row to the top, which made the list visibly jump on every Left.
            val visible = sidebarState.layoutInfo.visibleItemsInfo.any { it.index == index }
            if (!visible) {
                runCatching { sidebarState.animateScrollToItem(index) }
            }
        }
        selectedCategoryRequester.requestFocusWhenReady()
    }

    suspend fun focusFirstChannel() {
        // Let the new filter's items compose before the requester targets index 0.
        delay(60)
        runCatching { gridState.scrollToItem(0) }
        firstChannelRequester.requestFocusWhenReady()
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Full-height sidebar: the hero lives in the right pane, so the category list is never
        // squeezed into the bottom half of the screen.
        Box(
            modifier = Modifier
                .width(SidebarWidth)
                .fillMaxHeight()
                .padding(
                    start = gutter,
                    top = SunsetSpacing.Md,
                    bottom = SunsetSpacing.Md,
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
                verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Xs),
            ) {
                LiveGuideButton(onClick = onOpenGuide)

                Column(verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Xs)) {
                    pinnedEntries.forEach { entry ->
                        LiveSidebarFilterRow(
                            entry = entry,
                            selected = state.selectedFilter == entry.filter,
                            focusRequester = when {
                                entry.filter == LiveFilter.All ->
                                    entryFocus ?: selectedCategoryRequester
                                state.selectedFilter == entry.filter -> selectedCategoryRequester
                                else -> null
                            },
                            onFocused = { previewFilter = entry.filter },
                            onClick = {
                                onSelectFilter(entry.filter)
                                scope.launch { focusFirstChannel() }
                            },
                            onRight = {
                                onSelectFilter(entry.filter)
                                scope.launch { focusFirstChannel() }
                            },
                        )
                    }
                }

                LazyColumn(
                    state = sidebarState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Xs),
                    contentPadding = PaddingValues(bottom = SunsetSpacing.Sm),
                ) {
                    items(categoryRows, key = { it.key }) { row ->
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
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = SunsetSpacing.Lg,
                    end = gutter,
                ),
        ) {
            // The category title and channel count live inside the hero, so the grid gets the
            // vertical space a separate header row used to take.
            SunsetHeroPreviewHeader(
                focusedChannel = focusedChannel,
                previewChannel = previewChannel,
                programme = focusedChannel?.let { state.programmes[it.id] },
                previewEngine = previewEngine,
                nowMs = nowMs,
                selectedCategoryTitle = selectedEntry.label,
                channelCount = selectedEntry.count,
                isGridFocused = isGridFocused,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        .weight(1f)
                        .clipToBounds()
                        .onFocusChanged {
                            isGridFocused = it.hasFocus
                            if (!it.hasFocus) onClearFocus()
                        },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = SunsetSpacing.Xl,
                    ),
                ) {
                    itemsIndexed(
                        items = state.visibleChannels,
                        key = { _, channel -> channel.id },
                    ) { index, channel ->
                        val programme = state.programmes[channel.id]?.now
                        val status = channelStatus(channel, programme, nowMs)
                        SunsetLiveChannelTile(
                            channel = channel,
                            nowTitle = programme?.title,
                            isFavourite = channel.id in state.favourites,
                            favouriteLabel = stringResource(R.string.live_favourite),
                            unfavouriteLabel = stringResource(R.string.live_unfavourite),
                            onPlay = { onPlayChannel(channel) },
                            onToggleFavourite = { onToggleFavourite(channel) },
                            progress = (status as? ChannelStatus.Live)?.progress,
                            startsAtEpochMillis =
                                (status as? ChannelStatus.Upcoming)?.startsAtEpochMillis,
                            startedAtEpochMillis =
                                (status as? ChannelStatus.Live)?.startedAtEpochMillis,
                            onFocusChanged = { isFocused ->
                                if (isFocused) onChannelFocused(channel)
                            },
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(firstChannelRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (channel.id == restoreTargetId) {
                                        Modifier.focusRequester(restoreRequester)
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
                .padding(horizontal = SunsetSpacing.Sm, vertical = SunsetSpacing.Sm),
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
            start = 12.dp,
            end = 8.dp,
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
                .drawBehind {
                    if (selected) {
                        drawRect(
                            color = SunsetColors.AccentPrimary,
                            topLeft = Offset.Zero,
                            size = Size(2.dp.toPx(), this.size.height),
                        )
                    }
                }
                .padding(
                    start = 12.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
        ) {
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
