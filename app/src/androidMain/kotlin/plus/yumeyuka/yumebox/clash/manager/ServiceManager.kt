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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import plus.yumeyuka.yumebox.core.Clash
import plus.yumeyuka.yumebox.core.model.TunnelState
import plus.yumeyuka.yumebox.clash.config.ClashConfiguration
import plus.yumeyuka.yumebox.common.util.SystemProxyHelper
import plus.yumeyuka.yumebox.domain.model.RunningMode
import android.content.Context
import timber.log.Timber
import java.net.InetSocketAddress

class ServiceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val stateManager: ProxyStateManager,
    private val proxyGroupManager: ProxyGroupManager
) {
    private var trafficMonitorJob: Job? = null

    suspend fun startTunMode(
        fd: Int,
        config: ClashConfiguration.TunConfig = ClashConfiguration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stateManager.connecting(RunningMode.Tun)
            
            ClashConfiguration.applyOverride(
                ClashConfiguration.ProxyMode.Tun
            )

            Clash.startTun(
                fd = fd,
                stack = config.stack,
                gateway = "${config.gateway}/30",
                portal = "${config.portal}/30",
                dns = config.dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            val profile = stateManager.currentProfile.value
                ?: throw IllegalStateException("Cannot start TUN mode without loaded profile")
            stateManager.running(profile, RunningMode.Tun)
            startMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "TUN 模式启动失败")
            stateManager.error("TUN 模式启动失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun startHttpMode(config: ClashConfiguration.HttpConfig = ClashConfiguration.HttpConfig()): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val profile = stateManager.currentProfile.value
            if (profile == null) {
                Timber.e("HTTP 代理启动失败 - 没有已加载的配置")
                return@withContext Result.failure(IllegalStateException("Cannot start HTTP mode without loaded profile"))
            }
            
            val httpMode = RunningMode.Http(config.address)
            stateManager.connecting(httpMode)
            
            ClashConfiguration.applyOverride(ClashConfiguration.ProxyMode.Http(config.port))
            val address = Clash.startHttp(config.listenAddress) ?: config.address
            stateManager.running(profile, RunningMode.Http(address))
            startMonitoring()
            Result.success(address)
        } catch (e: Exception) {
            Timber.e(e, "HTTP 代理启动失败")
            stateManager.error("HTTP 代理启动失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun stop() {
        runCatching {
            stateManager.stopping()
            when (val mode = stateManager.runningMode.value) {
                is RunningMode.Tun -> { Clash.stopTun(); Clash.reset() }
                is RunningMode.Http -> {
                    Clash.stopHttp()
                    Clash.reset()
                    SystemProxyHelper.clearSystemProxy(context)
                }
                RunningMode.None -> {}
            }
            stateManager.reset()
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        stopMonitoring()

        var proxyGroupRefreshCounter = 0

        trafficMonitorJob = scope.launch {
            while (isActive) {
                runCatching {
                    stateManager.updateTrafficNow(plus.yumeyuka.yumebox.domain.model.TrafficData.from(Clash.queryTrafficNow()))
                    stateManager.updateTrafficTotal(plus.yumeyuka.yumebox.domain.model.TrafficData.from(Clash.queryTrafficTotal()))
                    stateManager.updateTunnelState(Clash.queryTunnelState())

                    proxyGroupRefreshCounter++
                    if (proxyGroupRefreshCounter >= 60) {
                        proxyGroupManager.refreshProxyGroups(skipCacheClear = true, currentProfile = stateManager.currentProfile.value)
                        proxyGroupRefreshCounter = 0
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopMonitoring() {
        trafficMonitorJob?.cancel()
        trafficMonitorJob = null
    }
}