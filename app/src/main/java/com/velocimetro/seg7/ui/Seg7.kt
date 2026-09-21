package com.velocimetro.seg7.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

private val DIGIT_MASK = intArrayOf(
    0b0111111, // 0
    0b0000110, // 1
    0b1011011, // 2
    0b1001111, // 3
    0b1100110, // 4
    0b1101101, // 5
    0b1111101, // 6
    0b0000111, // 7
    0b1111111, // 8
    0b1101111  // 9
)

private const val GAP_RATIO = 0.30f
private const val DIGIT_ASPECT = 0.62f

@Composable
fun Seg7Display(
    text: String,
    modifier: Modifier = Modifier,
    onColor: Color = LedGreen,
    offColor: Color = LedGreenDim
) {
    Canvas(modifier) {
        val chars = text.take(6)
        if (chars.isEmpty()) return@Canvas

        val count = chars.length
        val totalUnits = count + (count - 1) * GAP_RATIO
        val digitWidth = min(size.height * DIGIT_ASPECT, size.width / totalUnits)
        if (digitWidth <= 0f) return@Canvas

        val digitHeight = digitWidth / DIGIT_ASPECT
        val gap = digitWidth * GAP_RATIO
        val totalWidth = count * digitWidth + (count - 1) * gap
        val startX = (size.width - totalWidth) / 2f
        val startY = (size.height - digitHeight) / 2f

        for (i in 0 until count) {
            val digit = chars[i].digitToIntOrNull()
            val mask = if (digit == null) 0 else DIGIT_MASK[digit]
            drawDigit(
                origin = Offset(startX + i * (digitWidth + gap), startY),
                width = digitWidth,
                height = digitHeight,
                mask = mask,
                onColor = onColor,
                offColor = offColor
            )
        }
    }
}

private fun DrawScope.drawDigit(
    origin: Offset,
    width: Float,
    height: Float,
    mask: Int,
    onColor: Color,
    offColor: Color
) {
    val thickness = height * 0.15f
    val gap = thickness * 0.12f
    val segments = buildSegments(origin, width, height, thickness, gap)

    val glow = listOf(
        height * 0.10f to 0.05f,
        height * 0.055f to 0.08f,
        height * 0.025f to 0.12f
    )

    for (index in 0 until 7) {
        val path = segments[index]
        val isOn = (mask shr index) and 1 == 1
        if (isOn) {
            for ((strokeWidth, alpha) in glow) {
                drawPath(path, onColor.copy(alpha = alpha), style = Stroke(strokeWidth))
            }
            drawPath(path, onColor)
        } else {
            drawPath(path, offColor)
        }
    }
}

private fun buildSegments(
    origin: Offset,
    width: Float,
    height: Float,
    thickness: Float,
    gap: Float
): List<Path> {
    val half = thickness / 2f
    val midTop = (height - thickness) / 2f
    val midBottom = (height + thickness) / 2f

    fun horizontal(x1: Float, x2: Float, y: Float): Path = Path().apply {
        moveTo(x1 + half, y)
        lineTo(x2 - half, y)
        lineTo(x2, y + half)
        lineTo(x2 - half, y + thickness)
        lineTo(x1 + half, y + thickness)
        lineTo(x1, y + half)
        close()
    }

    fun vertical(x: Float, y1: Float, y2: Float): Path = Path().apply {
        moveTo(x, y1 + half)
        lineTo(x + half, y1)
        lineTo(x + thickness, y1 + half)
        lineTo(x + thickness, y2 - half)
        lineTo(x + half, y2)
        lineTo(x, y2 - half)
        close()
    }

    val left = 0f
    val right = width - thickness

    val a = horizontal(half, width - half, 0f)
    val b = vertical(right, half + gap, midTop - gap)
    val c = vertical(right, midBottom + gap, height - half - gap)
    val d = horizontal(half, width - half, height - thickness)
    val e = vertical(left, midBottom + gap, height - half - gap)
    val f = vertical(left, half + gap, midTop - gap)
    val g = horizontal(half, width - half, midTop)

    return listOf(a, b, c, d, e, f, g).map { path ->
        path.apply { translate(origin) }
    }
}
