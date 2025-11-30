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

package plus.yumeyuka.yumebox.clash.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.domain.model.TrafficData
import plus.yumeyuka.yumebox.core.model.TunnelState
import plus.yumeyuka.yumebox.domain.model.RunningMode
import plus.yumeyuka.yumebox.domain.model.ProxyState

class ProxyStateManager(private val scope: CoroutineScope) {
    private val _proxyState = MutableStateFlow<ProxyState>(ProxyState.Idle)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    val isRunning: StateFlow<Boolean> = proxyState
        .map { it.isRunning }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val runningMode: StateFlow<RunningMode> = proxyState
        .map { state ->
            when (state) {
                is ProxyState.Running -> state.mode
                is ProxyState.Connecting -> state.mode
                else -> RunningMode.None
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, RunningMode.None)

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(TrafficData.ZERO)
    val trafficNow: StateFlow<TrafficData> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(TrafficData.ZERO)
    val trafficTotal: StateFlow<TrafficData> = _trafficTotal.asStateFlow()

    private val _tunnelState = MutableStateFlow<TunnelState?>(null)
    val tunnelState: StateFlow<TunnelState?> = _tunnelState.asStateFlow()

    fun transitionTo(newState: ProxyState) {
        _proxyState.value = newState
        if (newState is ProxyState.Running) {
            _currentProfile.value = newState.profile
        }
    }

    fun preparing(message: String = "正在准备...") {
        transitionTo(ProxyState.Preparing(message))
    }

    fun connecting(mode: RunningMode) {
        transitionTo(ProxyState.Connecting(mode))
    }

    fun running(profile: Profile, mode: RunningMode) {
        _currentProfile.value = profile
        transitionTo(ProxyState.Running(profile, mode))
    }

    fun stopping() {
        transitionTo(ProxyState.Stopping)
    }

    fun error(message: String, cause: Throwable? = null) {
        transitionTo(ProxyState.Error(message, cause))
    }

    fun idle() {
        transitionTo(ProxyState.Idle)
    }

    fun updateCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    fun updateTrafficNow(traffic: TrafficData) {
        _trafficNow.value = traffic
    }

    fun updateTrafficTotal(traffic: TrafficData) {
        _trafficTotal.value = traffic
    }

    fun updateTunnelState(state: TunnelState?) {
        _tunnelState.value = state
    }

    fun reset() {
        _currentProfile.value = null
        _proxyState.value = ProxyState.Idle
        _trafficNow.value = TrafficData.ZERO
        _trafficTotal.value = TrafficData.ZERO
        _tunnelState.value = null
    }
}