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

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import plus.yumeyuka.yumebox.core.model.LogMessage
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.repository.NetworkInfoService
import plus.yumeyuka.yumebox.data.repository.IpMonitoringState
import plus.yumeyuka.yumebox.data.repository.ProxyChainResolver
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.domain.facade.ProxyFacade
import plus.yumeyuka.yumebox.domain.facade.ProfilesRepository
import plus.yumeyuka.yumebox.clash.loader.ConfigAutoLoader
import dev.oom_wg.purejoy.mlang.MLang

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val appSettingsStorage: AppSettingsStorage,
    private val configAutoLoadService: ConfigAutoLoader,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver
) : AndroidViewModel(application) {

    val profiles: StateFlow<List<Profile>> = profilesRepository.profiles
    val recommendedProfile: StateFlow<Profile?> = profilesRepository.recommendedProfile
    val hasEnabledProfile: Flow<Boolean> = profiles.map { it.any { profile -> profile.enabled } }

    val proxyState = proxyFacade.proxyState
    val isRunning = proxyFacade.isRunning
    val runningMode = proxyFacade.runningMode
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val trafficTotal = proxyFacade.trafficTotal
    val proxyGroups = proxyFacade.proxyGroups
    val tunnelState = proxyFacade.tunnelState

    val oneWord: StateFlow<String> = appSettingsStorage.oneWord.state
    val oneWordAuthor: StateFlow<String> = appSettingsStorage.oneWordAuthor.state
    val oneWordAndAuthor: StateFlow<String> =
        combine(appSettingsStorage.oneWord.state, appSettingsStorage.oneWordAuthor.state) { word, author ->
            "\"$word\" — $author"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    private val _speedHistory = MutableStateFlow<List<Long>>(emptyList())
    val speedHistory: StateFlow<List<Long>> = _speedHistory.asStateFlow()

    private val mainProxyNode: StateFlow<plus.yumeyuka.yumebox.core.model.Proxy?> = 
        combine(isRunning, proxyGroups) { running, groups ->
            if (!running || groups.isEmpty()) return@combine null
            val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
            if (mainGroup != null && mainGroup.now.isNotBlank()) {
                proxyChainResolver.resolveEndNode(mainGroup.now, groups)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val selectedServerName: StateFlow<String?> = mainProxyNode.map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.let {
            proxyFacade.getCachedDelay(it.name)?.takeIf { d -> d > 0 } ?: it.delay.takeIf { d -> d > 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ipMonitoringState: StateFlow<IpMonitoringState> =
        networkInfoService.startIpMonitoring(isRunning)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    private val _recentLogs = MutableStateFlow<List<LogMessage>>(emptyList())
    val recentLogs: StateFlow<List<LogMessage>> = _recentLogs.asStateFlow()

    init {
        subscribeToLogs()
        startSpeedSampling()
    }

    suspend fun reloadProfile(profileId: String) {
        try {
            setLoading(true)
            val result = configAutoLoadService.reloadConfig(profileId)
            if (result.isSuccess) {
                showMessage(MLang.Home.Message.ConfigSwitched)
            } else {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(result.exceptionOrNull()?.message))
            }
        } catch (e: Exception) {
            showError(MLang.Home.Message.ConfigSwitchFailed.format(e.message))
        } finally {
            setLoading(false)
        }
    }

    fun startProxy(profileId: String, useTunMode: Boolean? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isStartingProxy = true, loadingProgress = MLang.Home.Message.Preparing) }

                val result = proxyFacade.startProxy(profileId, useTunMode)

                result.fold(
                    onSuccess = { intent ->
                        if (intent != null) {
                            _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                            _vpnPrepareIntent.emit(intent)
                        } else {
                            _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                        showError(MLang.Home.Message.StartFailed.format(error.message))
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                showError(MLang.Home.Message.StartFailed.format(e.message))
            }
        }
    }

    fun stopProxy() {
        viewModelScope.launch {
            try {
                setLoading(true)
                proxyFacade.stopProxy()
                showMessage(MLang.Home.Message.ProxyStopped)
            } catch (e: Exception) {
                showError(MLang.Home.Message.StopFailed.format(e.message))
            } finally {
                setLoading(false)
            }
        }
    }

    fun refreshIpInfo() = networkInfoService.triggerRefresh()

    fun clearLogs() { _recentLogs.value = emptyList() }

    private fun subscribeToLogs() {
        viewModelScope.launch {
            proxyFacade.logs.collect { log ->
                _recentLogs.update { currentLogs ->
                    buildList {
                        add(log)
                        addAll(currentLogs.take(49))
                    }
                }
            }
        }
    }

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(proxyFacade.trafficNow.value.download.coerceAtLeast(0L))
                    kotlinx.coroutines.delay(1000L)
                }
            }.catch { }.collect { sample ->
                _speedHistory.update { old ->
                    buildList(sampleLimit) {
                        repeat((sampleLimit - old.size - 1).coerceAtLeast(0)) { add(0L) }
                        addAll(old.takeLast(sampleLimit - 1))
                        add(sample)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}