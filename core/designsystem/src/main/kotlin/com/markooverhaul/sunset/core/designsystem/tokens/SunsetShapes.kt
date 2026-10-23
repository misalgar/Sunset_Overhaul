package com.markooverhaul.sunset.core.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radius tokens from Appendix C. */
object SunsetRadius {
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Pill = 999.dp
}

val SunsetShapes = Shapes(
    extraSmall = RoundedCornerShape(SunsetRadius.Sm),
    small = RoundedCornerShape(SunsetRadius.Sm),
    medium = RoundedCornerShape(SunsetRadius.Md),
    large = RoundedCornerShape(SunsetRadius.Lg),
    extraLarge = RoundedCornerShape(SunsetRadius.Xl),
)
