package com.markooverhaul.sunset.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.WatchProgress
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetChannelCard
import com.markooverhaul.sunset.core.ui.components.SunsetContentRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetHeroBillboard
import com.markooverhaul.sunset.core.ui.components.SunsetLandscapeCard
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import java.util.Locale

private const val TV_POSTER_COLUMNS = 6
private val TV_MIN_POSTER_WIDTH = 120.dp
private val TV_MAX_POSTER_WIDTH = 176.dp

@Composable
fun HomeRoute(
    onPlayMovie: (Movie) -> Unit,
    onResume: (WatchProgress) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    onOpenChannel: (LiveChannel) -> Unit,
    onOpenLive: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenMyList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPlayMovie = onPlayMovie,
        onResume = onResume,
        onOpenDetails = onOpenDetails,
        onOpenChannel = onOpenChannel,
        onOpenLive = onOpenLive,
        onOpenMovies = onOpenMovies,
        onOpenSeries = onOpenSeries,
        onOpenMyList = onOpenMyList,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlayMovie: (Movie) -> Unit,
    onResume: (WatchProgress) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    onOpenChannel: (LiveChannel) -> Unit,
    onOpenLive: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenMyList: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gutter = if (isTv) 48.dp else 16.dp
        val itemSpacing = if (isTv) 16.dp else 12.dp
        // 6 columns on TV (spec §7.5) and never larger than 176 dp, so a full row plus captions
        // fits a 10-foot layout instead of four oversized posters.
        val posterWidth = if (isTv) {
            ((maxWidth - gutter * 2 - itemSpacing * (TV_POSTER_COLUMNS - 1)) / TV_POSTER_COLUMNS.toFloat())
                .coerceIn(TV_MIN_POSTER_WIDTH, TV_MAX_POSTER_WIDTH)
        } else {
            148.dp
        }
        // Keep the hero cinematic but leave room for the first rail on short TV viewports.
        // The hero content (two-line display title, meta, CTAs) needs ~300 dp; anything shorter
        // clips the title at the top of the billboard.
        val heroHeight = if (isTv) {
            (maxHeight * 0.52f).coerceIn(320.dp, 380.dp)
        } else {
            400.dp
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isTv) 64.dp else 40.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTv) 20.dp else 28.dp),
        ) {
        item(key = "demo-banner") {
            SunsetDemoBanner()
        }

        when {
            state.isLoading -> {
                item(key = "hero-skeleton") {
                    SunsetSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                    )
                }
                item(key = "rows-skeleton") {
                    RowSkeleton(gutter = gutter, posterWidth = posterWidth)
                }
            }

            state.error != null -> {
                item(key = "error") {
                    SunsetErrorState(
                        title = stringResource(R.string.error_network_title),
                        body = providerErrorText(state.error),
                        retryLabel = stringResource(R.string.action_retry),
                        onRetry = onRetry,
                    )
                }
            }

            state.isEmpty -> {
                item(key = "empty") {
                    SunsetEmptyState(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = onRetry,
                    )
                }
            }

            else -> {
                val hero = state.hero
                if (hero != null) {
                    item(key = "hero") {
                        HeroSection(
                            movie = hero,
                            progress = state.continueWatching.firstOrNull {
                                it.contentId == hero.id && !it.completed
                            },
                            isTv = isTv,
                            height = heroHeight,
                            onPlay = { onPlayMovie(hero) },
                            onResume = onResume,
                            onDetails = { onOpenDetails(ContentType.MOVIE, hero.id) },
                        )
                    }
                }

                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_continue),
                            items = state.continueWatching,
                            key = { it.contentId + it.episodeId.orEmpty() },
                            gutter = gutter,
                        ) { progress ->
                            SunsetLandscapeCard(
                                title = progress.title,
                                imageUrl = null,
                                onClick = { onResume(progress) },
                                eyebrow = contentTypeLabel(progress.contentType),
                                progress = progress.fraction,
                                width = if (isTv) 360.dp else 280.dp,
                            )
                        }
                    }
                }

                if (state.liveNow.isNotEmpty()) {
                    item(key = "live") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_live_now),
                            items = state.liveNow,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenLive,
                        ) { channel ->
                            val nowNext = state.nowNext[channel.id]
                            SunsetChannelCard(
                                channel = channel,
                                nowTitle = nowNext?.now?.title,
                                timeRange = nowNext?.now?.let {
                                    SunsetFormat.timeRange(context = context, startMs = it.startUtcMs, endMs = it.endUtcMs)
                                },
                                onClick = { onOpenChannel(channel) },
                                progress = nowNext?.now?.let { programme ->
                                    val span = (programme.endUtcMs - programme.startUtcMs).coerceAtLeast(1L)
                                    ((System.currentTimeMillis() - programme.startUtcMs).toFloat() / span)
                                        .coerceIn(0f, 1f)
                                },
                                width = if (isTv) 300.dp else 250.dp,
                            )
                        }
                    }
                }

                val hasHistory = state.continueWatching.isNotEmpty()
                val discovery = if (hasHistory) state.recommended else state.recentMovies
                if (discovery.isNotEmpty()) {
                    item(key = "discovery") {
                        SunsetContentRow(
                            title = stringResource(
                                if (hasHistory) R.string.home_row_recommended else R.string.home_row_new_on_sunset,
                            ),
                            items = discovery,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenMovies,
                        ) { movie ->
                            MoviePoster(movie, posterWidth, onOpenDetails)
                        }
                    }
                }

                if (state.recentMovies.isNotEmpty()) {
                    item(key = "recent-movies") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_recent_movies),
                            items = state.recentMovies,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenMovies,
                        ) { movie ->
                            MoviePoster(movie, posterWidth, onOpenDetails)
                        }
                    }
                }

                if (state.recentSeries.isNotEmpty()) {
                    item(key = "recent-series") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_recent_series),
                            items = state.recentSeries,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenSeries,
                        ) { series ->
                            SeriesPoster(series, posterWidth, onOpenDetails)
                        }
                    }
                }

                val saved = savedCards(state)
                if (saved.isNotEmpty()) {
                    item(key = "saved") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_my_list),
                            items = saved,
                            key = { it.type.name + it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenMyList,
                        ) { card ->
                            SunsetPosterCard(
                                title = card.title,
                                imageUrl = card.posterUrl,
                                onClick = { onOpenDetails(card.type, card.id) },
                                year = card.year,
                                subtitle = card.subtitle,
                                width = posterWidth,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun HeroSection(
    movie: Movie,
    progress: WatchProgress?,
    isTv: Boolean,
    height: androidx.compose.ui.unit.Dp,
    onPlay: () -> Unit,
    onResume: (WatchProgress) -> Unit,
    onDetails: () -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val resumable = progress != null && progress.fraction >= WatchProgress.MIN_RESUME_FRACTION
    val meta = buildList {
        movie.year?.let { add(it.toString()) }
        movie.certification?.let { add(it) }
        SunsetFormat.runtime(context, movie.durationMinutes)?.let { add(it) }
        movie.rating?.let { add(context.getString(R.string.rating_value, String.format(Locale.getDefault(), "%.1f", it))) }
        movie.genres.firstOrNull()?.let { add(it) }
    }

    LaunchedEffect(movie.id, isTv) {
        if (isTv) runCatching { focusRequester.requestFocus() }
    }

    SunsetHeroBillboard(
        eyebrow = stringResource(R.string.home_eyebrow_movie),
        title = movie.title,
        synopsis = movie.plot,
        meta = meta,
        imageUrl = movie.backdropUrl,
        primaryLabel = stringResource(if (resumable) R.string.action_resume else R.string.action_play),
        onPrimary = { if (progress != null && resumable) onResume(progress) else onPlay() },
        primaryButtonModifier = if (isTv) Modifier.focusRequester(focusRequester) else Modifier,
        secondaryLabel = stringResource(R.string.action_more_info),
        onSecondary = onDetails,
        height = height,
    )
}

@Composable
private fun MoviePoster(
    movie: Movie,
    width: androidx.compose.ui.unit.Dp,
    onOpenDetails: (ContentType, String) -> Unit,
) {
    SunsetPosterCard(
        title = movie.title,
        imageUrl = movie.posterUrl,
        onClick = { onOpenDetails(ContentType.MOVIE, movie.id) },
        year = movie.year,
        subtitle = movie.genres.firstOrNull() ?: movie.year?.toString(),
        width = width,
    )
}

@Composable
private fun SeriesPoster(
    series: SeriesSummary,
    width: androidx.compose.ui.unit.Dp,
    onOpenDetails: (ContentType, String) -> Unit,
) {
    val subtitle = if (series.seasonCount == 1) {
        stringResource(R.string.season_count_one, series.seasonCount)
    } else {
        stringResource(R.string.season_count, series.seasonCount)
    }
    SunsetPosterCard(
        title = series.title,
        imageUrl = series.posterUrl,
        onClick = { onOpenDetails(ContentType.SERIES, series.id) },
        year = series.year,
        subtitle = subtitle,
        width = width,
    )
}

@Composable
private fun RowSkeleton(gutter: androidx.compose.ui.unit.Dp, posterWidth: androidx.compose.ui.unit.Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SunsetSkeleton(
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .width(220.dp)
                        .height(28.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = gutter),
                ) {
                    items(5) {
                        SunsetSkeleton(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(posterWidth * 1.5f + 88.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun contentTypeLabel(type: ContentType): String = stringResource(
    when (type) {
        ContentType.LIVE -> R.string.content_type_live
        ContentType.MOVIE -> R.string.content_type_movie
        ContentType.SERIES -> R.string.content_type_series
        ContentType.EPISODE -> R.string.content_type_episode
    },
)

private data class SavedCard(
    val id: String,
    val type: ContentType,
    val title: String,
    val posterUrl: String?,
    val year: Int?,
    val subtitle: String?,
)

private fun savedCards(state: HomeUiState): List<SavedCard> = buildList {
    state.recentMovies
        .filter { it.id in state.favouriteIds }
        .forEach { add(SavedCard(it.id, ContentType.MOVIE, it.title, it.posterUrl, it.year, it.genres.firstOrNull())) }
    state.recentSeries
        .filter { it.id in state.favouriteIds }
        .forEach { add(SavedCard(it.id, ContentType.SERIES, it.title, it.posterUrl, it.year, it.genres.firstOrNull())) }
}
