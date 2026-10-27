package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetSpacing
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.LocalContentEntryFocus
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetLiveChannelTile
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import kotlinx.coroutines.delay

@Composable
fun PpvRoute(
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PpvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PpvScreen(
        state = state,
        onPlayChannel = { channel ->
            viewModel.onChannelPlayed(channel)
            onPlayChannel(channel)
        },
        onSelectCategory = viewModel::selectCategory,
        onToggleFavouritesOnly = viewModel::toggleFavouritesOnly,
        onToggleFavourite = viewModel::toggleFavourite,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

/**
 * Pay per view uses the same tiles as Live TV so events carry their start time, live status and
 * EPG progress instead of a bare channel name.
 */
@Composable
fun PpvScreen(
    state: PpvUiState,
    onPlayChannel: (LiveChannel) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val entryFocus = LocalContentEntryFocus.current

    // Event feeds start on their own; a minute tick keeps "Starts …"/LIVE honest.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SunsetDemoBanner()

        SunsetSectionHeader(
            title = stringResource(R.string.ppv_title),
            subtitle = state.scheduleLabel ?: stringResource(R.string.ppv_subtitle),
            seeAllLabel = stringResource(R.string.action_refresh),
            onSeeAll = onRefresh,
            modifier = Modifier.padding(horizontal = gutter),
        )

        Spacer(modifier = Modifier.height(SunsetSpacing.Md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = gutter),
        ) {
            item(key = "all") {
                CategoryChip(
                    text = stringResource(R.string.ppv_all_events),
                    selected = state.selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    modifier = if (entryFocus != null) {
                        Modifier.focusRequester(entryFocus)
                    } else {
                        Modifier
                    },
                )
            }
            items(state.categories, key = { it.id }) { category ->
                CategoryChip(
                    text = category.name,
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onSelectCategory(category.id) },
                )
            }
            item(key = "favourites") {
                CategoryChip(
                    text = stringResource(R.string.live_filter_favourites),
                    selected = state.favouritesOnly,
                    onClick = onToggleFavouritesOnly,
                )
            }
        }

        Spacer(modifier = Modifier.height(SunsetSpacing.Md))

        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gutter),
                verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
            ) {
                repeat(5) {
                    SunsetSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                    )
                }
            }

            state.error != null -> SunsetErrorState(
                title = stringResource(R.string.ppv_unavailable),
                body = providerErrorText(state.error),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onRefresh,
                modifier = Modifier.padding(horizontal = gutter),
            )

            state.visibleChannels.isEmpty() -> SunsetEmptyState(
                title = stringResource(R.string.ppv_empty_title),
                body = stringResource(R.string.ppv_empty_body),
                actionLabel = stringResource(R.string.action_refresh),
                onAction = onRefresh,
                modifier = Modifier.padding(horizontal = gutter),
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = gutter,
                    end = gutter,
                    bottom = SunsetSpacing.Xl,
                ),
                horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
            ) {
                items(state.visibleChannels, key = { it.id }) { channel ->
                    val programme = state.programmes[channel.id]?.now
                    val status = rememberChannelStatus(channel, programme, nowMs)
                    SunsetLiveChannelTile(
                        channel = channel,
                        nowTitle = programme?.title,
                        isFavourite = channel.id in state.favourites,
                        favouriteLabel = stringResource(R.string.live_favourite),
                        unfavouriteLabel = stringResource(R.string.live_unfavourite),
                        onPlay = { onPlayChannel(channel) },
                        onToggleFavourite = { onToggleFavourite(channel) },
                        progress = status.progress,
                        startsAtLabel = status.startsAtLabel,
                    )
                }
            }
        }
    }
}
