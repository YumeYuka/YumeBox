package plus.yumeyuka.yumeyuka.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import plus.yumeyuka.yumeyuka.controller.GradientController
import yumeyuka.composeapp.generated.resources.Res
import yumeyuka.composeapp.generated.resources.file
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedGradientVectorLogo(
    controller: GradientController,
    modifier: Modifier = Modifier,
    size: Float = 120f,
    gradientMode: GradientMode = GradientMode.LINEAR_ROTATING,
    customColors: List<androidx.compose.ui.graphics.Color>? = null
) {
    val currentColors by rememberUpdatedState(customColors ?: controller.currentColors)
    val logoAlpha by rememberUpdatedState(controller.getLogoAlpha())
    val logoScale by rememberUpdatedState(controller.getLogoScale())

    val infiniteTransition = rememberInfiniteTransition(label = "logoGradient")

    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientAngle"
    )

    val gradientOffsetX by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    val gradientOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .alpha(logoAlpha)
            .scale(logoScale)
    ) {
        Image(
            painter = painterResource(Res.drawable.file),
            contentDescription = "YumeYuka Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    val drawSize: androidx.compose.ui.geometry.Size = this.size

                    val brush = when (gradientMode) {
                        GradientMode.LINEAR_ROTATING -> createLinearRotatingBrush(
                            colors = currentColors,
                            angle = gradientAngle,
                            canvasSize = drawSize
                        )

                        GradientMode.RADIAL_MOVING -> createRadialMovingBrush(
                            colors = currentColors,
                            offsetX = gradientOffsetX,
                            offsetY = gradientOffsetY,
                            canvasSize = drawSize
                        )

                        GradientMode.LINEAR_DIAGONAL -> createLinearDiagonalBrush(
                            colors = currentColors,
                            canvasSize = drawSize
                        )
                    }

                    drawContent()

                    drawRect(
                        brush = brush,
                        blendMode = BlendMode.SrcIn
                    )
                }
                .graphicsLayer {
                    alpha = 0.99f
                    renderEffect = null
                }
        )
    }
}

enum class GradientMode {
    LINEAR_ROTATING,
    RADIAL_MOVING,
    LINEAR_DIAGONAL
}

private fun createLinearRotatingBrush(
    colors: List<androidx.compose.ui.graphics.Color>,
    angle: Float,
    canvasSize: androidx.compose.ui.geometry.Size
): Brush {
    val angleRad = angle * PI.toFloat() / 180f
    val centerX = canvasSize.width / 2
    val centerY = canvasSize.height / 2
    val radius = maxOf(canvasSize.width, canvasSize.height)

    return Brush.linearGradient(
        colors = colors,
        start = Offset(
            centerX + cos(angleRad) * radius,
            centerY + sin(angleRad) * radius
        ),
        end = Offset(
            centerX - cos(angleRad) * radius,
            centerY - sin(angleRad) * radius
        )
    )
}

private fun createRadialMovingBrush(
    colors: List<androidx.compose.ui.graphics.Color>,
    offsetX: Float,
    offsetY: Float,
    canvasSize: androidx.compose.ui.geometry.Size
): Brush {
    return Brush.radialGradient(
        colors = colors,
        center = Offset(
            canvasSize.width * offsetX,
            canvasSize.height * offsetY
        ),
        radius = canvasSize.minDimension * 0.8f
    )
}

private fun createLinearDiagonalBrush(
    colors: List<androidx.compose.ui.graphics.Color>,
    canvasSize: androidx.compose.ui.geometry.Size
): Brush {
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(canvasSize.width, canvasSize.height)
    )
}

@Composable
fun SimpleGradientVectorLogo(
    controller: GradientController,
    modifier: Modifier = Modifier,
    size: Float = 120f
) {
    AnimatedGradientVectorLogo(
        controller = controller,
        modifier = modifier,
        size = size,
        gradientMode = GradientMode.LINEAR_ROTATING
    )
}