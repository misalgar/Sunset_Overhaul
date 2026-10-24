package com.markooverhaul.sunset.feature.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetFilterRow
import com.markooverhaul.sunset.core.ui.components.LocalDemoMode
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun SeriesRoute(
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val series = viewModel.series.collectAsLazyPagingItems()
    SeriesScreen(
        state = state,
        series = series,
        onSelectCategory = viewModel::selectCategory,
        onOpenDetails = onOpenDetails,
        modifier = modifier,
    )
}

@Composable
fun SeriesScreen(
    state: SeriesUiState,
    series: LazyPagingItems<SeriesSummary>,
    onSelectCategory: (String?) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    // Six columns on TV keep the poster plus its two-line caption inside the viewport height.
    val posterWidth = if (isTv) 140.dp else 150.dp
    val refresh = series.loadState.refresh

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (isTv) 140.dp else 150.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "banner", span = { GridItemSpan(maxLineSpan) }) {
            SunsetDemoBanner()
        }
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.series_title),
                style = MaterialTheme.typography.displaySmall,
                color = SunsetColors.TextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            SunsetFilterRow(
                categories = state.categories,
                selectedId = state.selectedCategoryId,
                onSelect = onSelectCategory,
                gutter = 0.dp,
            )
        }

        if (state.error != null) {
            item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                SunsetErrorState(
                    title = stringResource(R.string.error_network_title),
                    body = providerErrorText(state.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = { series.retry() },
                )
            }
        } else if (refresh is LoadState.Loading && series.itemCount == 0) {
            items(count = 6, key = { "skeleton-$it" }) {
                SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(posterWidth * 1.5f))
            }
        } else if (refresh is LoadState.Error) {
            item(key = "refresh-error", span = { GridItemSpan(maxLineSpan) }) {
                SunsetErrorState(
                    title = stringResource(R.string.error_generic_title),
                    body = providerErrorText(refresh.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = { series.retry() },
                )
            }
        } else if (series.itemCount == 0) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                SunsetEmptyState(
                    title = stringResource(
                        if (LocalDemoMode.current) R.string.series_empty_title
                        else R.string.series_provider_unavailable_title,
                    ),
                    body = stringResource(
                        if (LocalDemoMode.current) R.string.series_empty_body
                        else R.string.series_provider_unavailable_body,
                    ),
                    actionLabel = if (LocalDemoMode.current) stringResource(R.string.action_retry) else null,
                    onAction = if (LocalDemoMode.current) ({ series.retry() }) else null,
                )
            }
        }

        items(
            count = series.itemCount,
            key = series.itemKey { it.id },
            span = { GridItemSpan(1) },
        ) { index ->
            val summary = series[index] ?: return@items
            // The Xtream series list has no season count; it only arrives with get_series_info, so
            // fall back to the year instead of claiming "0 seasons".
            val subtitle = when {
                summary.seasonCount == 1 -> stringResource(R.string.season_count_one, 1)
                summary.seasonCount > 1 -> stringResource(R.string.season_count, summary.seasonCount)
                summary.year != null -> summary.year.toString()
                else -> null
            }
            SunsetPosterCard(
                title = summary.title,
                imageUrl = summary.posterUrl,
                onClick = { onOpenDetails(ContentType.SERIES, summary.id) },
                year = summary.year,
                subtitle = subtitle,
                width = posterWidth,
            )
        }

        when (val append = series.loadState.append) {
            is LoadState.Loading -> item(key = "append", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SunsetColors.AccentPrimary)
                }
            }

            is LoadState.Error -> item(key = "append-error", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.load_more_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.SemanticError,
                    )
                    SunsetSecondaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = { series.retry() },
                    )
                }
            }

            else -> Unit
        }
    }
}
