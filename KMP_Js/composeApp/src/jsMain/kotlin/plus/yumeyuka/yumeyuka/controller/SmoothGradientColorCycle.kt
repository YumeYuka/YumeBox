// https://github.com/ReChronoRain/HyperCeiler/tree/main/app/src/main/java/com/sevtinge/hyperceiler/main/page/about/controller

package plus.yumeyuka.yumeyuka.controller

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SmoothGradientColorCycle(
    private val effectData: BgEffectData
) {
    private var time = 0f
    
    private val colorSets = listOf(
        effectData.gradientColors1,
        effectData.gradientColors2,
        effectData.gradientColors3
    )
    
    fun updateAndGetColors(deltaTime: Float): List<Color> {
        time += deltaTime * 1.2f
        
        val weights = calculateColorWeights(time)
        
        val baseColors = mixColorSets(colorSets, weights)
        
        return baseColors
    }
    
    private fun calculateColorWeights(t: Float): List<Float> {
        val numSets = colorSets.size
        val rawWeights = mutableListOf<Float>()
        
        for (i in 0 until numSets) {
            val phase = (i * 2f * PI.toFloat() / numSets)
            val cycleSpeed = 2f * PI.toFloat() / 8f
            
            val raw = cos(t * cycleSpeed + phase)
            val normalized = (raw + 1f) / 2f
            
            val minWeight = 0.25f
            val withFloor = minWeight + (1f - minWeight) * normalized
            
            rawWeights.add(withFloor)
        }
        
        val sum = rawWeights.sum()
        return if (sum > 0f) {
            rawWeights.map { it / sum }
        } else {
            List(numSets) { 1f / numSets }
        }
    }
    
    private fun mixColorSets(
        sets: List<Array<FloatArray>>,
        weights: List<Float>
    ): List<Color> {
        val numColors = sets[0].size
        val result = mutableListOf<Color>()
        
        for (colorIndex in 0 until numColors) {
            var r = 0f
            var g = 0f
            var b = 0f
            var a = 0f
            
            for (setIndex in sets.indices) {
                val weight = weights[setIndex]
                val color = sets[setIndex][colorIndex]
                
                r += color[0] * weight
                g += color[1] * weight
                b += color[2] * weight
                a += color[3] * weight
            }
            
            val enhancedColor = enhanceColorSaturation(r, g, b, a)
            result.add(enhancedColor)
        }
        
        return result
    }
    
    private fun enhanceColorSaturation(r: Float, g: Float, b: Float, a: Float): Color {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        if (delta > 0.3f || max < 0.01f) {
            return Color(r, g, b, a)
        }
        
        val saturationBoost = 1.3f
        val center = (max + min) / 2f
        
        val newR = center + (r - center) * saturationBoost
        val newG = center + (g - center) * saturationBoost
        val newB = center + (b - center) * saturationBoost
        
        return Color(
            red = newR.coerceIn(0f, 1f),
            green = newG.coerceIn(0f, 1f),
            blue = newB.coerceIn(0f, 1f),
            alpha = a
        )
    }
    
    fun reset() {
        time = 0f
    }
}