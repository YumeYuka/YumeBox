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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import plus.yumeyuka.yumebox.presentation.component.Card
import plus.yumeyuka.yumebox.presentation.component.ScreenLazyColumn
import plus.yumeyuka.yumebox.presentation.component.SmallTitle
import plus.yumeyuka.yumebox.presentation.component.TopBar
import plus.yumeyuka.yumebox.presentation.icon.Yume
import plus.yumeyuka.yumebox.presentation.icon.yume.`Settings-2`
import plus.yumeyuka.yumebox.presentation.viewmodel.AccessControlViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

@Composable
@Destination<RootGraph>
fun AccessControlScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val showSettingsSheet = rememberSaveable { mutableStateOf(false) }
    val searchExpanded = rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = searchExpanded.value) {
        searchExpanded.value = false
        viewModel.onSearchQueryChange("")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = MLang.AccessControl.Title,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            modifier = Modifier.padding(end = 24.dp),
                            onClick = { showSettingsSheet.value = true }
                        ) {
                            Icon(Yume.`Settings-2`, contentDescription = MLang.AccessControl.Settings.Title)
                        }
                    }
                )
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(MLang.AccessControl.AppList.Loading, color = MiuixTheme.colorScheme.onSurface)
                }
            } else {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    topPadding = 20.dp
                ) {
                    item {
                        var searchText by remember { mutableStateOf("") }
                        var expanded by remember { mutableStateOf(false) }
                        SearchBar(
                            inputField = {
                                InputField(
                                    query = searchText,
                                    onQueryChange = {
                                        searchText = it
                                        viewModel.onSearchQueryChange(it)
                                    },
                                    onSearch = { expanded = false },
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it },
                                    label = MLang.AccessControl.Search.Placeholder
                                )
                            },
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                        }
                    }

                    item {
                        SmallTitle(MLang.AccessControl.AppList.Title.format(uiState.selectedPackages.size))
                    }

                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppCard(
                            app = app,
                            onSelectionChange = { checked ->
                                viewModel.onAppSelectionChange(app.packageName, checked)
                            },
                            onClick = {
                                viewModel.onAppSelectionChange(app.packageName, !app.isSelected)
                            }
                        )
                    }
                }
            }

            SuperBottomSheet(
                show = showSettingsSheet,
                title = MLang.AccessControl.Settings.Title,
                onDismissRequest = { showSettingsSheet.value = false },
                insideMargin = DpSize(32.dp, 16.dp),
            ) {
                Column {
                    top.yukonga.miuix.kmp.basic.Card {
                        SuperSwitch(
                            title = MLang.AccessControl.Settings.ShowSystemApps,
                            checked = uiState.showSystemApps,
                            onCheckedChange = { viewModel.onShowSystemAppsChange(it) }
                        )
                        SuperSwitch(
                            title = MLang.AccessControl.Settings.DescendingOrder,
                            checked = uiState.descending,
                            onCheckedChange = { viewModel.onDescendingChange(it) }
                        )
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    top.yukonga.miuix.kmp.basic.Card {
                        SuperDropdown(
                            title = MLang.AccessControl.Settings.SortMode,
                            summary = MLang.AccessControl.Settings.SortModeCurrent.format(uiState.sortMode.displayName),
                            items = AccessControlViewModel.SortMode.values().map { it.displayName },
                            selectedIndex = AccessControlViewModel.SortMode.values()
                                .indexOf(uiState.sortMode)
                                .coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                AccessControlViewModel.SortMode.entries.getOrNull(index)
                                    ?.let { viewModel.onSortModeChange(it) }
                            }
                        )
                        SuperDropdown(
                            title = MLang.AccessControl.Settings.BatchOperation,
                            items = listOf(MLang.AccessControl.Settings.SelectAll, MLang.AccessControl.Settings.DeselectAll, MLang.AccessControl.Settings.Invert),
                            selectedIndex = 0,
                            onSelectedIndexChange = { index ->
                                when (index) {
                                    0 -> viewModel.selectAll()
                                    1 -> viewModel.deselectAll()
                                    2 -> viewModel.invertSelection()
                                }
                            }
                        )
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(MLang.AccessControl.Button.Cancel)
                    }
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(MLang.AccessControl.Button.Confirm, color = MiuixTheme.colorScheme.background)
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = searchExpanded.value,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        ExpandedSearchOverlay(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            filteredApps = uiState.filteredApps,
            onAppSelectionChange = { packageName, checked ->
                viewModel.onAppSelectionChange(packageName, checked)
            },
            onDismiss = {
                searchExpanded.value = false
                viewModel.onSearchQueryChange("")
            }
        )
    }
}

@Composable
private fun ExpandedSearchOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredApps: List<AccessControlViewModel.AppInfo>,
    onAppSelectionChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = MLang.AccessControl.Search.Placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    BasicComponent(
                        title = app.label,
                        summary = app.packageName,
                        leftAction = {
                            app.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap(width = 80, height = 80).asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        rightActions = {
                            Checkbox(
                                checked = app.isSelected,
                                onCheckedChange = { checked ->
                                    onAppSelectionChange(app.packageName, checked)
                                }
                            )
                        },
                        onClick = {
                            onAppSelectionChange(app.packageName, !app.isSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AccessControlViewModel.AppInfo,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        BasicComponent(
            title = app.label,
            summary = app.packageName,
            leftAction = {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap(width = 80, height = 80).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(45.dp)
                            .padding(end = 12.dp)
                    )
                }
            },
            rightActions = {
                Checkbox(
                    checked = app.isSelected,
                    onCheckedChange = onSelectionChange
                )
            },
            onClick = onClick
        )
    }
}
