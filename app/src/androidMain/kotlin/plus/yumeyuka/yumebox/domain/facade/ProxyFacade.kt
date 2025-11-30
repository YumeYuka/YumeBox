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

package plus.yumeyuka.yumebox.domain.facade

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.core.model.LogMessage
import plus.yumeyuka.yumebox.core.model.TunnelState
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.repository.ProxyConnectionService
import plus.yumeyuka.yumebox.domain.model.ProxyGroupInfo
import plus.yumeyuka.yumebox.domain.model.ProxyState
import plus.yumeyuka.yumebox.domain.model.RunningMode
import plus.yumeyuka.yumebox.domain.model.TrafficData

class ProxyFacade(
    private val clashManager: ClashManager,
    private val proxyConnectionService: ProxyConnectionService
) {
    val proxyState: StateFlow<ProxyState> = clashManager.proxyState
    val isRunning: StateFlow<Boolean> = clashManager.isRunning
    val currentProfile: StateFlow<Profile?> = clashManager.currentProfile
    val trafficNow: StateFlow<TrafficData> = clashManager.trafficNow
    val trafficTotal: StateFlow<TrafficData> = clashManager.trafficTotal
    val tunnelState: StateFlow<TunnelState?> = clashManager.tunnelState
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyGroups
    val runningMode: StateFlow<RunningMode> = clashManager.runningMode
    val logs = clashManager.logs

    suspend fun startProxy(profileId: String, forceTunMode: Boolean? = null): Result<Intent?> {
        return proxyConnectionService.prepareAndStart(profileId, forceTunMode)
    }

    fun stopProxy() {
        proxyConnectionService.stop(runningMode.value)
    }

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false): Result<Unit> {
        return clashManager.refreshProxyGroups(skipCacheClear)
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean {
        return clashManager.selectProxy(groupName, proxyName)
    }

    fun testProxyDelay(groupName: String) {
        clashManager.testProxyDelay(groupName)
    }

    fun getCachedDelay(nodeName: String): Int? {
        return clashManager.getCachedDelay(nodeName)
    }
}
