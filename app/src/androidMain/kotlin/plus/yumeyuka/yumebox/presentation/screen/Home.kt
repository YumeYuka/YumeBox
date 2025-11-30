/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import plus.yumeyuka.yumebox.common.AppConstants
import plus.yumeyuka.yumebox.presentation.component.LocalNavigator
import plus.yumeyuka.yumebox.presentation.component.ScreenLazyColumn
import plus.yumeyuka.yumebox.presentation.component.TopBar
import plus.yumeyuka.yumebox.presentation.component.combinePaddingValues
import plus.yumeyuka.yumebox.presentation.screen.home.HomeIdleContent
import plus.yumeyuka.yumebox.presentation.screen.home.HomeRunningContent
import plus.yumeyuka.yumebox.presentation.screen.home.ProxyControlButton
import plus.yumeyuka.yumebox.presentation.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class HomeDisplayState {
    Idle,
    Running
}

@Composable
fun HomePager(mainInnerPadding: PaddingValues) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current

    val isRunning by homeViewModel.isRunning.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val profiles by homeViewModel.profiles.collectAsState()
    val ipMonitoringState by homeViewModel.ipMonitoringState.collectAsState()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsState()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsState(initial = false)
    val tunnelState by homeViewModel.tunnelState.collectAsState()
    val currentProfile by homeViewModel.currentProfile.collectAsState()
    val oneWord by homeViewModel.oneWord.collectAsState()
    val oneWordAuthor by homeViewModel.oneWordAuthor.collectAsState()
    val selectedServerName by homeViewModel.selectedServerName.collectAsState()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsState()
    val speedHistory by homeViewModel.speedHistory.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val displayState by remember {
        derivedStateOf {
            if (isRunning || uiState.isStartingProxy) HomeDisplayState.Running else HomeDisplayState.Idle
        }
    }

    var pendingProfileId by remember { mutableStateOf<String?>(null) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingProfileId?.let { profileId ->
                homeViewModel.startProxy(profileId, useTunMode = true)
            }
        }
        pendingProfileId = null
    }

    LaunchedEffect(Unit) {
        homeViewModel.vpnPrepareIntent.collect { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = { TopBar(title = "YumeBox", scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
                topPadding = 15.dp
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING)
                            .padding(top = AppConstants.UI.DEFAULT_VERTICAL_SPACING),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(AppConstants.UI.DEFAULT_VERTICAL_SPACING)
                    ) {
                        AnimatedContent(
                            targetState = displayState,
                            transitionSpec = { createHomeTransitionSpec() },
                            label = "HomeContentTransition"
                        ) { state ->
                            when (state) {
                                HomeDisplayState.Idle -> HomeIdleContent(
                                    oneWord = oneWord,
                                    author = oneWordAuthor
                                )
                                HomeDisplayState.Running -> HomeRunningContent(
                                    trafficNow = trafficNow,
                                    profileName = currentProfile?.name,
                                    tunnelMode = tunnelState?.mode,
                                    serverName = selectedServerName,
                                    serverPing = selectedServerPing,
                                    ipMonitoringState = ipMonitoringState,
                                    speedHistory = speedHistory,
                                    onChartClick = {
                                        navigator.navigate(TrafficStatisticsScreenDestination) { launchSingleTop = true }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }

            ProxyControlButton(
                isRunning = isRunning || uiState.isStartingProxy,
                isEnabled = profiles.isNotEmpty() && hasEnabledProfile && !uiState.isStartingProxy,
                hasEnabledProfile = hasEnabledProfile,
                hasProfiles = profiles.isNotEmpty(),
                onClick = {
                    handleProxyToggle(
                        isRunning = isRunning,
                        recommendedProfile = recommendedProfile,
                        onStart = { profile ->
                            pendingProfileId = profile.id
                            coroutineScope.launch {
                                homeViewModel.startProxy(profileId = profile.id)
                            }
                        },
                        onStop = {
                            coroutineScope.launch {
                                homeViewModel.stopProxy()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING)
                    .padding(bottom = mainInnerPadding.calculateBottomPadding() + 32.dp)
                    .padding(top = AppConstants.UI.DEFAULT_VERTICAL_SPACING)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<HomeDisplayState>.createHomeTransitionSpec(): ContentTransform {
    val animDuration = 500
    return when {
        targetState == HomeDisplayState.Idle -> {
            (slideInVertically(animationSpec = tween(animDuration)) { -it } + 
             fadeIn(animationSpec = tween(animDuration)) + 
             scaleIn(initialScale = 0.9f, animationSpec = tween(animDuration))).togetherWith(
                slideOutVertically(animationSpec = tween(animDuration)) { it } + 
                fadeOut(animationSpec = tween(animDuration)) + 
                scaleOut(targetScale = 1.1f, animationSpec = tween(animDuration))
            )
        }
        else -> {
            (slideInVertically(animationSpec = tween(animDuration)) { it } + 
             fadeIn(animationSpec = tween(animDuration)) + 
             scaleIn(initialScale = 0.9f, animationSpec = tween(animDuration))).togetherWith(
                slideOutVertically(animationSpec = tween(animDuration)) { -it } + 
                fadeOut(animationSpec = tween(animDuration)) + 
                scaleOut(targetScale = 1.1f, animationSpec = tween(animDuration))
            )
        }
    }
}

private fun handleProxyToggle(
    isRunning: Boolean,
    recommendedProfile: plus.yumeyuka.yumebox.data.model.Profile?,
    onStart: (plus.yumeyuka.yumebox.data.model.Profile) -> Unit,
    onStop: () -> Unit
) {
    if (!isRunning) {
        recommendedProfile?.let { profile -> onStart(profile) }
    } else {
        onStop()
    }
}
