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

package plus.yumeyuka.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.core.Clash
import plus.yumeyuka.yumebox.core.model.Proxy
import plus.yumeyuka.yumebox.core.model.TunnelState
import plus.yumeyuka.yumebox.data.store.ProxyDisplaySettingsStore
import plus.yumeyuka.yumebox.domain.model.ProxyDisplayMode
import plus.yumeyuka.yumebox.domain.model.ProxyGroupInfo
import plus.yumeyuka.yumebox.domain.model.ProxySortMode
import dev.oom_wg.purejoy.mlang.MLang

class ProxyViewModel(
    private val clashManager: ClashManager,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _currentMode = MutableStateFlow(TunnelState.Mode.Rule)
    val currentMode: StateFlow<TunnelState.Mode> = _currentMode.asStateFlow()

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.DOUBLE_SIMPLE)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsStore.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)


    private val _selectedGroupIndex = MutableStateFlow(0)
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()


    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyGroups


    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode) { groups, sortMode ->
            groups.map { group ->
                group.copy(
                    proxies = sortProxies(group.proxies, sortMode)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testRequested = MutableStateFlow(false)

    init { loadCurrentMode() }

    private fun loadCurrentMode() {
        viewModelScope.launch {
            runCatching {
                val override = Clash.queryOverride(Clash.OverrideSlot.Session)
                _currentMode.value = override.mode ?: TunnelState.Mode.Rule
            }
        }
    }

    fun patchMode(mode: TunnelState.Mode) {
        viewModelScope.launch {
            runCatching {
                val override = Clash.queryOverride(Clash.OverrideSlot.Session)
                override.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Session, override)
                clashManager.reloadCurrentProfile()
                kotlinx.coroutines.delay(100)
                _currentMode.value = mode
                val modeName = when (mode) {
                    TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
                    TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
                    TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
                    else -> MLang.Proxy.Mode.Unknown
                }
                showMessage(MLang.Proxy.Mode.Switched.format(modeName))
                clashManager.refreshProxyGroups()
            }.onFailure { e ->
                showError(MLang.Proxy.Mode.SwitchFailed.format(e.message))
            }
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                _testRequested.value = true
                setLoading(true)
                clearError()
                if (groupName != null) {
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    clashManager.healthCheck(groupName)
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    clashManager.healthCheckAll()
                }
                showMessage(MLang.Proxy.Testing.RequestSent)
                kotlinx.coroutines.delay(3000)
                setLoading(false)
                _testRequested.value = false
            } catch (e: Exception) {
                showError(MLang.Proxy.Testing.Failed.format(e.message))
                _testRequested.value = false
                setLoading(false)
            }
        }
    }

    fun refreshProxyGroups() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = clashManager.refreshProxyGroups()
                if (result.isSuccess) {
                    showMessage(MLang.Proxy.Refresh.Success)
                } else {
                    showError(MLang.Proxy.Refresh.Failed.format(result.exceptionOrNull()?.message))
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Refresh.Failed.format(e.message))
            } finally {
                setLoading(false)
            }
        }
    }

    fun setSelectedGroup(index: Int) {
        val groups = proxyGroups.value
        _selectedGroupIndex.value = index.coerceIn(0, groups.size - 1)
    }

    fun toggleDisplayMode() {
        val current = displayMode.value
        val newMode = when (current) {
            ProxyDisplayMode.SINGLE_DETAILED -> ProxyDisplayMode.SINGLE_SIMPLE
            ProxyDisplayMode.SINGLE_SIMPLE -> ProxyDisplayMode.DOUBLE_DETAILED
            ProxyDisplayMode.DOUBLE_DETAILED -> ProxyDisplayMode.DOUBLE_SIMPLE
            ProxyDisplayMode.DOUBLE_SIMPLE -> ProxyDisplayMode.SINGLE_DETAILED
        }
        setDisplayMode(newMode)
    }

    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsStore.displayMode.set(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    fun toggleSortMode() {
        val current = sortMode.value
        val newMode = when (current) {
            ProxySortMode.DEFAULT -> ProxySortMode.BY_NAME
            ProxySortMode.BY_NAME -> ProxySortMode.BY_LATENCY
            ProxySortMode.BY_LATENCY -> ProxySortMode.DEFAULT
        }
        setSortMode(newMode)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val success = clashManager.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Selection.Error.format(e.message))
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> proxies.sortedWith(compareBy { if (it.delay > 0) it.delay else Int.MAX_VALUE })
    }

    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}