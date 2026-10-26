package com.markooverhaul.sunset.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetSkeleton
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetSpacing
import com.markooverhaul.sunset.core.model.EpgProgramme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.core.ui.components.ChannelLogo
import com.markooverhaul.sunset.core.ui.components.SunsetDemoBanner
import com.markooverhaul.sunset.core.ui.components.artworkGradient
import com.markooverhaul.sunset.core.ui.util.SunsetFormat
import com.markooverhaul.sunset.core.ui.util.providerErrorText
import java.util.Locale
import kotlinx.coroutines.delay

private const val DAY_MS = 86_400_000L
private const val SLOT_MINUTES = 30
private const val SLOT_COUNT = 48
private const val MINUTE_MS = 60_000L

/** The programme currently shown in the preview panel. */
private data class GuideSelection(
    val channel: LiveChannel,
    val programme: EpgProgramme,
)

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
        onSelectCountry = viewModel::selectCountry,
        onOpenCountryPicker = viewModel::openCountryPicker,
        onDismissCountryPicker = viewModel::dismissCountryPicker,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

/**
 * TV guide modelled on a broadcast grid: a programme preview on top, a time ruler, then one row
 * per channel with blocks sized by their real duration. The preview follows the focused block, so
 * reading the grid never requires opening a detail screen.
 */
@Composable
fun GuideScreen(
    state: GuideUiState,
    onPlayChannel: (LiveChannel) -> Unit,
    onBack: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
    onSelectCountry: (String?) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onDismissCountryPicker: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val gutter = if (isTv) SunsetSpacing.Xxl else SunsetSpacing.Lg
    val channelColumn = if (isTv) 208.dp else 124.dp
    val slotWidth = if (isTv) 150.dp else 112.dp
    val rowHeight = if (isTv) 52.dp else 56.dp
    val dayWidth = slotWidth * SLOT_COUNT.toFloat()
    val minuteWidth = slotWidth / SLOT_MINUTES.toFloat()

    val dayStart = GuideViewModel.startOfDay(state.selectedDay)
    val dayEnd = dayStart + DAY_MS
    val horizontalState = rememberScrollState()
    val todayFocus = remember { FocusRequester() }
    val tomorrowFocus = remember { FocusRequester() }
    val favouritesFocus = remember { FocusRequester() }
    val countryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val watchFocus = remember { FocusRequester() }
    val activeTabFocus = when {
        state.favouritesOnly -> favouritesFocus
        state.selectedDay == 0 -> todayFocus
        else -> tomorrowFocus
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE_MS)
            now = System.currentTimeMillis()
        }
    }

    // A programme can span midnight, so keep everything that overlaps the visible day.
    val programmesByChannel = remember(state.programmes, dayStart) {
        state.programmes
            .filter { it.endUtcMs > dayStart && it.startUtcMs < dayEnd }
            .groupBy { it.channelId }
    }

    val fallbackSelection = remember(programmesByChannel, state.visibleChannels, now, dayStart) {
        state.visibleChannels.firstNotNullOfOrNull { channel ->
            val programmes = programmesByChannel[channel.id].orEmpty()
            val programme = programmes.firstOrNull { now >= it.startUtcMs && now < it.endUtcMs }
                ?: programmes.firstOrNull()
            programme?.let { GuideSelection(channel, it) }
        }
    }
    var selection by remember { mutableStateOf<GuideSelection?>(null) }
    LaunchedEffect(dayStart, state.favouritesOnly) { selection = null }
    val preview = selection ?: fallbackSelection

    // Start the grid two slots before now, snapped to a slot boundary so the first ruler label is
    // never half-clipped.
    LaunchedEffect(dayStart, density, slotWidth) {
        val rawMinutes = ((now - dayStart) / MINUTE_MS).toInt() - SLOT_MINUTES
        val minutes = ((rawMinutes / SLOT_MINUTES) * SLOT_MINUTES).coerceAtLeast(0)
        val offset = with(density) { (minuteWidth * minutes.toFloat()).roundToPx() }
        horizontalState.scrollTo(offset)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SunsetDemoBanner()

        GuideTopBar(
            now = now,
            selectedDay = state.selectedDay,
            dayStart = dayStart,
            gutter = gutter,
            onBack = onBack,
            showBack = !isTv,
        )

        if (state.error == null && state.visibleChannels.isNotEmpty()) {
            GuidePreview(
                selection = preview,
                now = now,
                gutter = gutter,
                isTv = isTv,
                watchFocus = watchFocus,
                tabFocus = activeTabFocus,
                onWatch = { preview?.let { onPlayChannel(it.channel) } },
            )
        }

        GuideFilterRow(
            state = state,
            gutter = gutter,
            isTv = isTv,
            onSelectDay = onSelectDay,
            onToggleFavouritesOnly = onToggleFavouritesOnly,
            onOpenCountryPicker = onOpenCountryPicker,
            todayFocus = todayFocus,
            tomorrowFocus = tomorrowFocus,
            favouritesFocus = favouritesFocus,
            countryFocus = countryFocus,
            watchFocus = watchFocus,
            gridFocus = gridFocus,
        )

        // The panel currently publishes channels without programmes. Explain it once instead of
        // repeating "No guide data" on every row.
        val epgUnavailable = !state.isLoading &&
            state.error == null &&
            state.visibleChannels.isNotEmpty() &&
            state.programmes.isEmpty()
        if (epgUnavailable) {
            GuideEpgUnavailableBanner(gutter = gutter)
        }

        when {
            state.isLoading -> GuideLoading(gutter = gutter)

            state.error != null -> SunsetErrorState(
                title = stringResource(R.string.guide_data_unavailable),
                body = providerErrorText(state.error),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = gutter),
            )

            state.visibleChannels.isEmpty() -> SunsetEmptyState(
                title = stringResource(R.string.guide_no_channels),
                body = stringResource(R.string.guide_empty_body),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = gutter),
            )

            else -> {
                GuideRuler(
                    dayStart = dayStart,
                    now = now,
                    channelColumn = channelColumn,
                    slotWidth = slotWidth,
                    gutter = gutter,
                    scrollState = horizontalState,
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = SunsetSpacing.Xl),
                ) {
                    itemsIndexed(
                        items = state.visibleChannels,
                        key = { _, channel -> channel.id },
                    ) { index, channel ->
                        GuideChannelRow(
                            channel = channel,
                            programmes = programmesByChannel[channel.id].orEmpty(),
                            now = now,
                            dayStart = dayStart,
                            dayEnd = dayEnd,
                            minuteWidth = minuteWidth,
                            dayWidth = dayWidth,
                            channelColumn = channelColumn,
                            rowHeight = rowHeight,
                            gutter = gutter,
                            striped = index % 2 == 0,
                            scrollState = horizontalState,
                            firstRow = index == 0,
                            showNoDataLabel = !epgUnavailable,
                            gridFocus = gridFocus,
                            tabFocus = activeTabFocus,
                            onFocusProgramme = { programme ->
                                selection = GuideSelection(channel, programme)
                            },
                            onPlayChannel = onPlayChannel,
                        )
                    }
                }
            }
        }
    }

    if (state.countryPickerOpen) {
        GuideCountryDialog(
            channels = state.channels,
            selectedCountry = state.selectedCountry,
            onSelect = onSelectCountry,
            onDismiss = onDismissCountryPicker,
        )
    }
}

@Composable
private fun GuideEpgUnavailableBanner(gutter: Dp) {
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter, vertical = SunsetSpacing.Sm)
            .background(SunsetColors.BgSurface2, shape)
            .border(1.dp, SunsetColors.BorderSubtle, shape)
            .padding(horizontal = SunsetSpacing.Md, vertical = SunsetSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = SunsetColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.guide_epg_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = SunsetColors.TextSecondary,
        )
    }
}

@Composable
private fun GuideTopBar(
    now: Long,
    selectedDay: Int,
    dayStart: Long,
    gutter: Dp,
    onBack: () -> Unit,
    showBack: Boolean,
) {
    val context = LocalContext.current
    val stamp = if (selectedDay == 0) {
        stringResource(
            R.string.guide_date_time,
            SunsetFormat.date(context, now),
            SunsetFormat.time(context, now),
        )
    } else {
        stringResource(
            R.string.guide_day_date,
            stringResource(R.string.guide_tomorrow),
            SunsetFormat.date(context, dayStart),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter, vertical = SunsetSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
    ) {
        if (showBack) {
            SunsetFocusable(
                onClick = onBack,
                shape = RoundedCornerShape(SunsetRadius.Pill),
                scaleOnFocus = false,
            ) {
                Text(
                    text = stringResource(R.string.action_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = SunsetColors.TextSecondary,
                    modifier = Modifier
                        .background(SunsetColors.BgSurface2, RoundedCornerShape(SunsetRadius.Pill))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.guide_title),
            style = MaterialTheme.typography.headlineSmall,
            color = SunsetColors.TextPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stamp,
            style = MaterialTheme.typography.labelLarge,
            color = SunsetColors.TextSecondary,
        )
    }
}

@Composable
private fun GuidePreview(
    selection: GuideSelection?,
    now: Long,
    gutter: Dp,
    isTv: Boolean,
    watchFocus: FocusRequester,
    tabFocus: FocusRequester,
    onWatch: () -> Unit,
) {
    val context = LocalContext.current
    val height = if (isTv) 128.dp else 138.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter)
            .height(height)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        SunsetColors.BgSurface3,
                        SunsetColors.BgSurface,
                        SunsetColors.BgBase,
                    ),
                ),
            ),
    ) {
        val showArtwork = maxWidth >= 520.dp
        if (selection == null) {
            Text(
                text = stringResource(R.string.guide_select_programme),
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = SunsetSpacing.Xl),
            )
            return@BoxWithConstraints
        }

        val programme = selection.programme
        val channel = selection.channel
        val isNow = now >= programme.startUtcMs && now < programme.endUtcMs
        val meta = buildString {
            append(
                SunsetFormat.timeRange(context, programme.startUtcMs, programme.endUtcMs),
            )
            append("  ·  ")
            if (isNow) {
                append(SunsetFormat.remaining(context, now, programme.endUtcMs).orEmpty())
            } else {
                val minutes = ((programme.endUtcMs - programme.startUtcMs) / MINUTE_MS).toInt()
                append(SunsetFormat.runtime(context, minutes).orEmpty())
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SunsetSpacing.Lg, vertical = SunsetSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
        ) {
            ChannelLogo(
                name = channel.name,
                logoUrl = channel.logoUrl,
                modifier = Modifier.size(if (isTv) 64.dp else 52.dp),
                size = if (isTv) 54.dp else 42.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "${channel.number}  ${channel.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SunsetColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = programme.title,
                    style = if (isTv) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = SunsetColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
                ) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isNow) SunsetColors.AccentPrimary else SunsetColors.TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    GuideAction(
                        text = stringResource(R.string.guide_watch_channel),
                        onClick = onWatch,
                        focusRequester = watchFocus,
                        downFocus = tabFocus,
                    )
                }
                programme.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showArtwork) {
                GuidePreviewArtwork(
                    programme = programme,
                    channel = channel,
                    modifier = Modifier
                        .widthIn(max = if (isTv) 176.dp else 150.dp)
                        .aspectRatio(16f / 9f),
                )
            }
        }
    }
}

@Composable
private fun GuidePreviewArtwork(
    programme: EpgProgramme,
    channel: LiveChannel,
    modifier: Modifier = Modifier,
) {
    val imageUrl = programme.iconUrl?.takeIf { it.isNotBlank() }
        ?: channel.logoUrl?.takeIf { it.isNotBlank() }
    val gradient = remember(programme.title) { artworkGradient(programme.title) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SunsetRadius.Sm))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = if (programme.iconUrl.isNullOrBlank()) {
                    ContentScale.Fit
                } else {
                    ContentScale.Crop
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (programme.iconUrl.isNullOrBlank()) 18.dp else 0.dp),
            )
        } else {
            Text(
                text = programme.title.take(2).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = SunsetColors.TextPrimary.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun GuideFilterRow(
    state: GuideUiState,
    gutter: Dp,
    isTv: Boolean,
    onSelectDay: (Int) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
    onOpenCountryPicker: () -> Unit,
    todayFocus: FocusRequester,
    tomorrowFocus: FocusRequester,
    favouritesFocus: FocusRequester,
    countryFocus: FocusRequester,
    watchFocus: FocusRequester,
    gridFocus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter, vertical = SunsetSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(
            if (isTv) SunsetSpacing.Xl else SunsetSpacing.Md,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GuideTab(
            text = stringResource(R.string.guide_today),
            selected = state.selectedDay == 0,
            onClick = { onSelectDay(0) },
            focusRequester = todayFocus,
            upFocus = watchFocus,
            downFocus = gridFocus,
        )
        GuideTab(
            text = stringResource(R.string.guide_tomorrow),
            selected = state.selectedDay == 1,
            onClick = { onSelectDay(1) },
            focusRequester = tomorrowFocus,
            upFocus = watchFocus,
            downFocus = gridFocus,
        )
        GuideTab(
            text = stringResource(R.string.guide_favourites),
            selected = state.favouritesOnly,
            onClick = onToggleFavouritesOnly,
            focusRequester = favouritesFocus,
            upFocus = watchFocus,
            downFocus = gridFocus,
        )
        Spacer(modifier = Modifier.weight(1f))
        GuideTab(
            text = countryFilterLabel(state.selectedCountry),
            selected = state.selectedCountry != null,
            onClick = onOpenCountryPicker,
            focusRequester = countryFocus,
            upFocus = watchFocus,
            downFocus = gridFocus,
        )
    }
}

@Composable
private fun countryFilterLabel(code: String?): String = when (code) {
    null -> stringResource(R.string.guide_country_all)
    GuideViewModel.OTHER_COUNTRY -> stringResource(R.string.guide_country_other)
    else -> Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }
}

/** Text-style filter tab: no pill, amber text and a 2 dp underline when selected or focused. */
@Composable
private fun GuideTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    upFocus: FocusRequester,
    downFocus: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .then(requesterModifier)
            .focusProperties {
                up = upFocus
                down = downFocus
            },
        shape = RectangleShape,
        scaleOnFocus = false,
        drawFocusRing = false,
        onFocusChanged = { focused = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    if (focused) SunsetColors.AccentPrimary.copy(alpha = 0.10f) else Color.Transparent,
                )
                .padding(horizontal = SunsetSpacing.Xs),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    focused -> SunsetColors.AccentHover
                    selected -> SunsetColors.AccentPrimary
                    else -> SunsetColors.TextSecondary
                },
                modifier = Modifier.drawBehind {
                    if (selected) {
                        val y = size.height + 3.dp.toPx()
                        drawLine(
                            color = SunsetColors.AccentPrimary,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            )
        }
    }
}

/** Borderless text action used by the preview: icon + label, underline on focus. */
@Composable
private fun GuideAction(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    downFocus: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .focusRequester(focusRequester)
            .focusProperties { down = downFocus },
        shape = RectangleShape,
        scaleOnFocus = false,
        drawFocusRing = false,
        onFocusChanged = { focused = it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    if (focused) SunsetColors.AccentPrimary.copy(alpha = 0.10f) else Color.Transparent,
                )
                .padding(horizontal = SunsetSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.drawBehind {
                    if (focused) {
                        val y = size.height + 3.dp.toPx()
                        drawLine(
                            color = SunsetColors.AccentPrimary,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (focused) SunsetColors.AccentHover else SunsetColors.AccentPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (focused) SunsetColors.AccentHover else SunsetColors.AccentPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GuideCountryDialog(
    channels: List<LiveChannel>,
    selectedCountry: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val counts = remember(channels) { channels.groupingBy { it.countryCode }.eachCount() }
    val countries = remember(counts) {
        counts.entries
            .filter { it.key != null }
            .sortedWith(
                compareByDescending<Map.Entry<String?, Int>> { it.value }
                    .thenBy { it.key.orEmpty() },
            )
            .mapNotNull { it.key }
    }
    val otherCount = counts[null] ?: 0
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { initialFocus.requestFocus() }
    }
    val selectedKey = selectedCountry ?: "all"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SunsetColors.ScrimSolid.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.8f)
                    .background(
                        SunsetColors.BgSurface2,
                        RoundedCornerShape(SunsetRadius.Lg),
                    )
                    .padding(SunsetSpacing.Lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.guide_country_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = SunsetColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    GuideLink(
                        text = stringResource(R.string.guide_country_close),
                        onClick = onDismiss,
                    )
                }
                Spacer(modifier = Modifier.height(SunsetSpacing.Sm))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "all") {
                        CountryRow(
                            label = stringResource(R.string.guide_country_all),
                            count = channels.size,
                            selected = selectedCountry == null,
                            focusRequester = if (selectedKey == "all") initialFocus else null,
                            onClick = { onSelect(null) },
                        )
                    }
                    items(countries, key = { it }) { code ->
                        CountryRow(
                            label = displayCountry(code),
                            count = counts[code] ?: 0,
                            selected = selectedCountry == code,
                            focusRequester = if (selectedKey == code) initialFocus else null,
                            onClick = { onSelect(code) },
                        )
                    }
                    item(key = "other") {
                        CountryRow(
                            label = stringResource(R.string.guide_country_other),
                            count = otherCount,
                            selected = selectedCountry == GuideViewModel.OTHER_COUNTRY,
                            focusRequester = if (selectedKey == GuideViewModel.OTHER_COUNTRY) {
                                initialFocus
                            } else {
                                null
                            },
                            onClick = { onSelect(GuideViewModel.OTHER_COUNTRY) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(
    label: String,
    count: Int,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(requesterModifier),
        shape = RectangleShape,
        scaleOnFocus = false,
        drawFocusRing = false,
        onFocusChanged = { focused = it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        focused -> SunsetColors.AccentPrimary.copy(alpha = 0.14f)
                        selected -> SunsetColors.AccentPrimary.copy(alpha = 0.06f)
                        else -> Color.Transparent
                    },
                )
                .drawBehind {
                    drawLine(
                        color = SunsetColors.BorderSubtle.copy(alpha = 0.45f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(horizontal = SunsetSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    focused || selected -> SunsetColors.AccentPrimary
                    else -> SunsetColors.TextPrimary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun GuideLink(
    text: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    SunsetFocusable(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = RectangleShape,
        scaleOnFocus = false,
        drawFocusRing = false,
        onFocusChanged = { focused = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = SunsetSpacing.Sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (focused) SunsetColors.AccentHover else SunsetColors.AccentPrimary,
                modifier = Modifier.drawBehind {
                    if (focused) {
                        val y = size.height + 3.dp.toPx()
                        drawLine(
                            color = SunsetColors.AccentPrimary,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            )
        }
    }
}

private fun displayCountry(code: String): String =
    Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }

@Composable
private fun GuideRuler(
    dayStart: Long,
    now: Long,
    channelColumn: Dp,
    slotWidth: Dp,
    gutter: Dp,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val context = LocalContext.current
    val dayWidth = slotWidth * SLOT_COUNT.toFloat()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .drawBehind {
                drawLine(
                    color = SunsetColors.BorderSubtle.copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = gutter),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(modifier = Modifier.width(channelColumn))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .horizontalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .fillMaxHeight()
                    .drawBehind {
                        val slotPx = slotWidth.toPx()
                        val nowX = (((now - dayStart).toFloat() / MINUTE_MS) *
                            (slotPx / SLOT_MINUTES.toFloat()))
                        if (nowX in 0f..size.width) {
                            drawLine(
                                color = SunsetColors.SemanticLive,
                                start = Offset(nowX, 0f),
                                end = Offset(nowX, size.height),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                            val marker = Path().apply {
                                moveTo(nowX - 5.dp.toPx(), size.height)
                                lineTo(nowX + 5.dp.toPx(), size.height)
                                lineTo(nowX, size.height - 6.dp.toPx())
                                close()
                            }
                            drawPath(marker, SunsetColors.SemanticLive)
                        }
                    },
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    repeat(SLOT_COUNT) { slot ->
                        Text(
                            text = SunsetFormat.time(context, dayStart + slot * SLOT_MINUTES * MINUTE_MS),
                            style = MaterialTheme.typography.labelMedium,
                            color = SunsetColors.TextSecondary,
                            modifier = Modifier
                                .width(slotWidth)
                                .padding(start = SunsetSpacing.Sm, bottom = SunsetSpacing.Xs),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideChannelRow(
    channel: LiveChannel,
    programmes: List<EpgProgramme>,
    now: Long,
    dayStart: Long,
    dayEnd: Long,
    minuteWidth: Dp,
    dayWidth: Dp,
    channelColumn: Dp,
    rowHeight: Dp,
    gutter: Dp,
    striped: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    firstRow: Boolean,
    showNoDataLabel: Boolean,
    gridFocus: FocusRequester,
    tabFocus: FocusRequester,
    onFocusProgramme: (EpgProgramme) -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
) {
    val rowBackground = if (striped) {
        SunsetColors.BgSurface.copy(alpha = 0.30f)
    } else {
        Color.Transparent
    }
    var channelFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(rowBackground)
            .drawBehind {
                drawLine(
                    color = SunsetColors.BorderSubtle.copy(alpha = 0.45f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunsetFocusable(
            onClick = { onPlayChannel(channel) },
            modifier = Modifier
                .width(channelColumn)
                .fillMaxHeight()
                .then(
                    if (firstRow) {
                        Modifier
                            .focusRequester(gridFocus)
                            .focusProperties { up = tabFocus }
                    } else {
                        Modifier
                    },
                ),
            shape = RectangleShape,
            scaleOnFocus = false,
            drawFocusRing = false,
            onFocusChanged = { channelFocused = it },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (channelFocused) {
                            SunsetColors.AccentPrimary.copy(alpha = 0.16f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .drawBehind {
                        if (channelFocused) {
                            drawRect(
                                color = SunsetColors.AccentPrimary,
                                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                            )
                        }
                        drawLine(
                            color = SunsetColors.BorderSubtle.copy(alpha = 0.6f),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(start = SunsetSpacing.Sm, end = SunsetSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
            ) {
                Text(
                    text = channel.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (channelFocused) SunsetColors.AccentPrimary else SunsetColors.TextTertiary,
                    modifier = Modifier.widthIn(min = 20.dp),
                )
                ChannelLogo(
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    modifier = Modifier.size(34.dp),
                    size = 28.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = if (programmes.isEmpty() && showNoDataLabel) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (programmes.isEmpty() && showNoDataLabel) {
                        Text(
                            text = stringResource(R.string.guide_no_data_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetColors.TextTertiary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .horizontalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .fillMaxHeight(),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    var cursorMinutes = 0f
                    programmes.forEach { programme ->
                        val startMinutes = ((programme.startUtcMs.coerceAtLeast(dayStart) - dayStart)
                            .toFloat() / MINUTE_MS)
                        val endMinutes = ((programme.endUtcMs.coerceAtMost(dayEnd) - dayStart)
                            .toFloat() / MINUTE_MS)
                        // Sequential layout keeps focus traversal aligned with the timeline. Absolute
                        // offsets made the focus engine treat every block as being at the row start,
                        // so D-pad left/right did not follow the grid.
                        val gap = (startMinutes - cursorMinutes).coerceAtLeast(0f)
                        if (gap > 0f) {
                            Spacer(modifier = Modifier.width(minuteWidth * gap))
                        }
                        GuideProgrammeBlock(
                            programme = programme,
                            now = now,
                            width = minuteWidth * (endMinutes - startMinutes).coerceAtLeast(1f),
                            rowHeight = rowHeight,
                            onFocus = { onFocusProgramme(programme) },
                            onClick = { onPlayChannel(channel) },
                        )
                        cursorMinutes = endMinutes
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideProgrammeBlock(
    programme: EpgProgramme,
    now: Long,
    width: Dp,
    rowHeight: Dp,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isNow = now >= programme.startUtcMs && now < programme.endUtcMs
    var focused by remember { mutableStateOf(false) }
    val progress = if (isNow) {
        ((now - programme.startUtcMs).toFloat() /
            (programme.endUtcMs - programme.startUtcMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val cellBackground = when {
        focused -> SunsetColors.AccentPrimary
        isNow -> SunsetColors.AccentPrimary.copy(alpha = 0.07f)
        else -> Color.Transparent
    }
    val titleColor = when {
        focused -> SunsetColors.AccentOn
        isNow -> SunsetColors.AccentPrimary
        else -> SunsetColors.TextPrimary
    }
    val timeColor = when {
        focused -> SunsetColors.AccentOn.copy(alpha = 0.72f)
        isNow -> SunsetColors.AccentPrimary.copy(alpha = 0.78f)
        else -> SunsetColors.TextTertiary
    }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .drawBehind {
                // Programme boundaries are the only vertical lines in the grid; there are no
                // per-block cards, so the schedule reads as one continuous surface.
                drawLine(
                    color = SunsetColors.BorderSubtle.copy(alpha = 0.38f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        SunsetFocusable(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            scaleOnFocus = false,
            drawFocusRing = false,
            onFocusChanged = {
                focused = it
                if (it) onFocus()
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (cellBackground != Color.Transparent) {
                            drawRect(color = cellBackground)
                        }
                        if (isNow) {
                            val x = ((now - programme.startUtcMs).toFloat() /
                                (programme.endUtcMs - programme.startUtcMs)
                                    .coerceAtLeast(1L))
                                .coerceIn(0f, 1f) * size.width
                            drawLine(
                                color = if (focused) {
                                    SunsetColors.AccentOn.copy(alpha = 0.55f)
                                } else {
                                    SunsetColors.SemanticLive
                                },
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = programme.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (width >= 112.dp) {
                        Text(
                            text = SunsetFormat.timeRange(
                                context,
                                programme.startUtcMs,
                                programme.endUtcMs,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = timeColor,
                            maxLines = 1,
                        )
                    }
                }
                if (isNow) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (focused) {
                                    SunsetColors.AccentOn.copy(alpha = 0.25f)
                                } else {
                                    SunsetColors.BorderSubtle
                                },
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    if (focused) SunsetColors.AccentOn else SunsetColors.SemanticLive,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideLoading(gutter: Dp) {
    Column(
        modifier = Modifier.padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(SunsetSpacing.Md),
    ) {
        repeat(4) {
            SunsetSkeleton(modifier = Modifier.fillMaxWidth().height(72.dp))
        }
    }
}
