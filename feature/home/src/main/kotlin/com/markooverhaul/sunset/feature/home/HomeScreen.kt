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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesSummary
import com.markooverhaul.sunset.core.model.WatchProgress
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.SunsetChannelCard
import com.markooverhaul.sunset.core.ui.components.LocalContentEntryFocus
import com.markooverhaul.sunset.core.ui.components.SunsetContentRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.SunsetHeroBillboard
import com.markooverhaul.sunset.core.ui.components.SunsetLandscapeCard
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import java.util.Locale
import kotlinx.coroutines.launch

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
        // 6 columns on TV (spec Â§7.5) and never larger than 176 dp, so a full row plus captions
        // fits a 10-foot layout instead of four oversized posters.
        val posterWidth = if (isTv) {
            ((maxWidth - gutter * 2 - itemSpacing * (TV_POSTER_COLUMNS - 1)) / TV_POSTER_COLUMNS.toFloat())
                .coerceIn(TV_MIN_POSTER_WIDTH, TV_MAX_POSTER_WIDTH)
        } else {
            148.dp
        }
        // Keep the hero cinematic but leave room for the first rail on short TV viewports. The
        // compact billboard style fits two title lines in this height.
        val heroHeight = if (isTv) {
            (maxHeight * 0.50f).coerceIn(280.dp, 320.dp)
        } else {
            400.dp
        }

        // TV rails are laid out vertically, but Compose's geometric focus search tends to keep
        // moving sideways inside a LazyRow. Explicit up/down links make Down go to the next row and
        // Up return to the row above instead of jumping to the hero.
        // The nav's Down key targets this requester. Home attaches it to the hero Play button so
        // entering Home from the top bar is deterministic, and row navigation reuses the same one.
        val entryFocus = LocalContentEntryFocus.current
        val heroFocus = remember(entryFocus) { entryFocus ?: FocusRequester() }
        var initialHeroFocusApplied by remember { mutableStateOf(false) }
        LaunchedEffect(state.hero?.id, isTv) {
            if (isTv && state.hero != null && !initialHeroFocusApplied) {
                initialHeroFocusApplied = true
                runCatching { heroFocus.requestFocus() }
            }
        }
        val rowFocus = remember {
            mapOf(
                "continue" to FocusRequester(),
                "live" to FocusRequester(),
                "because" to FocusRequester(),
                "discovery" to FocusRequester(),
                "recentMovies" to FocusRequester(),
                "recentSeries" to FocusRequester(),
                "saved" to FocusRequester(),
            )
        }
        val hasHistory = state.continueWatching.isNotEmpty()
        val discovery = if (hasHistory) state.recommended else state.recentMovies
        val saved = savedCards(state)
        val visibleRowKeys = buildList {
            if (state.continueWatching.isNotEmpty()) add("continue")
            if (state.liveNow.isNotEmpty()) add("live")
            if (state.becauseItems.isNotEmpty()) add("because")
            if (discovery.isNotEmpty()) add("discovery")
            if (state.recentMovies.isNotEmpty()) add("recentMovies")
            if (state.recentSeries.isNotEmpty()) add("recentSeries")
            if (saved.isNotEmpty()) add("saved")
        }
        // Includes the skeleton items so scroll fallbacks address the correct lazy-list index
        // while individual sections are still loading.
        val sectionKeys = buildList {
            if (state.continueWatching.isNotEmpty()) add("continue")
            if (state.loadingLive || state.liveNow.isNotEmpty()) add("live")
            if (state.becauseLoading || state.becauseItems.isNotEmpty()) add("because")
            if (state.loadingCatalogue || discovery.isNotEmpty()) add("discovery")
            if (state.loadingCatalogue || state.recentMovies.isNotEmpty()) add("recentMovies")
            if (state.loadingCatalogue || state.recentSeries.isNotEmpty()) add("recentSeries")
            if (saved.isNotEmpty()) add("saved")
        }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var focusedRowKey by remember { mutableStateOf<String?>(null) }
        var focusedItem by remember { mutableStateOf<Pair<String, Any>?>(null) }
        val heroItemOffset = if (state.hero != null || state.loadingCatalogue) 1 else 0

        fun itemFocusModifier(
            rowKey: String,
            itemKey: Any,
            modifier: Modifier,
        ): Modifier = modifier.onFocusChanged { focusState ->
            val identity = rowKey to itemKey
            if (focusState.isFocused) {
                focusedRowKey = rowKey
                focusedItem = identity
            } else if (focusedItem == identity) {
                // A focus move within the same LazyRow can deliver the new item's focus before the
                // old item's loss. Only the item that still owns focus may clear the row state.
                focusedItem = null
                if (focusedRowKey == rowKey) focusedRowKey = null
            }
        }

        fun rowItemIndex(key: String): Int =
            1 + heroItemOffset + sectionKeys.indexOf(key)

        fun focusRow(key: String) {
            val index = rowItemIndex(key)
            if (index < 0) return
            scope.launch {
                // Prefer focus-driven bring-into-view: it scrolls just enough to reveal the card.
                // Only fall back to a row scroll when the target row was outside the composed window.
                val moved = runCatching { rowFocus.getValue(key).requestFocus() }.isSuccess
                if (!moved) {
                    runCatching { listState.animateScrollToItem(index) }
                    kotlinx.coroutines.delay(150)
                    runCatching { rowFocus.getValue(key).requestFocus() }
                }
            }
        }

        fun focusHero() {
            if (state.hero == null) return
            scope.launch {
                runCatching { listState.scrollToItem(1) }
                kotlinx.coroutines.delay(120)
                runCatching { heroFocus.requestFocus() }
            }
        }

        fun moveUp(key: String) {
            val index = visibleRowKeys.indexOf(key)
            when {
                index < 0 -> Unit
                index == 0 -> focusHero()
                else -> focusRow(visibleRowKeys[index - 1])
            }
        }

        fun moveDown(key: String) {
            val index = visibleRowKeys.indexOf(key)
            visibleRowKeys.getOrNull(index + 1)?.let(::focusRow)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val row = focusedRowKey
                    when (event.key) {
                        Key.DirectionUp -> if (row != null) {
                            moveUp(row)
                            true
                        } else {
                            false
                        }

                        Key.DirectionDown -> if (row != null) {
                            moveDown(row)
                            true
                        } else {
                            false
                        }

                        else -> false
                    }
                },
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
                            focusRequester = heroFocus,
                            onPlay = { onPlayMovie(hero) },
                            onResume = onResume,
                            onDetails = { onOpenDetails(ContentType.MOVIE, hero.id) },
                        )
                    }
                } else if (state.loadingCatalogue) {
                    item(key = "hero-skeleton") {
                        SunsetSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heroHeight),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
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
                            firstItemFocusRequester = rowFocus.getValue("continue"),
                        ) { progress, modifier ->
                            SunsetLandscapeCard(
                                title = progress.title,
                                imageUrl = progress.artworkUrl,
                                onClick = { onResume(progress) },
                                eyebrow = contentTypeLabel(progress.contentType),
                                progress = progress.fraction,
                                width = if (isTv) 240.dp else 200.dp,
                                modifier = itemFocusModifier(
                                    "continue",
                                    progress.contentId + progress.episodeId.orEmpty(),
                                    modifier,
                                ),
                            )
                        }
                    }
                }

                if (state.loadingLive) {
                    item(key = "live-skeleton") {
                        SectionSkeleton(
                            title = stringResource(R.string.home_row_live_now),
                            gutter = gutter,
                            cardWidth = if (isTv) 240.dp else 220.dp,
                            cardHeight = if (isTv) 176.dp else 166.dp,
                        )
                    }
                } else if (state.liveNow.isNotEmpty()) {
                    item(key = "live") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_live_now),
                            items = state.liveNow,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenLive,
                            firstItemFocusRequester = rowFocus.getValue("live"),
                        ) { channel, modifier ->
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
                                width = if (isTv) 240.dp else 220.dp,
                                modifier = itemFocusModifier("live", channel.id, modifier),
                            )
                        }
                    }
                }

                if (state.becauseLoading) {
                    item(key = "because-skeleton") {
                        SectionSkeleton(
                            title = stringResource(
                                R.string.home_row_because,
                                state.becauseTitle.orEmpty(),
                            ),
                            gutter = gutter,
                            cardWidth = posterWidth,
                            cardHeight = posterWidth * 1.5f + 88.dp,
                        )
                    }
                } else if (state.becauseItems.isNotEmpty()) {
                    item(key = "because") {
                        SunsetContentRow(
                            title = stringResource(
                                R.string.home_row_because,
                                state.becauseTitle.orEmpty(),
                            ),
                            items = state.becauseItems,
                            key = { item -> item.type.name + item.id },
                            gutter = gutter,
                            firstItemFocusRequester = rowFocus.getValue("because"),
                        ) { item, modifier ->
                            SunsetPosterCard(
                                title = item.title,
                                imageUrl = item.posterUrl,
                                onClick = { onOpenDetails(item.type, item.id) },
                                year = item.year,
                                subtitle = item.subtitle,
                                width = posterWidth,
                                modifier = itemFocusModifier(
                                    "because",
                                    item.type.name + item.id,
                                    modifier,
                                ),
                            )
                        }
                    }
                }

                if (state.loadingCatalogue) {
                    item(key = "discovery-skeleton") {
                        SectionSkeleton(
                            title = stringResource(
                                if (hasHistory) R.string.home_row_recommended else R.string.home_row_new_on_sunset,
                            ),
                            gutter = gutter,
                            cardWidth = posterWidth,
                            cardHeight = posterWidth * 1.5f + 88.dp,
                        )
                    }
                } else if (discovery.isNotEmpty()) {
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
                            firstItemFocusRequester = rowFocus.getValue("discovery"),
                        ) { movie, modifier ->
                            MoviePoster(
                                movie,
                                posterWidth,
                                onOpenDetails,
                                itemFocusModifier("discovery", movie.id, modifier),
                            )
                        }
                    }
                }

                if (state.loadingCatalogue) {
                    item(key = "recent-movies-skeleton") {
                        SectionSkeleton(
                            title = stringResource(R.string.home_row_recent_movies),
                            gutter = gutter,
                            cardWidth = posterWidth,
                            cardHeight = posterWidth * 1.5f + 88.dp,
                        )
                    }
                } else if (state.recentMovies.isNotEmpty()) {
                    item(key = "recent-movies") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_recent_movies),
                            items = state.recentMovies,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenMovies,
                            firstItemFocusRequester = rowFocus.getValue("recentMovies"),
                        ) { movie, modifier ->
                            MoviePoster(
                                movie,
                                posterWidth,
                                onOpenDetails,
                                itemFocusModifier("recentMovies", movie.id, modifier),
                            )
                        }
                    }
                }

                if (state.loadingCatalogue) {
                    item(key = "recent-series-skeleton") {
                        SectionSkeleton(
                            title = stringResource(R.string.home_row_recent_series),
                            gutter = gutter,
                            cardWidth = posterWidth,
                            cardHeight = posterWidth * 1.5f + 88.dp,
                        )
                    }
                } else if (state.recentSeries.isNotEmpty()) {
                    item(key = "recent-series") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_recent_series),
                            items = state.recentSeries,
                            key = { it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenSeries,
                            firstItemFocusRequester = rowFocus.getValue("recentSeries"),
                        ) { series, modifier ->
                            SeriesPoster(
                                series,
                                posterWidth,
                                onOpenDetails,
                                itemFocusModifier("recentSeries", series.id, modifier),
                            )
                        }
                    }
                }

                if (saved.isNotEmpty()) {
                    item(key = "saved") {
                        SunsetContentRow(
                            title = stringResource(R.string.home_row_my_list),
                            items = saved,
                            key = { it.type.name + it.id },
                            gutter = gutter,
                            seeAllLabel = stringResource(R.string.action_see_all),
                            onSeeAll = onOpenMyList,
                            firstItemFocusRequester = rowFocus.getValue("saved"),
                        ) { card, modifier ->
                            SunsetPosterCard(
                                title = card.title,
                                imageUrl = card.posterUrl,
                                onClick = { onOpenDetails(card.type, card.id) },
                                year = card.year,
                                subtitle = card.subtitle,
                                width = posterWidth,
                                modifier = itemFocusModifier(
                                    "saved",
                                    card.type.name + card.id,
                                    modifier,
                                ),
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
    focusRequester: FocusRequester,
    onPlay: () -> Unit,
    onResume: (WatchProgress) -> Unit,
    onDetails: () -> Unit,
) {
    val context = LocalContext.current
    val resumable = progress != null && progress.fraction >= WatchProgress.MIN_RESUME_FRACTION
    val meta = buildList {
        movie.year?.let { add(it.toString()) }
        movie.certification?.let { add(it) }
        SunsetFormat.runtime(context, movie.durationMinutes)?.let { add(it) }
        movie.rating?.let { add(context.getString(R.string.rating_value, String.format(Locale.getDefault(), "%.1f", it))) }
        movie.genres.firstOrNull()?.let { add(it) }
    }

    SunsetHeroBillboard(
        eyebrow = stringResource(R.string.home_eyebrow_movie),
        title = movie.title,
        synopsis = movie.plot,
        meta = meta,
        imageUrl = movie.backdropUrl ?: movie.posterUrl,
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
    modifier: Modifier = Modifier,
) {
    SunsetPosterCard(
        title = movie.title,
        imageUrl = movie.posterUrl,
        onClick = { onOpenDetails(ContentType.MOVIE, movie.id) },
        year = movie.year,
        subtitle = movie.genres.firstOrNull() ?: movie.year?.toString(),
        width = width,
        modifier = modifier,
    )
}

@Composable
private fun SeriesPoster(
    series: SeriesSummary,
    width: androidx.compose.ui.unit.Dp,
    onOpenDetails: (ContentType, String) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}

@Composable
private fun SectionSkeleton(
    title: String,
    gutter: androidx.compose.ui.unit.Dp,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SunsetSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = gutter),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = gutter),
        ) {
            items(5) {
                SunsetSkeleton(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight),
                )
            }
        }
    }
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

