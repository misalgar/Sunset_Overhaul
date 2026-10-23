package com.markooverhaul.sunset.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import com.markooverhaul.sunset.core.designsystem.components.SunsetProgressBar
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.Episode
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetArtwork
import com.markooverhaul.sunset.core.ui.components.SunsetBackHeader
import com.markooverhaul.sunset.core.ui.components.SunsetContentRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetHeroBillboard
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import java.util.Locale

@Composable
fun DetailsRoute(
    onBack: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onPlayEpisode: (seriesId: String, seriesTitle: String, episodeId: String, episodeTitle: String) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailsScreen(
        state = state,
        onBack = onBack,
        onPlayMovie = onPlayMovie,
        onPlayEpisode = onPlayEpisode,
        onOpenDetails = onOpenDetails,
        onSelectSeason = viewModel::selectSeason,
        onToggleFavourite = viewModel::toggleFavourite,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onPlayEpisode: (seriesId: String, seriesTitle: String, episodeId: String, episodeTitle: String) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onToggleFavourite: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val gutter = if (isTv) 48.dp else 16.dp
    val context = LocalContext.current
    val detail = state.series

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetColors.BgBase),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1360.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "top") {
                SunsetBackHeader(
                    title = stringResource(
                        if (state.movie != null) R.string.content_type_movie else R.string.content_type_series,
                    ),
                    onBack = onBack,
                )
            }

            when {
                state.isLoading -> {
                    item(key = "skeleton") {
                        Column(
                            modifier = Modifier.padding(horizontal = gutter),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(if (isTv) 460.dp else 380.dp))
                            SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(24.dp))
                            SunsetSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(24.dp))
                        }
                    }
                }

                state.error != null -> {
                    item(key = "error") {
                        SunsetErrorState(
                            title = stringResource(R.string.not_found_title),
                            body = providerErrorText(state.error),
                            retryLabel = stringResource(R.string.action_retry),
                            onRetry = onRetry,
                        )
                    }
                }

                state.movie == null && detail == null -> {
                    item(key = "empty") {
                        SunsetEmptyState(
                            title = stringResource(R.string.not_found_title),
                            body = stringResource(R.string.details_not_found),
                            actionLabel = stringResource(R.string.back_to_home),
                            onAction = onBack,
                        )
                    }
                }

                else -> {
                    val movie = state.movie
                    if (movie != null) {
                        item(key = "hero") {
                            MovieHero(
                                movie = movie,
                                state = state,
                                isTv = isTv,
                                context = context,
                                onPlay = { onPlayMovie(movie) },
                                onToggleFavourite = onToggleFavourite,
                            )
                        }
                        item(key = "facts") {
                            FactsBlock(
                                cast = movie.cast,
                                director = movie.director,
                                gutter = gutter,
                            )
                        }
                    }

                    if (detail != null) {
                        item(key = "series-hero") {
                            val seasonCount = detail.summary.seasonCount
                            val seasonLabel = if (seasonCount == 1) {
                                stringResource(R.string.season_count_one, seasonCount)
                            } else {
                                stringResource(R.string.season_count, seasonCount)
                            }
                            SeriesHero(
                                detail = detail,
                                state = state,
                                qualifier = seasonLabel,
                                isTv = isTv,
                                context = context,
                                onToggleFavourite = onToggleFavourite,
                                onPlay = {
                                    val all = detail.seasons.flatMap { it.episodes }
                                    val target = all.firstOrNull { it.id == state.resumeEpisodeId }
                                        ?: detail.seasons.firstOrNull { it.number == state.selectedSeason }?.episodes?.firstOrNull()
                                        ?: all.firstOrNull()
                                    if (target != null) {
                                        onPlayEpisode(detail.summary.id, detail.summary.title, target.id, target.title)
                                    }
                                },
                            )
                        }

                        item(key = "facts") {
                            FactsBlock(
                                cast = detail.cast,
                                director = null,
                                gutter = gutter,
                            )
                        }

                        if (detail.seasons.isNotEmpty()) {
                            item(key = "season-title") {
                                SunsetSectionHeader(
                                    title = stringResource(R.string.details_episodes),
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            }
                            item(key = "seasons") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = gutter),
                                ) {
                                    items(detail.seasons, key = { it.number }) { season ->
                                        CategoryChip(
                                            text = stringResource(R.string.season_label, season.number),
                                            selected = season.number == state.selectedSeason,
                                            onClick = { onSelectSeason(season.number) },
                                        )
                                    }
                                }
                            }
                            item(key = "episode-count") {
                                val season = detail.seasons.firstOrNull { it.number == state.selectedSeason }
                                val count = season?.episodes?.size ?: 0
                                Text(
                                    text = stringResource(
                                        if (count == 1) R.string.episode_count_one else R.string.episode_count,
                                        count,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SunsetColors.TextTertiary,
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            }
                            val season = detail.seasons.firstOrNull { it.number == state.selectedSeason }
                            items(season?.episodes.orEmpty(), key = { it.id }) { episode ->
                                EpisodeRow(
                                    episode = episode,
                                    isCurrent = state.resumeEpisodeId == episode.id,
                                    progress = if (state.resumeEpisodeId == episode.id) state.progress?.fraction else null,
                                    gutter = gutter,
                                    isTv = isTv,
                                    onPlay = {
                                        onPlayEpisode(detail.summary.id, detail.summary.title, episode.id, episode.title)
                                    },
                                )
                            }
                        }

                        if (detail.seasons.isEmpty()) {
                            item(key = "no-episodes") {
                                Text(
                                    text = stringResource(R.string.details_episodes_coming_soon),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SunsetColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            }
                        }
                    }

                    if (state.similar.isNotEmpty()) {
                        item(key = "similar") {
                            SunsetContentRow(
                                title = stringResource(R.string.details_more_like_this),
                                items = state.similar,
                                key = { item ->
                                    when (item) {
                                        is SimilarItem.MovieItem -> item.movie.id
                                        is SimilarItem.SeriesItem -> item.series.id
                                    }
                                },
                                gutter = gutter,
                            ) { item ->
                                when (item) {
                                    is SimilarItem.MovieItem -> SunsetPosterCard(
                                        title = item.movie.title,
                                        imageUrl = item.movie.posterUrl,
                                        onClick = { onOpenDetails(ContentType.MOVIE, item.movie.id) },
                                        year = item.movie.year,
                                        subtitle = item.movie.genres.firstOrNull(),
                                        width = if (isTv) 184.dp else 148.dp,
                                    )

                                    is SimilarItem.SeriesItem -> SunsetPosterCard(
                                        title = item.series.title,
                                        imageUrl = item.series.posterUrl,
                                        onClick = { onOpenDetails(ContentType.SERIES, item.series.id) },
                                        year = item.series.year,
                                        subtitle = item.series.genres.firstOrNull(),
                                        width = if (isTv) 184.dp else 148.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieHero(
    movie: Movie,
    state: DetailsUiState,
    isTv: Boolean,
    context: android.content.Context,
    onPlay: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    val meta = buildList {
        movie.year?.let { add(it.toString()) }
        movie.certification?.let { add(it) }
        SunsetFormat.runtime(context, movie.durationMinutes)?.let { add(it) }
        movie.rating?.let { add(context.getString(R.string.rating_value, String.format(Locale.getDefault(), "%.1f", it))) }
        movie.genres.firstOrNull()?.let { add(it) }
    }
    val resumable = state.progress?.fraction?.let { it >= 0.05f && it < 0.95f } == true
    SunsetHeroBillboard(
        eyebrow = stringResource(R.string.content_type_movie),
        title = movie.title,
        synopsis = movie.plot,
        meta = meta,
        imageUrl = movie.backdropUrl,
        primaryLabel = stringResource(if (resumable) R.string.action_resume else R.string.action_play),
        onPrimary = onPlay,
        secondaryLabel = stringResource(
            if (state.isFavourite) R.string.action_remove_from_list else R.string.action_add_to_list,
        ),
        onSecondary = onToggleFavourite,
        height = if (isTv) 460.dp else 380.dp,
    )
}

@Composable
private fun SeriesHero(
    detail: SeriesDetail,
    state: DetailsUiState,
    qualifier: String,
    isTv: Boolean,
    context: android.content.Context,
    onPlay: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    val summary = detail.summary
    val meta = buildList {
        summary.year?.let { add(it.toString()) }
        detail.certification?.let { add(it) }
        add(qualifier)
        summary.rating?.let { add(context.getString(R.string.rating_value, String.format(Locale.getDefault(), "%.1f", it))) }
        summary.genres.firstOrNull()?.let { add(it) }
    }
    val resumable = state.progress?.fraction?.let { it >= 0.05f && it < 0.95f } == true
    SunsetHeroBillboard(
        eyebrow = stringResource(R.string.content_type_series),
        title = summary.title,
        synopsis = detail.plot,
        meta = meta,
        imageUrl = summary.backdropUrl,
        primaryLabel = stringResource(if (resumable) R.string.action_resume else R.string.action_play),
        onPrimary = onPlay,
        secondaryLabel = stringResource(
            if (state.isFavourite) R.string.action_remove_from_list else R.string.action_add_to_list,
        ),
        onSecondary = onToggleFavourite,
        height = if (isTv) 460.dp else 380.dp,
    )
}

@Composable
private fun FactsBlock(
    cast: List<String>,
    director: String?,
    gutter: androidx.compose.ui.unit.Dp,
) {
    if (cast.isEmpty() && director == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (cast.isNotEmpty()) {
            Text(
                text = stringResource(R.string.details_cast),
                style = MaterialTheme.typography.labelLarge,
                color = SunsetColors.TextTertiary,
            )
            Text(
                text = cast.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextSecondary,
            )
        }
        if (director != null) {
            Text(
                text = "${stringResource(R.string.details_director)}: $director",
                style = MaterialTheme.typography.bodySmall,
                color = SunsetColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    isCurrent: Boolean,
    progress: Float?,
    gutter: androidx.compose.ui.unit.Dp,
    isTv: Boolean,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(SunsetRadius.Md)
    SunsetFocusable(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter),
        shape = shape,
        scaleOnFocus = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) SunsetColors.BgSurface3 else SunsetColors.BgSurface2)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(if (isTv) 200.dp else 132.dp)
                    .aspectRatio(16f / 9f),
            ) {
                SunsetArtwork(
                    title = episode.title,
                    imageUrl = episode.stillUrl,
                    shape = RoundedCornerShape(SunsetRadius.Sm),
                    modifier = Modifier.fillMaxSize(),
                )
                if (isCurrent && progress != null) {
                    SunsetProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.episode_label, episode.seasonNumber, episode.episodeNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetColors.AccentPrimary,
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                episode.durationMinutes?.let { minutes ->
                    Text(
                        text = stringResource(R.string.runtime_minutes, minutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextTertiary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.action_play_episode),
                tint = SunsetColors.AccentPrimary,
            )
        }
    }
}
