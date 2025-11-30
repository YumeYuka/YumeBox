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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import plus.yumeyuka.yumebox.common.util.WebViewUtils.getLocalBaseUrl
import plus.yumeyuka.yumebox.common.util.WebViewUtils.getPanelUrl
import plus.yumeyuka.yumebox.core.model.Proxy
import plus.yumeyuka.yumebox.core.model.TunnelState
import plus.yumeyuka.yumebox.domain.model.ProxyDisplayMode
import plus.yumeyuka.yumebox.domain.model.ProxySortMode
import plus.yumeyuka.yumebox.presentation.component.CenteredText
import plus.yumeyuka.yumebox.presentation.component.ProxyGroupTabs
import plus.yumeyuka.yumebox.presentation.component.ProxyNodeCard
import plus.yumeyuka.yumebox.presentation.component.TopBar
import plus.yumeyuka.yumebox.presentation.icon.Yume
import plus.yumeyuka.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Squares-exclude`
import plus.yumeyuka.yumebox.presentation.icon.yume.Zap
import plus.yumeyuka.yumebox.presentation.icon.yume.Zashboard
import plus.yumeyuka.yumebox.presentation.viewmodel.FeatureViewModel
import plus.yumeyuka.yumebox.presentation.viewmodel.HomeViewModel
import plus.yumeyuka.yumebox.presentation.viewmodel.ProxyViewModel
import plus.yumeyuka.yumebox.presentation.webview.WebViewActivity
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val featureViewModel = koinViewModel<FeatureViewModel>()

    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val selectedGroupIndex by proxyViewModel.selectedGroupIndex.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val uiState by proxyViewModel.uiState.collectAsState()
    val currentMode by proxyViewModel.currentMode.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val showBottomSheet = rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val pagerState = rememberPagerState(
        initialPage = selectedGroupIndex, pageCount = { proxyGroups.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRunning) {
        if (isRunning) proxyViewModel.refreshProxyGroups()
    }

    LaunchedEffect(proxyGroups.size) {
        if (proxyGroups.isNotEmpty() && selectedGroupIndex >= proxyGroups.size) {
            proxyViewModel.setSelectedGroup(0)
        }
    }

    LaunchedEffect(selectedGroupIndex) {
        if (proxyGroups.isNotEmpty() && pagerState.currentPage != selectedGroupIndex) {
            pagerState.animateScrollToPage(selectedGroupIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (proxyGroups.isNotEmpty() && pagerState.currentPage != selectedGroupIndex) {
            proxyViewModel.setSelectedGroup(pagerState.currentPage)
        }
    }

    LaunchedEffect(uiState.isLoading, isRefreshing) {
        if (!uiState.isLoading && isRefreshing) {
            delay(300)
            isRefreshing = false
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing && proxyGroups.isNotEmpty()) {
            val currentGroup = proxyGroups.getOrNull(selectedGroupIndex)
            if (currentGroup != null) {
                proxyViewModel.testDelay(currentGroup.name)
            } else {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Proxy.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(
                            modifier = Modifier.padding(start = 24.dp),
                            onClick = { navigator.navigate(ProvidersScreenDestination) { launchSingleTop = true } }
                        ) {
                            Icon(Yume.`Squares-exclude`, contentDescription = MLang.Proxy.Action.ExternalResources)
                        }

                        IconButton(
                            onClick = {
                                val panelUrl = getPanelUrl(context, selectedPanelType)
                                val webViewUrl = panelUrl.ifEmpty {
                                    val localUrl = getLocalBaseUrl(context)
                                    if (localUrl.isNotEmpty()) {
                                        localUrl + "index.html"
                                    } else {
                                        ""
                                    }
                                }
                                if (webViewUrl.isNotEmpty()) {
                                    WebViewActivity.start(context, webViewUrl)
                                }
                            }
                        ) {
                            Icon(Yume.Zashboard, contentDescription = MLang.Proxy.Action.Panel)
                        }
                    }
                },
                actions = {
                    val currentGroup = proxyGroups.getOrNull(selectedGroupIndex)

                    IconButton(
                        modifier = Modifier.padding(end = 16.dp), onClick = {
                            if (currentGroup != null) {
                                proxyViewModel.testDelay(currentGroup.name)
                            }
                        }) {
                        Icon(Yume.Zap, contentDescription = MLang.Proxy.Action.Test)
                    }

                    IconButton(
                        modifier = Modifier.padding(end = 24.dp), onClick = { showBottomSheet.value = true }) {
                        Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Action.Settings)
                    }
                })
        }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (proxyGroups.isEmpty()) {
                CenteredText(
                    firstLine = MLang.Proxy.Empty.NoNodes, secondLine = MLang.Proxy.Empty.Hint
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = innerPadding.calculateTopPadding() + 4.dp)
                    ) {
                        ProxyGroupTabs(
                            groups = proxyGroups, selectedIndex = selectedGroupIndex, onTabSelected = { index ->
                                proxyViewModel.setSelectedGroup(index)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            })
                    }

                    PullToRefresh(
                        isRefreshing = isRefreshing,
                        onRefresh = { isRefreshing = true },
                        pullToRefreshState = pullToRefreshState,
                        refreshTexts = listOf(
                            MLang.Proxy.PullToRefresh.PullToTest,
                            MLang.Proxy.PullToRefresh.ReleaseToTest,
                            MLang.Proxy.Refresh.Refreshing,
                            MLang.Proxy.PullToRefresh.DelaySuccess
                        )
                    ) {
                        HorizontalPager(
                            state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1
                        ) { page ->
                            val currentGroup = proxyGroups.getOrNull(page)

                            if (currentGroup != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scrollEndHaptic()
                                        .overScrollVertical()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = mainInnerPadding.calculateBottomPadding()
                                    ),
                                    overscrollEffect = null
                                ) {
                                    if (currentGroup.chainPath.isNotEmpty()) {
                                        item {
                                            ProxyChainIndicator(
                                                chain = currentGroup.chainPath, modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    proxyNodeGridItems(
                                        proxies = currentGroup.proxies,
                                        selectedProxyName = currentGroup.now,
                                        displayMode = displayMode,
                                        isSelectable = currentGroup.type == Proxy.Type.Selector,
                                        onProxyClick = { proxy ->
                                            proxyViewModel.selectProxy(currentGroup.name, proxy.name)
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }


        SuperBottomSheet(
            show = showBottomSheet,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
        ) {
            Column {
                Text(
                    text = MLang.Proxy.Settings.ProxyMode,
                    style = MiuixTheme.textStyles.subtitle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val modeTabs = listOf(MLang.Proxy.Mode.Rule, MLang.Proxy.Mode.Global, MLang.Proxy.Mode.Direct)
                val modeValues = listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct)
                var selectedModeIndex by remember(currentMode) {
                    mutableIntStateOf(modeValues.indexOf(currentMode).coerceAtLeast(0))
                }
                TabRowWithContour(
                    tabs = modeTabs, selectedTabIndex = selectedModeIndex, onTabSelected = { index ->
                        selectedModeIndex = index
                        if (index < modeValues.size) {
                            proxyViewModel.patchMode(modeValues[index])
                        }
                    })

                Spacer(Modifier.padding(12.dp))

                Text(
                    text = MLang.Proxy.Settings.SortMode,
                    style = MiuixTheme.textStyles.subtitle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val sortTabs = ProxySortMode.entries.map { it.displayName }
                var selectedSortIndex by remember { mutableIntStateOf(sortMode.ordinal) }
                TabRowWithContour(
                    tabs = sortTabs, selectedTabIndex = selectedSortIndex, onTabSelected = { index ->
                        selectedSortIndex = index
                        if (index < ProxySortMode.entries.size) {
                            proxyViewModel.setSortMode(ProxySortMode.entries[index])
                        }
                    })

                Spacer(Modifier.padding(12.dp))

                Text(
                    text = MLang.Proxy.Settings.DisplayMode,
                    style = MiuixTheme.textStyles.subtitle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val displayTabs = ProxyDisplayMode.entries.map { it.displayName }
                var selectedDisplayIndex by remember { mutableIntStateOf(displayMode.ordinal) }
                TabRowWithContour(
                    tabs = displayTabs, selectedTabIndex = selectedDisplayIndex, onTabSelected = { index ->
                        selectedDisplayIndex = index
                        if (index < ProxyDisplayMode.entries.size) {
                            proxyViewModel.setDisplayMode(ProxyDisplayMode.entries[index])
                        }
                    })

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showBottomSheet.value = false }, modifier = Modifier.weight(1f)
                    ) {
                        Text(MLang.Component.Button.Cancel)
                    }

                    Button(
                        onClick = { showBottomSheet.value = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
                    }
                }

            }
        }
    }
}


fun LazyListScope.proxyNodeGridItems(
    proxies: List<Proxy>,
    selectedProxyName: String,
    displayMode: ProxyDisplayMode,
    isSelectable: Boolean = true,
    onProxyClick: (Proxy) -> Unit
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2

    val groupedProxies = proxies.chunked(columns)

    groupedProxies.forEach { rowProxies ->
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowProxies.forEach { proxy ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProxyNodeCard(
                            proxy = proxy,
                            isSelected = proxy.name == selectedProxyName,
                            onClick = if (isSelectable) {
                                { onProxyClick(proxy) }
                            } else null,
                            isSingleColumn = displayMode.isSingleColumn,
                            showDetail = displayMode.showDetail
                        )
                    }
                }

                repeat(columns - rowProxies.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProxyChainIndicator(
    chain: List<String>, modifier: Modifier = Modifier
) {
    top.yukonga.miuix.kmp.basic.Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chain.forEachIndexed { index, nodeName ->
                Text(
                    text = nodeName, style = MiuixTheme.textStyles.body2, color = if (index == chain.lastIndex) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                )

                if (index < chain.lastIndex) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}

