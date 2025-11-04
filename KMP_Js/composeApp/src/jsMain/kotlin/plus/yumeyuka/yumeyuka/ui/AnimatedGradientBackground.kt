package plus.yumeyuka.yumeyuka.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import plus.yumeyuka.yumeyuka.controller.GradientController

@Composable
fun AnimatedGradientBackground(
    controller: GradientController,
    modifier: Modifier = Modifier
) {
    val currentColors by rememberUpdatedState(controller.currentColors)
    val bgAlpha by rememberUpdatedState(controller.getBackgroundAlpha())
    
    val infiniteTransition = rememberInfiniteTransition(label = "bgOffset")
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(35000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(bgAlpha)
            .background(
                brush = remember(currentColors, offsetX, offsetY, rotationAngle) {
                    createDynamicGradientBrush(
                        colors = currentColors,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        rotationAngle = rotationAngle
                    )
                }
            )
    )
}

private fun createDynamicGradientBrush(
    colors: List<androidx.compose.ui.graphics.Color>,
    offsetX: Float,
    offsetY: Float,
    rotationAngle: Float
): Brush {
    if (colors.isEmpty()) return Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.Transparent))
    
    val angleRad = rotationAngle * PI.toFloat() / 180f
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)
    
    val smoothColors = buildList {
        colors.forEachIndexed { index, color ->
            add(color)
            
            if (index < colors.size - 1) {
                val nextColor = colors[index + 1]
                
                val color1 = lerpColorWithHueShift(color, nextColor, 0.33f, -5f)
                add(color1)
                
                val color2 = lerpColorWithHueShift(color, nextColor, 0.67f, 5f)
                add(color2)
            }
        }
        
        val lastColor = colors.last()
        val firstColor = colors.first()
        add(lerpColorWithHueShift(lastColor, firstColor, 0.5f, 0f))
    }
    
    val centerX = offsetX * 1500f
    val centerY = offsetY * 1500f
    val distance = 3000f
    
    return Brush.linearGradient(
        colors = smoothColors,
        start = Offset(
            centerX - cosAngle * distance,
            centerY - sinAngle * distance
        ),
        end = Offset(
            centerX + cosAngle * distance,
            centerY + sinAngle * distance
        )
    )
}

private fun lerpColorWithHueShift(
    start: Color,
    end: Color,
    fraction: Float,
    hueShiftDegrees: Float
): Color {
    val lerped = Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
    
    return if (hueShiftDegrees != 0f) {
        shiftHueSimple(lerped, hueShiftDegrees)
    } else {
        lerped
    }
}

private fun shiftHueSimple(color: Color, degrees: Float): Color {
    if (degrees == 0f) return color
    
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    if (delta < 0.01f) return color
    
    var hue = when {
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    
    hue = (hue + degrees) % 360f
    if (hue < 0f) hue += 360f
    
    val saturation = delta / max
    val value = max
    
    val c = value * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = value - c
    
    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}