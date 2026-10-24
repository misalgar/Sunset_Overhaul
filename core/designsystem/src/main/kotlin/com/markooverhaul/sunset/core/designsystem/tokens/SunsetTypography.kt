package com.markooverhaul.sunset.core.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.markooverhaul.sunset.core.designsystem.R

/**
 * Inter is the single UI family (spec §7.4). Static weights are bundled so the app renders
 * identically offline and on API 23+; no downloadable fonts are used.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val BaseTypography = Typography(
    // display/hero — 56/64, 700
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
    ),
    // display/section — 32/40, 700
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    // headline — 24/32, 600
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    // title — 20/28, 600
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // body — 16/24, 400
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // label — 14/20, 600
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // micro — 11/14, 700
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * TV text is 1.15× phone type and body text never drops below 18 sp (spec §7.4).
 */
fun sunsetTypography(isTelevision: Boolean = false): Typography {
    if (!isTelevision) return BaseTypography
    fun TextStyle.tv(): TextStyle = copy(
        fontSize = (fontSize.value * 1.15f).sp,
        lineHeight = (lineHeight.value * 1.15f).sp,
    )
    return BaseTypography.copy(
        displayLarge = BaseTypography.displayLarge.tv(),
        displayMedium = BaseTypography.displayMedium.tv(),
        displaySmall = BaseTypography.displaySmall.tv(),
        headlineLarge = BaseTypography.headlineLarge.tv(),
        headlineMedium = BaseTypography.headlineMedium.tv(),
        headlineSmall = BaseTypography.headlineSmall.tv(),
        titleLarge = BaseTypography.titleLarge.tv(),
        titleMedium = BaseTypography.titleMedium.tv(),
        titleSmall = BaseTypography.titleSmall.tv(),
        bodyLarge = BaseTypography.bodyLarge.tv().copy(fontSize = 18.4.sp, lineHeight = 27.6.sp),
        bodyMedium = BaseTypography.bodyMedium.tv().copy(fontSize = 18.4.sp, lineHeight = 27.6.sp),
        bodySmall = BaseTypography.bodySmall.tv().copy(fontSize = 15.sp, lineHeight = 21.sp),
        labelLarge = BaseTypography.labelLarge.tv(),
        labelMedium = BaseTypography.labelMedium.tv(),
        labelSmall = BaseTypography.labelSmall.tv(),
    )
}

/** Hero/title sizes for the current device class (spec §7.4: 56 TV/tablet, 36 phone). */
object SunsetDisplay {
    val HeroPhone = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    )
    val HeroLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
    )
    /** Shorter TV billboards: two lines still fit above the meta row and CTAs. */
    val HeroTvCompact = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    )
    val Wordmark = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.6.sp,
    )
}
