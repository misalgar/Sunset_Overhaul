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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.markooverhaul.sunset.core.designsystem.components.CategoryChip
import com.markooverhaul.sunset.core.designsystem.components.SunsetEmptyState
import com.markooverhaul.sunset.core.designsystem.components.SunsetErrorState
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetPrimaryButton
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
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTelevision.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val gutter = if (isTv) SunsetSpacing.Xxxl else SunsetSpacing.Lg
    val channelColumn = if (isTv) 224.dp else 132.dp
    val slotWidth = if (isTv) 156.dp else 116.dp
    val rowHeight = if (isTv) 60.dp else 64.dp
    val dayWidth = slotWidth * SLOT_COUNT.toFloat()
    val minuteWidth = slotWidth / SLOT_MINUTES.toFloat()

    val dayStart = GuideViewModel.startOfDay(state.selectedDay)
    val dayEnd = dayStart + DAY_MS
    val horizontalState = rememberScrollState()

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

    // Start the grid two slots before now, where the viewer expects the current hour to be.
    LaunchedEffect(dayStart, density, slotWidth) {
        val minutes = ((now - dayStart).toFloat() / MINUTE_MS) - SLOT_MINUTES
        val offset = with(density) {
            (minuteWidth * minutes.coerceAtLeast(0f)).roundToPx()
        }
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
                onWatch = { preview?.let { onPlayChannel(it.channel) } },
            )
        }

        GuideFilterRow(
            state = state,
            gutter = gutter,
            onSelectDay = onSelectDay,
            onToggleFavouritesOnly = onToggleFavouritesOnly,
        )

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
                            slotWidth = slotWidth,
                            gutter = gutter,
                            striped = index % 2 == 0,
                            scrollState = horizontalState,
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
    onWatch: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(SunsetRadius.Lg)
    val height = if (isTv) 132.dp else 134.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter)
            .height(height)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(SunsetColors.BgSurface3, SunsetColors.BgSurface),
                ),
            )
            .border(1.dp, SunsetColors.BorderSubtle, shape),
    ) {
        val showArtwork = maxWidth >= 560.dp
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
            horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Lg),
        ) {
            ChannelLogo(
                name = channel.name,
                logoUrl = channel.logoUrl,
                modifier = Modifier.size(if (isTv) 72.dp else 56.dp),
                size = if (isTv) 60.dp else 44.dp,
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
                    // On TV this button is also the guaranteed focus anchor when the guide opens;
                    // Down moves into the first channel row.
                    SunsetPrimaryButton(
                        text = stringResource(R.string.guide_watch_channel),
                        onClick = onWatch,
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
                        .widthIn(max = if (isTv) 196.dp else 160.dp)
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
            .clip(RoundedCornerShape(SunsetRadius.Md))
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
    onSelectDay: (Int) -> Unit,
    onToggleFavouritesOnly: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
        contentPadding = PaddingValues(horizontal = gutter, vertical = SunsetSpacing.Sm),
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
            .padding(horizontal = gutter)
            .height(30.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.guide_channel_column).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = SunsetColors.TextTertiary,
            modifier = Modifier
                .width(channelColumn)
                .padding(start = SunsetSpacing.Xs, bottom = SunsetSpacing.Xs),
        )
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
                        for (slot in 0..SLOT_COUNT) {
                            drawLine(
                                color = SunsetColors.BorderSubtle.copy(alpha = 0.5f),
                                start = Offset(slot * slotPx, 0f),
                                end = Offset(slot * slotPx, size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        if (nowX in 0f..size.width) {
                            drawLine(
                                color = SunsetColors.SemanticLive,
                                start = Offset(nowX, 0f),
                                end = Offset(nowX, size.height),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    },
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    repeat(SLOT_COUNT) { slot ->
                        Text(
                            text = SunsetFormat.time(context, dayStart + slot * SLOT_MINUTES * MINUTE_MS),
                            style = MaterialTheme.typography.labelMedium,
                            color = SunsetColors.TextTertiary,
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
    slotWidth: Dp,
    gutter: Dp,
    striped: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onFocusProgramme: (EpgProgramme) -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
) {
    val rowBackground = if (striped) {
        SunsetColors.BgSurface.copy(alpha = 0.55f)
    } else {
        SunsetColors.BgBase
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(rowBackground)
            .padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunsetFocusable(
            onClick = { onPlayChannel(channel) },
            modifier = Modifier
                .width(channelColumn)
                .fillMaxHeight(),
            shape = RoundedCornerShape(SunsetRadius.Sm),
            scaleOnFocus = false,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLine(
                            color = SunsetColors.BorderSubtle.copy(alpha = 0.7f),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(start = SunsetSpacing.Xs, end = SunsetSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SunsetSpacing.Sm),
            ) {
                Text(
                    text = channel.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = SunsetColors.TextTertiary,
                    modifier = Modifier.widthIn(min = 22.dp),
                )
                ChannelLogo(
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    modifier = Modifier.size(38.dp),
                    size = 30.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetColors.TextSecondary,
                        maxLines = if (programmes.isEmpty()) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (programmes.isEmpty()) {
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
                    .fillMaxHeight()
                    .drawBehind {
                        val slotPx = slotWidth.toPx()
                        for (slot in 0..SLOT_COUNT) {
                            drawLine(
                                color = SunsetColors.BorderSubtle.copy(alpha = 0.5f),
                                start = Offset(slot * slotPx, 0f),
                                end = Offset(slot * slotPx, size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        val nowX = (((now - dayStart).toFloat() / MINUTE_MS) *
                            (slotPx / SLOT_MINUTES.toFloat()))
                        if (nowX in 0f..size.width) {
                            drawLine(
                                color = SunsetColors.SemanticLive.copy(alpha = 0.8f),
                                start = Offset(nowX, 0f),
                                end = Offset(nowX, size.height),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    },
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
    val shape = RoundedCornerShape(SunsetRadius.Sm)
    val background = if (isNow) {
        SunsetColors.BgSurface3
    } else {
        SunsetColors.BgSurface2
    }
    val progress = if (isNow) {
        ((now - programme.startUtcMs).toFloat() /
            (programme.endUtcMs - programme.startUtcMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(rowHeight)
            .padding(vertical = 3.dp, horizontal = 1.dp),
    ) {
        SunsetFocusable(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onFocus() },
            shape = shape,
            scaleOnFocus = false,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(horizontal = SunsetSpacing.Sm, vertical = SunsetSpacing.Xs),
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = programme.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isNow) SunsetColors.AccentPrimary else SunsetColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (width >= 110.dp) {
                        Text(
                            text = SunsetFormat.timeRange(
                                context,
                                programme.startUtcMs,
                                programme.endUtcMs,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetColors.TextTertiary,
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
                            .background(SunsetColors.BgSurface3),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(SunsetColors.SemanticLive),
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
