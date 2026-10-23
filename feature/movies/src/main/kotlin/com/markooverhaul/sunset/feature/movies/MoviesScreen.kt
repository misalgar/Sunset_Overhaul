package com.markooverhaul.sunset.feature.movies

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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetFilterRow
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun MoviesRoute(
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()
    MoviesScreen(
        state = state,
        movies = movies,
        onSelectCategory = viewModel::selectCategory,
        onOpenDetails = onOpenDetails,
        modifier = modifier,
    )
}

@Composable
fun MoviesScreen(
    state: MoviesUiState,
    movies: androidx.paging.compose.LazyPagingItems<com.markooverhaul.sunset.core.model.Movie>,
    onSelectCategory: (String?) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val posterWidth = if (isTv) 184.dp else 150.dp
    val refresh = movies.loadState.refresh

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (isTv) 200.dp else 150.dp),
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
                text = stringResource(R.string.movies_title),
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
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
                    onRetry = { movies.retry() },
                )
            }
        } else if (refresh is LoadState.Loading && movies.itemCount == 0) {
            items(count = 6, key = { "skeleton-$it" }) {
                SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(posterWidth * 1.5f))
            }
        } else if (refresh is LoadState.Error) {
            item(key = "refresh-error", span = { GridItemSpan(maxLineSpan) }) {
                SunsetErrorState(
                    title = stringResource(R.string.error_generic_title),
                    body = providerErrorText(refresh.error),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = { movies.retry() },
                )
            }
        } else if (movies.itemCount == 0) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                SunsetEmptyState(
                    title = stringResource(R.string.movies_empty_title),
                    body = stringResource(R.string.movies_empty_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { movies.retry() },
                )
            }
        }

        items(
            count = movies.itemCount,
            key = movies.itemKey { it.id },
            span = { GridItemSpan(1) },
        ) { index ->
            val movie = movies[index] ?: return@items
            SunsetPosterCard(
                title = movie.title,
                imageUrl = movie.posterUrl,
                onClick = { onOpenDetails(ContentType.MOVIE, movie.id) },
                year = movie.year,
                subtitle = movie.genres.firstOrNull(),
                width = posterWidth,
            )
        }

        when (val append = movies.loadState.append) {
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
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SunsetColors.SemanticError,
                    )
                    com.markooverhaul.sunset.core.designsystem.components.SunsetSecondaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = { movies.retry() },
                    )
                }
            }

            else -> Unit
        }
    }
}
