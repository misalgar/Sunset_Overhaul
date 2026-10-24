package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetChannelRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun PpvRoute(
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PpvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PpvScreen(
        state = state,
        onPlayChannel = onPlayChannel,
        onSelectCategory = viewModel::selectCategory,
        onToggleFavouritesOnly = viewModel::toggleFavouritesOnly,
        onToggleFavourite = viewModel::toggleFavourite,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

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
    val context = LocalContext.current
    val gutter = if (isTv) 48.dp else 16.dp

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "banner") { SunsetDemoBanner() }

        item(key = "header") {
            SunsetSectionHeader(
                title = stringResource(R.string.ppv_title),
                subtitle = state.scheduleLabel ?: stringResource(R.string.ppv_subtitle),
                seeAllLabel = stringResource(R.string.action_refresh),
                onSeeAll = onRefresh,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        item(key = "filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = gutter),
            ) {
                item(key = "all") {
                    CategoryChip(
                        text = stringResource(R.string.ppv_all_events),
                        selected = state.selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
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
        }

        when {
            state.isLoading -> items(count = 5, key = { "skeleton-$it" }) {
                SunsetSkeleton(
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .fillMaxWidth()
                        .height(72.dp),
                )
            }

            state.error != null -> item(key = "error") {
                SunsetErrorState(
                    title = stringResource(R.string.ppv_unavailable),
                    body = providerErrorText(state.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = onRefresh,
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }

            state.visibleChannels.isEmpty() -> item(key = "empty") {
                SunsetEmptyState(
                    title = stringResource(R.string.ppv_empty_title),
                    body = stringResource(R.string.ppv_empty_body),
                    actionLabel = stringResource(R.string.action_refresh),
                    onAction = onRefresh,
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }

            else -> items(state.visibleChannels, key = { it.id }) { channel ->
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
