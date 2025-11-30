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

package plus.yumeyuka.yumebox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import plus.yumeyuka.yumebox.presentation.theme.NavigationTransitions
import plus.yumeyuka.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import plus.yumeyuka.yumebox.presentation.theme.YumeTheme
import plus.yumeyuka.yumebox.presentation.component.BottomBar
import plus.yumeyuka.yumebox.presentation.component.LocalHandlePageChange
import plus.yumeyuka.yumebox.presentation.component.LocalNavigator
import plus.yumeyuka.yumebox.presentation.component.LocalPagerState
import plus.yumeyuka.yumebox.presentation.screen.HomePager
import plus.yumeyuka.yumebox.presentation.screen.ProfilesPager
import plus.yumeyuka.yumebox.presentation.screen.ProxyPager
import plus.yumeyuka.yumebox.presentation.screen.SettingPager
import plus.yumeyuka.yumebox.presentation.viewmodel.AppSettingsViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()
        fun clearPendingImportUrl() { _pendingImportUrl.value = null }
    }

    private val appSettingsStorage: plus.yumeyuka.yumebox.data.store.AppSettingsStorage by inject()
    private val networkSettingsStorage: plus.yumeyuka.yumebox.data.store.NetworkSettingsStorage by inject()
    private val profilesStore: plus.yumeyuka.yumebox.data.store.ProfilesStore by inject()
    private val clashManager: plus.yumeyuka.yumebox.clash.manager.ClashManager by inject()
    private val proxyConnectionService: plus.yumeyuka.yumebox.data.repository.ProxyConnectionService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION,
                )
            }
        }

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    colorTheme = colorTheme,
                ) {
                    val navController = rememberNavController()

                    Surface(
                        modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.surface
                    ) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                            defaultTransitions = NavigationTransitions.defaultStyle,
                        )
                    }
                }
            }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(plus.yumeyuka.yumebox.common.AppConstants.Timing.AUTO_START_DELAY_MS)
                plus.yumeyuka.yumebox.common.util.ProxyAutoStartHelper.checkAndAutoStart(
                    proxyConnectionService = proxyConnectionService,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    profilesStore = profilesStore,
                    clashManager = clashManager,
                    isBootCompleted = false
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            val scheme = uri.scheme
            if (scheme == "clash" || scheme == "clashmeta") {
                val host = uri.host
                if (host == "install-config") {
                    val configUrl = uri.getQueryParameter("url")
                    if (!configUrl.isNullOrBlank()) {
                        _pendingImportUrl.value = configUrl
                    }
                }
            }
        }
    }
}

@Composable
@Destination<RootGraph>(start = true)
fun MainScreen(navigator: DestinationsNavigator) {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.8f)),
    )

    val handlePageChange: (Int) -> Unit = remember(pagerState, coroutineScope) {
        { page ->
            coroutineScope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            activity?.finish()
        }
    }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalNavigator provides navigator,
    ) {
        Scaffold(
            bottomBar = {
                BottomBar(hazeState, hazeStyle)
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier.hazeSource(state = hazeState),
                state = pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = true,
            ) { page ->
                when (page) {
                    0 -> HomePager(innerPadding)
                    1 -> ProxyPager(innerPadding, navigator)
                    2 -> ProfilesPager(innerPadding)
                    3 -> SettingPager(innerPadding)
                }
            }
        }
    }
}