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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import plus.yumeyuka.yumebox.core.model.Provider
import plus.yumeyuka.yumebox.presentation.component.*
import plus.yumeyuka.yumebox.presentation.component.Card
import plus.yumeyuka.yumebox.presentation.viewmodel.ProvidersViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.*
import dev.oom_wg.purejoy.mlang.MLang

@Composable
@Destination<RootGraph>
fun ProvidersScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<ProvidersViewModel>()
    val scrollBehavior = MiuixScrollBehavior()

    val providers by viewModel.providers.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            viewModel.refreshProviders()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Providers.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                },
                actions = {
                    if (isRunning && providers.any { it.vehicleType == Provider.VehicleType.HTTP }) {
                        RotatingRefreshButton(
                            isRotating = uiState.isUpdatingAll,
                            onClick = { viewModel.updateAllProviders() },
                            modifier = Modifier.padding(end = 24.dp),
                            contentDescription = MLang.Providers.Action.UpdateAll
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (!isRunning) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NotRunning,
                secondLine = MLang.Providers.Empty.NotRunningHint
            )
        } else if (providers.isEmpty() && !uiState.isLoading) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NoProviders,
                secondLine = MLang.Providers.Empty.NoProvidersHint
            )
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = innerPadding,
            ) {
                val proxyProviders = providers.filter { it.type == Provider.Type.Proxy }
                val ruleProviders = providers.filter { it.type == Provider.Type.Rule }

                if (proxyProviders.isNotEmpty()) {
                    item {
                        SmallTitle(MLang.Providers.Type.ProxyProviders.format(proxyProviders.size))
                    }
                    proxyProviders.forEach { provider ->
                        item(key = "proxy_${provider.name}") {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(provider.name),
                                onUpdate = { viewModel.updateProvider(provider) }
                            )
                        }
                    }
                }

                if (ruleProviders.isNotEmpty()) {
                    item {
                        SmallTitle(MLang.Providers.Type.RuleProviders.format(ruleProviders.size))
                    }
                    ruleProviders.forEach { provider ->
                        item(key = "rule_${provider.name}") {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(provider.name),
                                onUpdate = { viewModel.updateProvider(provider) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    isUpdating: Boolean,
    onUpdate: () -> Unit
) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.vehicleType.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (provider.updatedAt > 0) {
                        Text(
                            text = "•",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = formatTimestamp(provider.updatedAt),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (provider.vehicleType == Provider.VehicleType.HTTP) {
                RotatingRefreshButton(
                    isRotating = isUpdating,
                    onClick = onUpdate
                )
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
