package com.markooverhaul.sunset.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.compositionLocalOf

/** Motion tokens from Appendix C. All durations are milliseconds. */
object SunsetMotion {
    const val Fast = 150
    const val Standard = 250
    const val Slow = 400
    const val Hero = 500

    val Easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * In-app "Reduce motion" preference, combined with the system setting by the app shell.
 * Components must use simple fades/static states when this is true.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** True on Android TV / Fire TV. Used for typography, gutters and focus behaviour. */
val LocalIsTelevision = compositionLocalOf { false }
