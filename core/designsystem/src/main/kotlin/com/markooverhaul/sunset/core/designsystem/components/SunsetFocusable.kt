package com.markooverhaul.sunset.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markooverhaul.sunset.core.designsystem.tokens.LocalReduceMotion
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius

/**
 * TV focus ring: 2 dp amber inside a dark outer stroke so it stays visible on bright artwork
 * (spec §7.3). Focus state never changes layout.
 *
 * [gap] draws the ring outside the shape with that much background between the edge and the ring.
 * Filled buttons use a gap so the amber ring is not lost against an amber fill; cards keep the
 * flush ring so it hugs the artwork.
 */
@Composable
fun Modifier.sunsetFocusRing(
    focused: Boolean,
    shape: Shape = RoundedCornerShape(SunsetRadius.Md),
    gap: Dp = 0.dp,
): Modifier {
    if (!focused) return this
    if (gap > 0.dp) return this.sunsetHaloRing(shape, gap)
    return this
        .border(width = 1.dp, color = SunsetColors.FocusOuterStroke, shape = shape)
        .border(width = 2.dp, color = SunsetColors.FocusRing, shape = shape)
}

/**
 * Draws the focus ring just outside the shape. Kept to strokes so nothing is rasterised or scaled,
 * which is what made the old focus lift look pixelated on TV.
 */
private fun Modifier.sunsetHaloRing(shape: Shape, gap: Dp): Modifier = drawWithContent {
    drawContent()
    val rounded = (shape.createOutline(size, layoutDirection, this) as? Outline.Rounded)?.roundRect
        ?: return@drawWithContent
    val gapPx = gap.toPx()
    val ringPx = 2.dp.toPx()
    val outerPx = 1.dp.toPx()

    val ringRect = rounded.inflatedBy(gapPx + ringPx / 2f)
    drawRoundRect(
        color = SunsetColors.FocusRing,
        topLeft = Offset(ringRect.left, ringRect.top),
        size = Size(ringRect.width, ringRect.height),
        cornerRadius = CornerRadius(ringRect.topLeftCornerRadius.x, ringRect.topLeftCornerRadius.y),
        style = Stroke(width = ringPx),
    )

    val outerRect = rounded.inflatedBy(gapPx + ringPx + outerPx / 2f)
    drawRoundRect(
        color = SunsetColors.FocusOuterStroke,
        topLeft = Offset(outerRect.left, outerRect.top),
        size = Size(outerRect.width, outerRect.height),
        cornerRadius = CornerRadius(outerRect.topLeftCornerRadius.x, outerRect.topLeftCornerRadius.y),
        style = Stroke(width = outerPx),
    )
}

/** Grows a rounded rect by [delta] on every side, keeping the corner arcs concentric. */
private fun RoundRect.inflatedBy(delta: Float): RoundRect = RoundRect(
    left = left - delta,
    top = top - delta,
    right = right + delta,
    bottom = bottom + delta,
    topLeftCornerRadius = CornerRadius(
        topLeftCornerRadius.x + delta,
        topLeftCornerRadius.y + delta,
    ),
    topRightCornerRadius = CornerRadius(
        topRightCornerRadius.x + delta,
        topRightCornerRadius.y + delta,
    ),
    bottomRightCornerRadius = CornerRadius(
        bottomRightCornerRadius.x + delta,
        bottomRightCornerRadius.y + delta,
    ),
    bottomLeftCornerRadius = CornerRadius(
        bottomLeftCornerRadius.x + delta,
        bottomLeftCornerRadius.y + delta,
    ),
)

/** Standard focus lift for cards. Disabled when the user prefers reduced motion. */
@Composable
fun Modifier.sunsetFocusScale(focused: Boolean): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (focused && !reduceMotion) 1.04f else 1f,
        label = "sunset-focus-scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable surface with a focus ring. [clickable] supplies the focus target and D-pad activation;
 * we do not add a second `focusable()` modifier, which would create duplicate focus stops.
 */
@Composable
fun SunsetFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SunsetRadius.Md),
    scaleOnFocus: Boolean = true,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focusModifier = if (scaleOnFocus) Modifier.sunsetFocusScale(focused) else Modifier
    Box(
        modifier = modifier
            .then(focusModifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .sunsetFocusRing(focused, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}
