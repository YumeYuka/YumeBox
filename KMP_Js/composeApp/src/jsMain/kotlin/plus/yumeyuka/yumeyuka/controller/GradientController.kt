// https://github.com/ReChronoRain/HyperCeiler/tree/main/app/src/main/java/com/sevtinge/hyperceiler/main/page/about/controller

package plus.yumeyuka.yumeyuka.controller

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Stable
class GradientController(
    deviceType: DeviceType = DeviceType.PHONE,
    themeMode: ThemeMode = ThemeMode.LIGHT
) {
    private val dataManager = BgEffectDataManager()
    private var effectData: BgEffectData = dataManager.getData(deviceType, themeMode)
    
    private val colorCycle = SmoothGradientColorCycle(effectData)
    
    private var _currentColors: List<Color> = effectData.gradientColors2.map { it.toComposeColor() }
    val currentColors: List<Color> get() = _currentColors
    
    var scrollY: Int = 0
        set(value) {
            field = value
            updateScrollFactors()
        }
    
    var actionBarPadding: Int = 200
    var logoPadding: Int = 300
    var logoHeight: Int = 120
    
    private var scrollFactor: Float = 0f
    private var logoScrollFactor: Float = 0f
    
    fun updateFrame(deltaTime: Float) {
        _currentColors = colorCycle.updateAndGetColors(deltaTime)
    }
    
    private fun calculateScrollFactor(scrollY: Int, padding: Int): Float {
        return min(1.0f, max(0.0f, abs(scrollY) / padding.toFloat()))
    }
    
    private fun updateScrollFactors() {
        scrollFactor = calculateScrollFactor(scrollY, actionBarPadding)
        
        logoScrollFactor = if (scrollY >= logoPadding) {
            calculateScrollFactor(scrollY - logoPadding, logoHeight)
        } else {
            0f
        }
    }
    
    fun getBackgroundAlpha(): Float {
        return 1.0f - scrollFactor
    }
    
    fun getLogoAlpha(): Float {
        return if (scrollY >= logoPadding) {
            1.0f - logoScrollFactor
        } else {
            1.0f
        }
    }
    
    fun getLogoScale(): Float {
        return if (scrollY >= logoPadding) {
            1.0f - 0.1f * logoScrollFactor
        } else {
            1.0f - 0.1f * scrollFactor
        }
    }
    
    fun getVersionAlpha(): Float {
        return if (logoPadding > 0) {
            1.0f - scrollFactor * (actionBarPadding.toFloat() / logoPadding.toFloat())
        } else {
            1.0f - scrollFactor
        }
    }
    
    fun setTheme(deviceType: DeviceType, themeMode: ThemeMode) {
        effectData = dataManager.getData(deviceType, themeMode)
        colorCycle.reset()
        _currentColors = effectData.gradientColors2.map { it.toComposeColor() }
    }
}