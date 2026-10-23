package com.markooverhaul.sunset.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
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
import com.markooverhaul.sunset.core.ui.components.SunsetChannelCard
import com.markooverhaul.sunset.core.ui.components.SunsetChannelRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText

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
        onSelectCategory = viewModel::selectCategory,
        onToggleFavouritesOnly = viewModel::toggleFavouritesOnly,
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
    onSelectCategory: (String?) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val context = LocalContext.current
    val visible = state.visibleChannels
    val onNow = visible.take(8)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "banner") { SunsetDemoBanner() }

        item(key = "header") {
            SunsetSectionHeader(
                title = stringResource(R.string.live_title),
                subtitle = stringResource(R.string.live_channel_count, state.channels.size),
                seeAllLabel = stringResource(R.string.action_go_to_guide),
                onSeeAll = onOpenGuide,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        if (state.isLoading) {
            item(key = "loading") {
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(4) {
                        SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
            return@LazyColumn
        }

        if (state.error != null) {
            item(key = "error") {
                SunsetErrorState(
                    title = stringResource(R.string.error_network_title),
                    body = providerErrorText(state.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = onRetry,
                )
            }
            return@LazyColumn
        }

        item(key = "filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = gutter),
            ) {
                item(key = "all") {
                    CategoryChip(
                        text = stringResource(R.string.live_filter_all),
                        selected = state.selectedCategoryId == null && !state.favouritesOnly,
                        onClick = {
                            onSelectCategory(null)
                            if (state.favouritesOnly) onToggleFavouritesOnly()
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
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                SunsetEmptyState(
                    title = stringResource(R.string.live_empty_title),
                    body = stringResource(R.string.live_empty_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
            }
            return@LazyColumn
        }

        item(key = "on-now") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SunsetSectionHeader(
                    title = stringResource(R.string.live_on_now),
                    modifier = Modifier.padding(horizontal = gutter),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = gutter),
                ) {
                    items(onNow, key = { "now-${it.id}" }) { channel ->
                        val programme = state.programmes[channel.id]?.now
                        SunsetChannelCard(
                            channel = channel,
                            nowTitle = programme?.title,
                            timeRange = programme?.let {
                                SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                            },
                            onClick = { onPlayChannel(channel) },
                            progress = programme?.let {
                                val span = (it.endUtcMs - it.startUtcMs).coerceAtLeast(1L)
                                ((System.currentTimeMillis() - it.startUtcMs).toFloat() / span).coerceIn(0f, 1f)
                            },
                            width = if (isTv) 300.dp else 250.dp,
                        )
                    }
                }
            }
        }

        item(key = "channels-header") {
            SunsetSectionHeader(
                title = stringResource(R.string.live_channels),
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        items(visible, key = { it.id }) { channel ->
            val programme = state.programmes[channel.id]
            val isFavourite = channel.id in state.favourites
            SunsetChannelRow(
                channel = channel,
                nowTitle = programme?.now?.title,
                nextTitle = programme?.next?.title,
                timeRange = programme?.now?.let {
                    SunsetFormat.timeRange(context, it.startUtcMs, it.endUtcMs)
                },
                onClick = { onPlayChannel(channel) },
                onToggleFavourite = { onToggleFavourite(channel) },
                isFavourite = isFavourite,
                favouriteLabel = stringResource(R.string.live_favourite),
                unfavouriteLabel = stringResource(R.string.live_unfavourite),
                modifier = Modifier.padding(horizontal = gutter),
            )
        }
    }
}
