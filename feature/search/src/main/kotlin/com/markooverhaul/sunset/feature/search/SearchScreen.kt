package com.markooverhaul.sunset.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetSectionHeader
import com.markooverhaul.sunset.core.designsystem.components.SunsetSearchField
import com.markooverhaul.sunset.core.designsystem.components.sunsetFocusRing
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.variantLabel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.LocalContentEntryFocus
import com.markooverhaul.sunset.core.ui.components.SunsetChannelRow
import com.markooverhaul.sunset.core.ui.components.SunsetContentRow
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.LocalDemoMode
import com.markooverhaul.sunset.core.ui.components.SunsetPosterCard
import com.markooverhaul.sunset.core.ui.util.providerErrorText

private const val TV_POSTER_COLUMNS = 6

/** First focusable group in the results, used to move focus out of the search field. */
private enum class SearchFocusGroup { Recent, Live, Movies, Series }

/**
 * Lazy list items attach their focus target during layout, which can be after the state effect
 * that wants to focus them. Retry briefly instead of dropping the request.
 */
private suspend fun FocusRequester.requestFocusWhenReady(
    attempts: Int = 10,
    delayMs: Long = 60L,
): Boolean {
    repeat(attempts) {
        if (runCatching { requestFocus() }.isSuccess) return true
        delay(delayMs)
    }
    return false
}

@Composable
fun SearchRoute(
    onOpenDetails: (ContentType, String) -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::submit,
        onClearQuery = viewModel::clearQuery,
        onClearRecent = viewModel::clearRecent,
        onSelectFilter = viewModel::setFilter,
        onToggleFavourite = viewModel::toggleChannelFavourite,
        onOpenDetails = onOpenDetails,
        onPlayChannel = onPlayChannel,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearQuery: () -> Unit,
    onClearRecent: () -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onToggleFavourite: (LiveChannel) -> Unit,
    onOpenDetails: (ContentType, String) -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldFocusRequester = remember { FocusRequester() }
    val launcherFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }
    // The nav bar's Down key focuses this; without it the key is consumed and nothing happens.
    val entryFocus = LocalContentEntryFocus.current
    val launcherInteraction = remember { MutableInteractionSource() }
    var pendingResultsFocus by remember { mutableStateOf(false) }
    var didInitialFocus by remember { mutableStateOf(false) }
    // TV: the search box is a plain focus stop. The IME only opens after an explicit Select, so
    // navigating up/down past the field can never pop the keyboard (reported on Fire TV).
    var editing by remember { mutableStateOf(!isTv) }
    var launcherFocused by remember { mutableStateOf(false) }
    var wasImeVisible by remember { mutableStateOf(false) }
    var wasEditing by remember { mutableStateOf(false) }
    var returnFocusToLauncher by remember { mutableStateOf(false) }
    var editStartedAt by remember { mutableStateOf(0L) }
    val imeVisible = WindowInsets.isImeVisible
    val searchShape = RoundedCornerShape(SunsetRadius.Sm)

    // Back dismisses the TV keyboard but does not change `editing` by itself. When the IME hides
    // after an edit session (and the viewer did not navigate away with Up/Down/Submit), return
    // focus to the search launcher so the box stays selected without reopening the keyboard.
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            wasImeVisible = false
            if (wasEditing) {
                wasEditing = false
                editing = false
                keyboardController?.hide()
                returnFocusToLauncher = true
            } else if (editing) {
                editing = false
                keyboardController?.hide()
            }
        }
    }

    BackHandler(enabled = isTv && editing) {
        wasEditing = false
        editing = false
        keyboardController?.hide()
        returnFocusToLauncher = true
    }

    LaunchedEffect(returnFocusToLauncher) {
        if (!returnFocusToLauncher) return@LaunchedEffect
        returnFocusToLauncher = false
        // The navigation also restores focus after Back, so keep asking for the search box and
        // only stop once it has held focus for two consecutive checks.
        var stableChecks = 0
        repeat(10) {
            if (launcherFocused) {
                stableChecks++
                if (stableChecks >= 2) return@LaunchedEffect
            } else {
                stableChecks = 0
                runCatching { launcherFocusRequester.requestFocus() }
            }
            delay(120)
        }
    }

    val results = state.results
    val liveResults = if (state.filter == SearchFilter.All || state.filter == SearchFilter.Live) {
        results?.live.orEmpty()
    } else {
        emptyList()
    }
    val movieResults = if (state.filter == SearchFilter.All || state.filter == SearchFilter.Movies) {
        results?.movies.orEmpty()
    } else {
        emptyList()
    }
    val seriesResults = if (state.filter == SearchFilter.All || state.filter == SearchFilter.Series) {
        results?.series.orEmpty()
    } else {
        emptyList()
    }
    val focusGroup = when {
        state.showPrompt && state.recent.isNotEmpty() -> SearchFocusGroup.Recent
        results != null && liveResults.isNotEmpty() -> SearchFocusGroup.Live
        results != null && movieResults.isNotEmpty() -> SearchFocusGroup.Movies
        results != null && seriesResults.isNotEmpty() -> SearchFocusGroup.Series
        else -> null
    }

    // Submit from the IME (Search/Next/Done/Go) or a recent chip, then leave the field so the
    // remote can reach the results. Previously focus stayed in the field and the IME reopened.
    val submitAndShowResults: () -> Unit = {
        onSubmit()
        wasEditing = false
        keyboardController?.hide()
        if (state.query.trim().length >= SearchUiState.MIN_QUERY_LENGTH) {
            pendingResultsFocus = true
        } else {
            editing = false
        }
    }

    LaunchedEffect(pendingResultsFocus, state.isSearching, focusGroup, results, state.error) {
        if (!pendingResultsFocus || state.isSearching) return@LaunchedEffect
        if (focusGroup != null) {
            pendingResultsFocus = false
            keyboardController?.hide()
            resultsFocusRequester.requestFocusWhenReady()
            // Only leave edit mode after the result owns focus; otherwise focus briefly falls back
            // to the top navigation and the result request is lost.
            editing = false
        } else if (results != null || state.error != null) {
            // Search finished with no focusable result: keep the current focus instead of clearing
            // it, which would hand focus back to the text field and reopen the keyboard.
            pendingResultsFocus = false
            editing = false
        }
    }

    // Entering edit mode is the only path that shows the IME on TV.
    LaunchedEffect(editing) {
        if (!isTv || !editing) return@LaunchedEffect
        wasEditing = true
        editStartedAt = System.currentTimeMillis()
        withFrameNanos { }
        withFrameNanos { }
        runCatching { fieldFocusRequester.requestFocus() }
        keyboardController?.show()
    }

    LaunchedEffect(isTv, focusGroup, results, state.query) {
        if (!isTv || didInitialFocus) return@LaunchedEffect
        when {
            focusGroup != null -> {
                didInitialFocus = true
                resultsFocusRequester.requestFocusWhenReady()
            }

            state.query.isBlank() && results == null && !state.isSearching -> {
                didInitialFocus = true
                val target = if (editing) fieldFocusRequester else launcherFocusRequester
                target.requestFocusWhenReady()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gutter = if (isTv) 48.dp else 16.dp
        val itemSpacing = if (isTv) 16.dp else 12.dp
        val posterWidth = if (isTv) {
            ((maxWidth - gutter * 2 - itemSpacing * (TV_POSTER_COLUMNS - 1)) / TV_POSTER_COLUMNS.toFloat())
                .coerceIn(120.dp, 176.dp)
        } else {
            148.dp
        }

        Column(modifier = Modifier.fillMaxSize()) {
            SunsetDemoBanner()

            Column(
                modifier = Modifier.padding(top = if (isTv) 12.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 8.dp),
            ) {
                // On TV the nav pill and the field placeholder already say "Search"; the title only
                // costs a row of height and pushes the first result group out of the viewport.
                if (!isTv) {
                    SunsetSectionHeader(
                        title = stringResource(R.string.search_title),
                        modifier = Modifier.padding(horizontal = gutter),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .then(
                            if (isTv) {
                                Modifier
                                    .focusRequester(launcherFocusRequester)
                                    .then(
                                        if (!editing && entryFocus != null) {
                                            Modifier.focusRequester(entryFocus)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .onFocusChanged {
                                        launcherFocused = it.isFocused
                                        // If the IME (or the system Back) drops the box before the
                                        // field ever reports focus loss, pull focus back here once
                                        // the navigation's own focus restoration has settled.
                                        if (!it.isFocused &&
                                            wasEditing &&
                                            System.currentTimeMillis() - editStartedAt > 400L
                                        ) {
                                            wasEditing = false
                                            editing = false
                                            returnFocusToLauncher = true
                                        }
                                    }
                                    .sunsetFocusRing(launcherFocused, searchShape)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) {
                                            false
                                        } else {
                                            when {
                                                // Select is the only way to open the keyboard.
                                                !editing && (
                                                    event.key == Key.DirectionCenter ||
                                                        event.key == Key.Enter
                                                    ) -> {
                                                    editing = true
                                                    true
                                                }

                                                !editing && event.key == Key.DirectionDown &&
                                                    focusGroup != null -> {
                                                    keyboardController?.hide()
                                                    runCatching { resultsFocusRequester.requestFocus() }
                                                    true
                                                }

                                                else -> false
                                            }
                                        }
                                    }
                                    .then(
                                        if (editing) {
                                            Modifier
                                        } else {
                                            Modifier.clickable(
                                                interactionSource = launcherInteraction,
                                                indication = null,
                                                onClick = { editing = true },
                                            )
                                        },
                                    )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    SunsetSearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.search_placeholder),
                        clearDescription = stringResource(R.string.search_clear),
                        onSearch = submitAndShowResults,
                        modifier = Modifier.then(
                            if (isTv) {
                                Modifier
                                    .focusRequester(fieldFocusRequester)
                                    .then(
                                        if (editing && entryFocus != null) {
                                            Modifier.focusRequester(entryFocus)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    // Not focusable until the viewer opts into editing, so D-pad
                                    // traversal never triggers the IME.
                                    .focusProperties { canFocus = editing }
                                    .onFocusChanged {
                                        // Back moves focus to the top navigation. If this was not an
                                        // intentional Up/Down/Submit, return focus to the search
                                        // launcher after the navigation settles.
                                        if (!it.isFocused && wasEditing) {
                                            wasEditing = false
                                            editing = false
                                            returnFocusToLauncher = true
                                        }
                                    }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) {
                                            false
                                        } else {
                                            when (event.key) {
                                                Key.DirectionDown -> {
                                                    if (focusGroup == null) {
                                                        false
                                                    } else {
                                                        wasEditing = false
                                                        editing = false
                                                        keyboardController?.hide()
                                                        runCatching {
                                                            resultsFocusRequester.requestFocus()
                                                        }
                                                        true
                                                    }
                                                }

                                                Key.DirectionUp -> {
                                                    wasEditing = false
                                                    editing = false
                                                    keyboardController?.hide()
                                                    false
                                                }

                                                else -> false
                                            }
                                        }
                                    }
                            } else {
                                Modifier
                            },
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTv) 20.dp else 16.dp),
            ) {
                if (state.isSearching) {
                    item(key = "searching") {
                        CircularProgressIndicator(
                            color = SunsetColors.AccentPrimary,
                            modifier = Modifier.padding(horizontal = gutter),
                        )
                    }
                }

                if (state.error != null) {
                    item(key = "error") {
                        SunsetErrorState(
                            title = stringResource(R.string.error_generic_title),
                            body = providerErrorText(state.error),
                            retryLabel = stringResource(R.string.action_retry),
                            onRetry = onSubmit,
                        )
                    }
                }

                if (state.showPrompt) {
                    if (state.recent.isNotEmpty()) {
                        item(key = "recent-header") {
                            SunsetSectionHeader(
                                title = stringResource(R.string.search_recent),
                                seeAllLabel = stringResource(R.string.action_clear_all),
                                onSeeAll = onClearRecent,
                                modifier = Modifier.padding(horizontal = gutter),
                            )
                        }
                        item(key = "recent") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = gutter),
                            ) {
                                itemsIndexed(state.recent, key = { _, item -> item.query }) { index, recent ->
                                    CategoryChip(
                                        text = recent.query,
                                        selected = false,
                                        onClick = {
                                            onQueryChange(recent.query)
                                            onSubmit()
                                            pendingResultsFocus = true
                                        },
                                        modifier = if (
                                            focusGroup == SearchFocusGroup.Recent && index == 0
                                        ) {
                                            Modifier.focusRequester(resultsFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "prompt") {
                            SunsetEmptyState(
                                title = stringResource(R.string.search_empty_title),
                            body = stringResource(
                                if (LocalDemoMode.current) R.string.search_empty_body_demo
                                else R.string.search_empty_body_live,
                            ),
                            )
                        }
                    }
                }

                if (!state.showPrompt && results != null) {
                    item(key = "filters") {
                        val filters = listOf(
                            SearchFilter.All,
                            SearchFilter.Live,
                            SearchFilter.Movies,
                            SearchFilter.Series,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = gutter),
                        ) {
                            itemsIndexed(filters, key = { _, filter -> filter.name }) { _, filter ->
                                CategoryChip(
                                    text = stringResource(
                                        when (filter) {
                                            SearchFilter.All -> R.string.search_filter_all
                                            SearchFilter.Live -> R.string.content_type_live
                                            SearchFilter.Movies -> R.string.nav_movies
                                            SearchFilter.Series -> R.string.nav_series
                                        },
                                    ),
                                    selected = state.filter == filter,
                                    onClick = { onSelectFilter(filter) },
                                )
                            }
                        }
                    }
                }

                if (!state.showPrompt && results != null) {
                    if (liveResults.isEmpty() && movieResults.isEmpty() && seriesResults.isEmpty()) {
                        item(key = "no-results") {
                            SunsetEmptyState(
                                title = stringResource(R.string.search_no_results_title, state.query.trim()),
                                body = stringResource(R.string.search_no_results_body),
                            )
                        }
                    } else {
                        if (liveResults.isNotEmpty()) {
                            item(key = "live-header") {
                                SunsetSectionHeader(
                                    title = stringResource(R.string.search_group_live, liveResults.size),
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            }
                            itemsIndexed(liveResults, key = { _, channel -> "live-${channel.id}" }) { index, channel ->
                                SunsetChannelRow(
                                    channel = channel,
                                    nowTitle = null,
                                    nextTitle = null,
                                    timeRange = null,
                                    onClick = { onPlayChannel(channel) },
                                    onToggleFavourite = { onToggleFavourite(channel) },
                                    isFavourite = channel.id in state.favouriteIds,
                                    favouriteLabel = stringResource(R.string.live_favourite),
                                    unfavouriteLabel = stringResource(R.string.live_unfavourite),
                                    modifier = Modifier
                                        .padding(horizontal = gutter)
                                        .then(
                                            if (focusGroup == SearchFocusGroup.Live && index == 0) {
                                                Modifier.focusRequester(resultsFocusRequester)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                        }

                        if (movieResults.isNotEmpty()) {
                            item(key = "movies") {
                                SunsetContentRow(
                                    title = stringResource(R.string.search_group_movies, movieResults.size),
                                    items = movieResults,
                                    key = { it.id },
                                    gutter = gutter,
                                ) { movie, itemModifier ->
                                    SunsetPosterCard(
                                        title = movie.title,
                                        imageUrl = movie.posterUrl,
                                        onClick = { onOpenDetails(ContentType.MOVIE, movie.id) },
                                        year = movie.year,
                                        subtitle = movie.genres.firstOrNull(),
                                        modifier = itemModifier.then(
                                            if (
                                                focusGroup == SearchFocusGroup.Movies &&
                                                movie.id == movieResults.first().id
                                            ) {
                                                Modifier.focusRequester(resultsFocusRequester)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                        width = posterWidth,
                                    )
                                }
                            }
                        }

                        if (seriesResults.isNotEmpty()) {
                            item(key = "series") {
                                SunsetContentRow(
                                    title = stringResource(R.string.search_group_series, seriesResults.size),
                                    items = seriesResults,
                                    key = { it.id },
                                    gutter = gutter,
                                ) { series, itemModifier ->
                                    SunsetPosterCard(
                                        title = series.title,
                                        imageUrl = series.posterUrl,
                                        onClick = { onOpenDetails(ContentType.SERIES, series.id) },
                                        year = series.year,
                                        subtitle = series.genres.firstOrNull(),
                                        badge = series.variantLabel,
                                        modifier = itemModifier.then(
                                            if (
                                                focusGroup == SearchFocusGroup.Series &&
                                                series.id == seriesResults.first().id
                                            ) {
                                                Modifier.focusRequester(resultsFocusRequester)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                        width = posterWidth,
                                    )
                                }
                            }
                        }
                    }
                }

                if (!state.showPrompt && !state.isSearching && results == null && state.error == null) {
                    item(key = "min-chars") {
                        Column(modifier = Modifier.padding(horizontal = gutter)) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.search_min_chars),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = SunsetColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}
