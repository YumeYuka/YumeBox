package plus.yumeyuka.yumeyuka

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun HomeScreen() {
    var deviceModel by remember { mutableStateOf("Loading...") }
    var socInfo by remember { mutableStateOf("Loading...") }
    var androidVersion by remember { mutableStateOf("Loading...") }
    var systemVersion by remember { mutableStateOf("Loading...") }
    var daysToNextVersion by remember { mutableStateOf("Calculating...") }
    var genshinVersion by remember { mutableStateOf("Loading...") }
    
    val isDarkTheme = isSystemInDarkTheme()
    val colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    
    val themeMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    val deviceType = DeviceType.PHONE
    
    val controller = remember { GradientController(deviceType, themeMode) }
    
    LaunchedEffect(themeMode) {
        controller.setTheme(deviceType, themeMode)
    }
    
    LaunchedEffect(Unit) {
        try {
            val modelResult = KsuBridge.exec("getprop ro.product.model")
            if (modelResult.isSuccess) {
                deviceModel = modelResult.output.trim()
            }
            
            val socResult = KsuBridge.exec("getprop ro.board.platform")
            if (socResult.isSuccess && socResult.output.isNotBlank()) {
                socInfo = socResult.output.trim()
            } else {
                val cpuResult = KsuBridge.exec("cat /proc/cpuinfo | grep Hardware")
                if (cpuResult.isSuccess) {
                    socInfo = cpuResult.output.split(":").lastOrNull()?.trim() ?: "Unknown"
                }
            }
            
            val androidResult = KsuBridge.exec("getprop ro.build.version.release")
            if (androidResult.isSuccess) {
                androidVersion = androidResult.output.trim()
            }
            
            val systemResult = KsuBridge.exec("getprop ro.build.id")
            if (systemResult.isSuccess) {
                systemVersion = systemResult.output.trim()
            }
            
            val genshinVersionResult = KsuBridge.exec(
                "dumpsys package com.miHoYo.Yuanshen | grep versionName"
            )
            if (genshinVersionResult.isSuccess && genshinVersionResult.output.isNotBlank()) {
                val versionName = genshinVersionResult.output
                    .split("=")
                    .lastOrNull()
                    ?.trim()
                
                val genshinCodeResult = KsuBridge.exec(
                    "dumpsys package com.miHoYo.Yuanshen | grep versionCode"
                )
                val versionCode = if (genshinCodeResult.isSuccess) {
                    genshinCodeResult.output
                        .split("=")
                        .lastOrNull()
                        ?.split(" ")
                        ?.firstOrNull()
                        ?.trim()
                } else null
                
                    genshinVersion = if (versionName != null && versionCode != null) {
                        "$versionName ($versionCode)"
                    } else if (versionName != null) {
                        versionName
                    } else {
                        "Not Installed"
                    }
                } else {
                    genshinVersion = "Not Installed"
                }
            
            val version61ReleaseDate = kotlinx.datetime.LocalDate(2025, 10, 21)
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            
            val daysSinceRelease = (today.toEpochDays() - version61ReleaseDate.toEpochDays()).toInt()
            val daysInCycle = daysSinceRelease % 45
            val daysRemaining = 45 - daysInCycle
            
            val currentVersionNumber = 6.1 + (daysSinceRelease / 45) * 0.1
            val nextVersionNumber = currentVersionNumber + 0.1
            
            val formattedVersion = js("nextVersionNumber.toFixed(1)") as String
            daysToNextVersion = "$daysRemaining days until v$formattedVersion"
            
        } catch (e: Exception) {
            console.error("Failed to load device info: ", e)
            deviceModel = "Failed"
            socInfo = "Failed"
            androidVersion = "Failed"
            systemVersion = "Failed"
            daysToNextVersion = "Failed"
            genshinVersion = "Failed"
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            KsuBridge.setFullScreen(true)
        } catch (e: Exception) {
        }
    }
    
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                controller.updateFrame(0.016f)
            }
        }
    }
    
    var isFullScreen by remember { mutableStateOf(false) }
    
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        logoVisible = true
    }
    
    val logoEnterAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logoEnterAlpha"
    )
    
    val logoEnterScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoEnterScale"
    )
    
    val currentColors by rememberUpdatedState(controller.currentColors)
    
    val textColors by remember {
        derivedStateOf {
            currentColors.map { color ->
                val brightness = (color.red + color.green + color.blue) / 3f
                
                if (brightness > 0.85f) {
                    color.deepen(
                        saturationBoost = 0.4f,
                        darknessFactor = 0.5f
                    )
                } else {
                    color.deepen(
                        saturationBoost = 0.3f,
                        darknessFactor = 0.65f
                    )
                }
            }
        }
    }
    
    MiuixTheme(colors = colors) {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedGradientBackground(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(200.dp))
                    
                    Column(
                        modifier = Modifier
                            .alpha(logoEnterAlpha)
                            .scale(logoEnterScale),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedGradientVectorLogo(
                            controller = controller,
                            size = 150f,
                            gradientMode = GradientMode.LINEAR_ROTATING,
                            customColors = textColors
                        )
                        
                        Text(
                            text = "Genshin Impact",
                            style = MiuixTheme.textStyles.headline1.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                brush = Brush.linearGradient(
                                    colors = textColors
                                )
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .alpha(controller.getVersionAlpha() * logoEnterAlpha)
                            .scale(controller.getLogoScale() * logoEnterScale),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = genshinVersion,
                            style = MiuixTheme.textStyles.body1.copy(
                                fontSize = 16.sp
                            ),
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        BasicComponent(
                            title = deviceModel,
                        )
                        BasicComponent(
                            title = socInfo,
                            summary = "Soc"
                        )
                        BasicComponent(
                            title = daysToNextVersion,
                            summary = "Next Genshin Update"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        text = "Start Genshin Impact",
                        onClick = {
                            kotlinx.coroutines.GlobalScope.launch {
                                try {
                                    val result = KsuBridge.exec(
                                        "am start -n com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity"
                                    )
                                    
                                    if (result.isSuccess) {
                                        console.log("✅ 原神启动成功")
                                        KsuBridge.toast("Launching Genshin Impact...")
                                    } else {
                                        console.error("❌ 原神启动失败: ${result.error}")
                                        KsuBridge.toast("Launch failed: ${result.error}")
                                    }
                                } catch (e: Exception) {
                                    console.error("❌ 启动异常: ", e)
                                    KsuBridge.toast("Launch exception: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}