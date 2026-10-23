package com.markooverhaul.sunset.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.LocalReduceMotion
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetShapes
import com.markooverhaul.sunset.core.designsystem.tokens.sunsetTypography

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetColors.AccentPrimary,
    onPrimary = SunsetColors.AccentOn,
    primaryContainer = SunsetColors.AccentPressed,
    onPrimaryContainer = SunsetColors.AccentOn,
    secondary = SunsetColors.AccentHover,
    onSecondary = SunsetColors.AccentOn,
    tertiary = SunsetColors.SemanticSuccess,
    background = SunsetColors.BgBase,
    onBackground = SunsetColors.TextPrimary,
    surface = SunsetColors.BgSurface,
    onSurface = SunsetColors.TextPrimary,
    surfaceVariant = SunsetColors.BgSurface2,
    onSurfaceVariant = SunsetColors.TextSecondary,
    surfaceContainer = SunsetColors.BgSurface2,
    surfaceContainerHigh = SunsetColors.BgSurface3,
    outline = SunsetColors.BorderSubtle,
    outlineVariant = SunsetColors.BorderSubtle,
    error = SunsetColors.SemanticError,
    onError = SunsetColors.AccentOn,
    scrim = Color.Black,
)

/**
 * Sunset theme. Dark-only by design: the brand is a warm amber horizon on near-black, and OLED
 * artwork is authored for dark surfaces.
 */
@Composable
fun SunsetTheme(
    isTelevision: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Dark-only; read the system setting so a future light theme is an explicit decision.
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()

    CompositionLocalProvider(
        LocalIsTelevision provides isTelevision,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = SunsetColorScheme,
            typography = sunsetTypography(isTelevision),
            shapes = SunsetShapes,
            content = content,
        )
    }
}

/** Exposed for previews and for code that needs to force a specific device class. */
val LocalSunsetIsTv = staticCompositionLocalOf { false }
