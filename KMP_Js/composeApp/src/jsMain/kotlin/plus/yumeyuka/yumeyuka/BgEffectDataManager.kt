// https://github.com/ReChronoRain/HyperCeiler/tree/main/app/src/main/java/com/sevtinge/hyperceiler/main/page/about/controller

package plus.yumeyuka.yumeyuka

import androidx.compose.ui.graphics.Color

enum class DeviceType {
    PHONE,
    TABLET
}

enum class ThemeMode {
    LIGHT,
    DARK
}

data class BgEffectData(
    var uTranslateY: Float = 0.0f,
    var uPoints: FloatArray = floatArrayOf(),
    var uAlphaMulti: Float = 1.0f,
    var uNoiseScale: Float = 1.5f,
    var uPointOffset: Float = 0.2f,
    var uPointRadiusMulti: Float = 1.0f,
    var uSaturateOffset: Float = 0.2f,
    var uLightOffset: Float = 0.1f,
    var uAlphaOffset: Float = 0.5f,
    var uShadowColorMulti: Float = 0.3f,
    var uShadowColorOffset: Float = 0.3f,
    var uShadowNoiseScale: Float = 5.0f,
    var uShadowOffset: Float = 0.01f,
    var colorInterpPeriod: Float = 5.0f,
    var gradientSpeedChange: Float = 1.6f,
    var gradientSpeedRest: Float = 1.05f,
    var gradientColors1: Array<FloatArray> = emptyArray(),
    var gradientColors2: Array<FloatArray> = emptyArray(),
    var gradientColors3: Array<FloatArray> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as BgEffectData
        return uPoints.contentEquals(other.uPoints)
    }

    override fun hashCode(): Int {
        return uPoints.contentHashCode()
    }
}

class BgEffectDataManager {
    val dataPhoneLight: BgEffectData
    val dataPadLight: BgEffectData
    val dataPhoneDark: BgEffectData
    val dataPadDark: BgEffectData

    init {
        dataPhoneLight = BgEffectData().apply {
            uTranslateY = 0.0f
            uPoints = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f)
            uAlphaMulti = 1.0f
            uNoiseScale = 1.5f
            uPointOffset = 0.2f
            uPointRadiusMulti = 1.0f
            uSaturateOffset = 0.2f
            uLightOffset = 0.1f
            uAlphaOffset = 0.5f
            uShadowColorMulti = 0.3f
            uShadowColorOffset = 0.3f
            uShadowNoiseScale = 5.0f
            uShadowOffset = 0.01f
            colorInterpPeriod = 10.0f
            gradientSpeedChange = 1.6f
            gradientSpeedRest = 1.05f

            gradientColors1 = arrayOf(
                floatArrayOf(1.0f, 0.9f, 0.94f, 1.0f),
                floatArrayOf(1.0f, 0.84f, 0.89f, 1.0f),
                floatArrayOf(0.97f, 0.73f, 0.82f, 1.0f),
                floatArrayOf(0.64f, 0.65f, 0.98f, 1.0f)
            )

            gradientColors2 = arrayOf(
                floatArrayOf(0.58f, 0.74f, 1.0f, 1.0f),
                floatArrayOf(1.0f, 0.9f, 0.93f, 1.0f),
                floatArrayOf(0.74f, 0.76f, 1.0f, 1.0f),
                floatArrayOf(0.97f, 0.77f, 0.84f, 1.0f)
            )

            gradientColors3 = arrayOf(
                floatArrayOf(0.98f, 0.86f, 0.9f, 1.0f),
                floatArrayOf(0.6f, 0.73f, 0.98f, 1.0f),
                floatArrayOf(0.92f, 0.93f, 1.0f, 1.0f),
                floatArrayOf(0.56f, 0.69f, 1.0f, 1.0f)
            )
        }

        dataPadLight = BgEffectData().apply {
            uTranslateY = 0.0f
            uPoints = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f)
            uAlphaMulti = 1.0f
            uNoiseScale = 1.5f
            uPointOffset = 0.2f
            uPointRadiusMulti = 1.0f
            uSaturateOffset = 0.2f
            uLightOffset = 0.1f
            uAlphaOffset = 0.5f
            uShadowColorMulti = 0.3f
            uShadowColorOffset = 0.3f
            uShadowNoiseScale = 5.0f
            uShadowOffset = 0.01f
            colorInterpPeriod = 10.0f
            gradientSpeedChange = 1.8f
            gradientSpeedRest = 1.0f

            gradientColors1 = arrayOf(
                floatArrayOf(0.99f, 0.77f, 0.86f, 1.0f),
                floatArrayOf(0.74f, 0.76f, 1.0f, 1.0f),
                floatArrayOf(0.72f, 0.74f, 1.0f, 1.0f),
                floatArrayOf(0.98f, 0.76f, 0.8f, 1.0f)
            )

            gradientColors2 = arrayOf(
                floatArrayOf(0.66f, 0.75f, 1.0f, 1.0f),
                floatArrayOf(1.0f, 0.86f, 0.91f, 1.0f),
                floatArrayOf(0.74f, 0.76f, 1.0f, 1.0f),
                floatArrayOf(0.97f, 0.77f, 0.84f, 1.0f)
            )

            gradientColors3 = arrayOf(
                floatArrayOf(0.97f, 0.79f, 0.85f, 1.0f),
                floatArrayOf(0.65f, 0.68f, 0.98f, 1.0f),
                floatArrayOf(0.66f, 0.77f, 1.0f, 1.0f),
                floatArrayOf(0.72f, 0.73f, 0.98f, 1.0f)
            )
        }

        dataPhoneDark = BgEffectData().apply {
            uTranslateY = 0.0f
            uPoints = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f)
            uAlphaMulti = 1.0f
            uNoiseScale = 1.5f
            uPointOffset = 0.4f
            uPointRadiusMulti = 1.0f
            uSaturateOffset = 0.17f
            uLightOffset = 0.0f
            uAlphaOffset = 0.5f
            uShadowColorMulti = 0.3f
            uShadowColorOffset = 0.3f
            uShadowNoiseScale = 5.0f
            uShadowOffset = 0.01f
            colorInterpPeriod = 10.0f
            gradientSpeedChange = 1.0f
            gradientSpeedRest = 1.0f

            gradientColors1 = arrayOf(
                floatArrayOf(0.2f, 0.06f, 0.88f, 0.4f),
                floatArrayOf(0.3f, 0.14f, 0.55f, 0.5f),
                floatArrayOf(0.0f, 0.64f, 0.96f, 0.5f),
                floatArrayOf(0.11f, 0.16f, 0.83f, 0.4f)
            )

            gradientColors2 = arrayOf(
                floatArrayOf(0.07f, 0.15f, 0.79f, 0.5f),
                floatArrayOf(0.62f, 0.21f, 0.67f, 0.5f),
                floatArrayOf(0.06f, 0.25f, 0.84f, 0.5f),
                floatArrayOf(0.0f, 0.2f, 0.78f, 0.5f)
            )

            gradientColors3 = arrayOf(
                floatArrayOf(0.58f, 0.3f, 0.74f, 0.4f),
                floatArrayOf(0.27f, 0.18f, 0.6f, 0.5f),
                floatArrayOf(0.66f, 0.26f, 0.62f, 0.5f),
                floatArrayOf(0.12f, 0.16f, 0.7f, 0.6f)
            )
        }

        dataPadDark = BgEffectData().apply {
            uTranslateY = 0.0f
            uPoints = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f)
            uAlphaMulti = 1.0f
            uNoiseScale = 1.5f
            uPointOffset = 0.2f
            uPointRadiusMulti = 1.0f
            uSaturateOffset = 0.0f
            uLightOffset = 0.0f
            uAlphaOffset = 0.5f
            uShadowColorMulti = 0.3f
            uShadowColorOffset = 0.3f
            uShadowNoiseScale = 5.0f
            uShadowOffset = 0.01f
            colorInterpPeriod = 10.0f
            gradientSpeedChange = 1.6f
            gradientSpeedRest = 1.2f

            gradientColors1 = arrayOf(
                floatArrayOf(0.66f, 0.26f, 0.62f, 0.4f),
                floatArrayOf(0.06f, 0.25f, 0.84f, 0.5f),
                floatArrayOf(0.0f, 0.64f, 0.96f, 0.5f),
                floatArrayOf(0.14f, 0.18f, 0.55f, 0.5f)
            )

            gradientColors2 = arrayOf(
                floatArrayOf(0.07f, 0.15f, 0.79f, 0.5f),
                floatArrayOf(0.11f, 0.16f, 0.83f, 0.5f),
                floatArrayOf(0.06f, 0.25f, 0.84f, 0.5f),
                floatArrayOf(0.66f, 0.26f, 0.62f, 0.5f)
            )

            gradientColors3 = arrayOf(
                floatArrayOf(0.58f, 0.3f, 0.74f, 0.5f),
                floatArrayOf(0.11f, 0.16f, 0.83f, 0.5f),
                floatArrayOf(0.66f, 0.26f, 0.62f, 0.5f),
                floatArrayOf(0.27f, 0.18f, 0.6f, 0.6f)
            )
        }
    }

    fun getData(deviceType: DeviceType, themeMode: ThemeMode): BgEffectData {
        return when (deviceType) {
            DeviceType.PHONE -> if (themeMode == ThemeMode.LIGHT) dataPhoneLight else dataPhoneDark
            DeviceType.TABLET -> if (themeMode == ThemeMode.LIGHT) dataPadLight else dataPadDark
        }
    }
}

fun FloatArray.toComposeColor(): Color {
    return Color(this[0], this[1], this[2], this[3])
}