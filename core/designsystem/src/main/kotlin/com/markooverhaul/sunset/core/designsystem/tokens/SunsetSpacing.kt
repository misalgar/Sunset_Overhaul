package com.markooverhaul.sunset.core.designsystem.tokens

import androidx.compose.ui.unit.dp

/** Spacing scale from Appendix C: base unit 4 dp, rhythm 8 dp. */
object SunsetSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp
    val Xxxxl = 64.dp

    fun gutter(isTelevision: Boolean, isTablet: Boolean): androidx.compose.ui.unit.Dp = when {
        isTelevision -> 48.dp
        isTablet -> 24.dp
        else -> 16.dp
    }
}
