package com.markooverhaul.sunset.core.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from spec Appendix C. Hex values are normative; keep them in sync with the spec.
 * Never introduce feature-local colours — add a token here instead.
 */
object SunsetColors {
    // Backgrounds
    val BgBase = Color(0xFF08090C)
    val BgSurface = Color(0xFF101218)
    val BgSurface2 = Color(0xFF171A21)
    val BgSurface3 = Color(0xFF1F232C)

    // Borders
    val BorderSubtle = Color(0xFF262A33)

    // Accent
    val AccentPrimary = Color(0xFFF5A524)
    val AccentHover = Color(0xFFFFC14D)
    val AccentPressed = Color(0xFFD98A12)
    val AccentOn = Color(0xFF0B0B0F)

    // Text
    val TextPrimary = Color(0xFFF7F7F8)
    val TextSecondary = Color(0xFFA7ABB5)
    val TextTertiary = Color(0xFF7C818D)

    // Semantic
    val SemanticLive = Color(0xFFFF453A)
    val SemanticSuccess = Color(0xFF32D583)
    val SemanticWarning = Color(0xFFF5A524)
    val SemanticError = Color(0xFFFF6B6B)

    // Focus
    val FocusRing = Color(0xFFFFD166)
    val FocusOuterStroke = Color(0xCC000000)

    // Scrims
    val ScrimClear = Color(0x00000000)
    val ScrimSolid = Color(0xFF08090C)
    val ScrimLeft = Color(0xE608090C)
    val ScrimLeftClear = Color(0x0008090C)
}
