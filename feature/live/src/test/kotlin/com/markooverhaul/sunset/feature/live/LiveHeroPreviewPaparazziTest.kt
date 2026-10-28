package com.markooverhaul.sunset.feature.live

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.LiveEventSchedule
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM-only visual snapshot of the Live hero header.
 *
 * The hero sits above the channel grid and carries the category breadcrumb, the focused
 * channel, its event status and the muted 16:9 preview, so it is the densest surface on the
 * screen and the one most likely to break on a type or spacing change.
 *
 * The device is a 1920x1080 landscape canvas at xhdpi, which is the standard TV dp viewport
 * (960x540 dp) the hero is designed against.
 *
 * Run `recordPaparazziDebug` to refresh the golden and `verifyPaparazziDebug` to compare
 * against it.
 */
class LiveHeroPreviewPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
            screenWidth = 1920,
            screenHeight = 1080,
            orientation = ScreenOrientation.LANDSCAPE,
            // 320 dpi (xhdpi) is the TV canvas: 1920x1080 px becomes 960x540 dp.
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            // AOSP resource qualifier syntax: language-rREGION. "en-US" parses to null.
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
    fun snapshotHeroPreview() {
        paparazzi.snapshot {
            SunsetTheme(isTelevision = true) {
                Hero()
            }
        }
    }

    @Composable
    private fun Hero() {
        SunsetHeroPreviewHeader(
            focusedChannel = EventChannel,
            previewChannel = null,
            // Event and PPV feeds have no EPG title, so the hero falls back to the event
            // status ("Live • 6:15 AM").
            programme = null,
            previewEngine = null,
            nowMs = NowUtcMs,
            selectedCategoryTitle = "Sports",
            channelCount = 976,
            isGridFocused = true,
        )
    }

    private companion object {
        /** 2024-09-10T06:15:00Z. */
        const val EventStartUtcMs = 1_725_948_900_000L

        /** Twenty minutes after kick-off, so the feed is on air and the start is in the past. */
        const val NowUtcMs = EventStartUtcMs + 20 * 60_000L

        /** The provider publishes the event schedule; the channel keeps only the start time. */
        val EventSchedule = LiveEventSchedule(
            startsAtUtcMs = EventStartUtcMs,
            displayName = "Fleetwood Town vs Arsenal",
        )

        val EventChannel = LiveChannel(
            id = "fleetwood-town-vs-arsenal",
            name = EventSchedule.displayName,
            number = 99,
            scheduledStartUtcMs = EventSchedule.startsAtUtcMs,
        )
    }
}
