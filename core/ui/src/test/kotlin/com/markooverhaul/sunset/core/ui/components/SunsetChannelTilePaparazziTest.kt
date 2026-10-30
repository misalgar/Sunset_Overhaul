package com.markooverhaul.sunset.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.LiveChannel
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM-only visual snapshots of the dense live tile that fills the 2-column TV grid.
 *
 * The device is a 1920x1080 landscape canvas at xhdpi, which is the standard TV dp viewport
 * (960x540 dp). The tile is rendered at the width of one cell of the `GridCells.Fixed(2)` grid
 * the Live screen uses, so the golden shows the real column width rather than a stretched card.
 *
 * Run `recordPaparazziDebug` to refresh the goldens and `verifyPaparazziDebug` to compare
 * against them.
 */
class SunsetChannelTilePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
            screenWidth = 1920,
            screenHeight = 1080,
            orientation = ScreenOrientation.LANDSCAPE,
            // 320 dpi (xhdpi) is the 1080p TV canvas: 1920x1080 px becomes 960x540 dp.
            // PIXEL_6_PRO ships 560 dpi, which would give a 548 dp phone-width viewport.
            density = Density.XHIGH,
            locale = "en-rUS",
        ),
    )

    private lateinit var defaultLocale: Locale
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinLocaleAndTimeZone() {
        defaultLocale = Locale.getDefault()
        defaultTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun snapshotUnfocusedTile() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                Column {
                    Tile(ShortChannel)
                    // A long name drops to the 14 sp title style and must ellipsize inside the
                    // column instead of pushing the status row out of the tile.
                    Tile(LongChannel)
                }
            }
        }
    }

    @Test
    fun snapshotFocusedTile() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                val focusRequester = remember { FocusRequester() }
                // The tile owns its focus state, so the only way to freeze it in the focused
                // state is to hand the real focus to it, exactly as LiveScreen does.
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Box(modifier = Modifier.width(TileWidth)) {
                    SunsetLiveChannelTile(
                        channel = ShortChannel,
                        nowTitle = null,
                        isFavourite = false,
                        favouriteLabel = "Add to favourites",
                        unfavouriteLabel = "Remove from favourites",
                        onPlay = {},
                        onToggleFavourite = {},
                        liveStatusText = LiveStatusText,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
            }
        }
    }

    @Test
    fun snapshotFocusedLongNameTile() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                val focusRequester = remember { FocusRequester() }
                // A focused overflowing title runs the marquee; at t=0 the animation sits at its
                // initial offset, so the golden freezes the full name clipped at the column edge
                // (no ellipsis) instead of stuttering off the marquee mid-loop.
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Box(modifier = Modifier.width(TileWidth)) {
                    SunsetLiveChannelTile(
                        channel = LongChannel,
                        nowTitle = null,
                        isFavourite = false,
                        favouriteLabel = "Add to favourites",
                        unfavouriteLabel = "Remove from favourites",
                        onPlay = {},
                        onToggleFavourite = {},
                        liveStatusText = LiveStatusText,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
            }
        }
    }

    @Composable
    private fun Tile(channel: LiveChannel) {
        Box(modifier = Modifier.width(TileWidth)) {
            SunsetLiveChannelTile(
                channel = channel,
                nowTitle = null,
                isFavourite = false,
                favouriteLabel = "Add to favourites",
                unfavouriteLabel = "Remove from favourites",
                onPlay = {},
                onToggleFavourite = {},
                liveStatusText = LiveStatusText,
            )
        }
    }

    private companion object {
        /**
         * One cell of the Live screen's 2-column grid on a 960 dp TV canvas:
         * 960 dp - 248 dp sidebar - 24 dp padding - 12 dp gutter, halved.
         */
        val TileWidth = 332.dp

        /** Event feeds render the start time instead of the programme title. */
        const val LiveStatusText = "Live • 6:15 AM"

        val ShortChannel = LiveChannel(
            id = "bbc-one",
            name = "BBC",
            number = 1,
        )

        val LongChannel = LiveChannel(
            id = "sky-sports-main-event",
            name = "Sky Sports Main Event HD 4K Dolby Vision Atmos",
            number = 2,
        )
    }
}
