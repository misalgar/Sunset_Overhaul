package com.markooverhaul.sunset.feature.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.LocalContentEntryFocus
import com.markooverhaul.sunset.core.ui.components.SunsetArtwork
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.util.providerErrorText

@Composable
fun MyListRoute(
    onOpen: (SavedItem) -> Unit,
    onOpenMovies: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyListScreen(
        state = state,
        onOpen = onOpen,
        onOpenMovies = onOpenMovies,
        onSelectFilter = viewModel::selectFilter,
        onRemove = viewModel::remove,
        modifier = modifier,
    )
}

@Composable
fun MyListScreen(
    state: MyListUiState,
    onOpen: (SavedItem) -> Unit,
    onOpenMovies: () -> Unit,
    onSelectFilter: (MyListFilter) -> Unit,
    onRemove: (SavedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val visible = state.visibleItems
    val entryFocus = LocalContentEntryFocus.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "banner") { SunsetDemoBanner() }

        item(key = "header") {
            SunsetSectionHeader(
                title = stringResource(R.string.mylist_title),
                subtitle = if (state.items.isEmpty()) null else state.items.size.toString(),
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
                        text = stringResource(R.string.mylist_filter_all),
                        selected = state.filter == MyListFilter.ALL,
                        onClick = { onSelectFilter(MyListFilter.ALL) },
                        modifier = if (entryFocus != null) {
                            Modifier.focusRequester(entryFocus)
                        } else {
                            Modifier
                        },
                    )
                }
                item(key = "movies") {
                    CategoryChip(
                        text = stringResource(R.string.mylist_filter_movies),
                        selected = state.filter == MyListFilter.MOVIES,
                        onClick = { onSelectFilter(MyListFilter.MOVIES) },
                    )
                }
                item(key = "series") {
                    CategoryChip(
                        text = stringResource(R.string.mylist_filter_series),
                        selected = state.filter == MyListFilter.SERIES,
                        onClick = { onSelectFilter(MyListFilter.SERIES) },
                    )
                }
                item(key = "live") {
                    CategoryChip(
                        text = stringResource(R.string.mylist_filter_live),
                        selected = state.filter == MyListFilter.LIVE,
                        onClick = { onSelectFilter(MyListFilter.LIVE) },
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
                        SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(88.dp))
                    }
                }
            }

            state.error != null -> item(key = "error") {
                Text(
                    text = providerErrorText(state.error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetColors.SemanticError,
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }

            visible.isEmpty() -> item(key = "empty") {
                SunsetEmptyState(
                    title = stringResource(R.string.mylist_empty_title),
                    body = stringResource(R.string.mylist_empty_body),
                    actionLabel = stringResource(R.string.mylist_browse_movies),
                    onAction = onOpenMovies,
                )
            }

            else -> items(visible, key = { it.type.name + it.id }) { item ->
                SavedRow(
                    item = item,
                    gutter = gutter,
                    isTv = isTv,
                    onClick = { onOpen(item) },
                    onRemove = { onRemove(item) },
                )
            }
        }
    }
}

@Composable
private fun SavedRow(
    item: SavedItem,
    gutter: androidx.compose.ui.unit.Dp,
    isTv: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter),
        shape = RoundedCornerShape(SunsetRadius.Md),
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SunsetColors.BgSurface2)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(if (isTv) 176.dp else 120.dp)
                    .aspectRatio(16f / 9f),
            ) {
                SunsetArtwork(
                    title = item.title,
                    imageUrl = item.imageUrl,
                    shape = RoundedCornerShape(SunsetRadius.Sm),
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        when (item.type) {
                            ContentType.MOVIE -> R.string.content_type_movie
                            ContentType.SERIES -> R.string.content_type_series
                            ContentType.LIVE -> R.string.content_type_live
                            ContentType.EPISODE -> R.string.content_type_episode
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.AccentPrimary,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = SunsetColors.TextTertiary,
                )
            }
        }
    }
}
