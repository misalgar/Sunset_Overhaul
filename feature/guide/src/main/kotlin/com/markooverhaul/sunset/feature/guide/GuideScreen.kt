package com.markooverhaul.sunset.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.ChannelLogo
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText

private const val DAY_MS = 86_400_000L

@Composable
fun GuideRoute(
    onPlayChannel: (LiveChannel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GuideScreen(
        state = state,
        onPlayChannel = onPlayChannel,
        onBack = onBack,
        onSelectDay = viewModel::selectDay,
        onToggleFavouritesOnly = viewModel::toggleFavouritesOnly,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
fun GuideScreen(
    state: GuideUiState,
    onPlayChannel: (LiveChannel) -> Unit,
    onBack: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val context = LocalContext.current
    val dayStart = GuideViewModel.startOfDay(state.selectedDay)
    val dayEnd = dayStart + DAY_MS
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "banner") { SunsetDemoBanner() }

        item(key = "header") {
            SunsetSectionHeader(
                title = stringResource(R.string.guide_title),
                modifier = Modifier.padding(horizontal = gutter),
                seeAllLabel = stringResource(R.string.action_back),
                onSeeAll = onBack,
            )
        }

        item(key = "days") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = gutter),
            ) {
                item(key = "today") {
                    CategoryChip(
                        text = stringResource(R.string.guide_today),
                        selected = state.selectedDay == 0,
                        onClick = { onSelectDay(0) },
                    )
                }
                item(key = "tomorrow") {
                    CategoryChip(
                        text = stringResource(R.string.guide_tomorrow),
                        selected = state.selectedDay == 1,
                        onClick = { onSelectDay(1) },
                    )
                }
                item(key = "favourites") {
                    CategoryChip(
                        text = stringResource(R.string.guide_favourites),
                        selected = state.favouritesOnly,
                        onClick = onToggleFavouritesOnly,
                    )
                }
            }
        }

        when {
            state.isLoading -> item(key = "loading") {
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(4) {
                        SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(96.dp))
                    }
                }
            }

            state.error != null -> item(key = "error") {
                SunsetErrorState(
                    title = stringResource(R.string.guide_data_unavailable),
                    body = providerErrorText(state.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = onRetry,
                )
            }

            state.visibleChannels.isEmpty() -> item(key = "empty") {
                SunsetEmptyState(
                    title = stringResource(R.string.guide_no_channels),
                    body = stringResource(R.string.guide_empty_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
            }

            else -> {
                items(state.visibleChannels, key = { it.id }) { channel ->
                    val programmes = state.programmes.filter {
                        it.channelId == channel.id && it.startUtcMs >= dayStart && it.startUtcMs < dayEnd
                    }
                    ChannelGuideBlock(
                        channel = channel,
                        programmes = programmes,
                        now = now,
                        gutter = gutter,
                        isTv = isTv,
                        onPlayChannel = onPlayChannel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelGuideBlock(
    channel: LiveChannel,
    programmes: List<EpgProgramme>,
    now: Long,
    gutter: androidx.compose.ui.unit.Dp,
    isTv: Boolean,
    onPlayChannel: (LiveChannel) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelLogo(name = channel.name, logoUrl = channel.logoUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${channel.number}  ${channel.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (programmes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.live_no_guide),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                    )
                }
            }
        }

        programmes.forEach { programme ->
            val isNow = now >= programme.startUtcMs && now < programme.endUtcMs
            val shape = RoundedCornerShape(SunsetRadius.Sm)
            SunsetFocusable(
                onClick = { onPlayChannel(channel) },
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                scaleOnFocus = false,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isNow) SunsetColors.BgSurface3 else SunsetColors.BgSurface2)
                        .padding(horizontal = 12.dp, vertical = if (isTv) 14.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = SunsetFormat.time(context, programme.startUtcMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = SunsetColors.TextTertiary,
                        modifier = Modifier.width(64.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = programme.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = SunsetColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = SunsetFormat.timeRange(context, programme.startUtcMs, programme.endUtcMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = SunsetColors.TextTertiary,
                        )
                    }
                    if (isNow) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SunsetColors.SemanticLive, CircleShape),
                            )
                            Text(
                                text = stringResource(R.string.guide_now),
                                style = MaterialTheme.typography.labelSmall,
                                color = SunsetColors.SemanticLive,
                            )
                        }
                    }
                }
            }
        }
    }
}
