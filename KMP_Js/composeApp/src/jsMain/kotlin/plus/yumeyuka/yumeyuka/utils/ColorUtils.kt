package plus.yumeyuka.yumeyuka.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

private fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    
    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    
    val s = if (max == 0f) 0f else delta / max
    
    val v = max
    
    return floatArrayOf(h, s, v)
}

private fun hsvToRgb(h: Float, s: Float, v: Float): FloatArray {
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return floatArrayOf(r1 + m, g1 + m, b1 + m)
}

fun Color.deepen(saturationBoost: Float = 0.4f, darknessFactor: Float = 0.7f): Color {
    val hsv = rgbToHsv(red, green, blue)
    
    hsv[1] = min(1.0f, hsv[1] + saturationBoost)
    
    hsv[2] = hsv[2] * darknessFactor
    
    val rgb = hsvToRgb(hsv[0], hsv[1], hsv[2])
    return Color(
        red = rgb[0],
        green = rgb[1],
        blue = rgb[2],
        alpha = alpha
    )
}

fun List<Color>.deepenAll(saturationBoost: Float = 0.4f, darknessFactor: Float = 0.7f): List<Color> {
    return map { it.deepen(saturationBoost, darknessFactor) }
}