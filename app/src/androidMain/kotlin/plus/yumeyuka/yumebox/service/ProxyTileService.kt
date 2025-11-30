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

package plus.yumeyuka.yumebox.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import plus.yumeyuka.yumebox.MainActivity
import plus.yumeyuka.yumebox.R
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import plus.yumeyuka.yumebox.data.repository.ProxyConnectionService
import plus.yumeyuka.yumebox.domain.model.RunningMode
import timber.log.Timber

class ProxyTileService : TileService() {

    companion object {
        private const val TAG = "ProxyTileService"
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()

    private var serviceScope: CoroutineScope? = null
    private var stateObserverJob: Job? = null
    private var isOperating = false

    override fun onStartListening() {
        super.onStartListening()
        Timber.tag(TAG).d("开始监听磁贴状态")
        
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        startObservingState()
    }

    override fun onStopListening() {
        super.onStopListening()
        Timber.tag(TAG).d("停止监听磁贴状态")
        
        stateObserverJob?.cancel()
        stateObserverJob = null
        serviceScope?.cancel()
        serviceScope = null
    }

    override fun onClick() {
        super.onClick()
        Timber.tag(TAG).d("磁贴被点击")

        if (isOperating) {
            Timber.tag(TAG).d("正在操作中，忽略点击")
            return
        }

        val isRunning = clashManager.isRunning.value
        
        if (isRunning) {
            stopProxy()
        } else {
            startProxy()
        }
    }

    private fun startProxy() {
        val profile = profilesStore.getRecommendedProfile()
        
        if (profile == null) {
            Timber.tag(TAG).w("没有可用的配置文件，打开应用")
            openApp()
            return
        }

        isOperating = true
        updateTileState(TileState.CONNECTING)

        serviceScope?.launch(Dispatchers.IO) {
            try {
                val result = proxyConnectionService.prepareAndStart(profile.id)
                
                result.fold(
                    onSuccess = { intent ->
                        if (intent != null) {
                            Timber.tag(TAG).d("需要 VPN 授权，打开应用")
                            openApp()
                        }
                    },
                    onFailure = { error ->
                        Timber.tag(TAG).e(error, "启动代理失败")
                        updateTileState(TileState.DISCONNECTED)
                    }
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "启动代理异常")
                updateTileState(TileState.DISCONNECTED)
            } finally {
                isOperating = false
            }
        }
    }

    private fun stopProxy() {
        isOperating = true
        updateTileState(TileState.DISCONNECTING)

        serviceScope?.launch(Dispatchers.IO) {
            try {
                val currentMode = clashManager.runningMode.value
                proxyConnectionService.stop(currentMode)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "停止代理异常")
            } finally {
                isOperating = false
            }
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(intent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun startObservingState() {
        stateObserverJob = serviceScope?.launch {
            combine(
                clashManager.isRunning,
                clashManager.runningMode,
                profilesStore.profiles
            ) { isRunning, runningMode, profiles ->
                Triple(isRunning, runningMode, profiles)
            }.collect { (isRunning, _, profiles) ->
                val hasProfile = profiles.isNotEmpty()
                
                val state = when {
                    !hasProfile -> TileState.UNAVAILABLE
                    isRunning -> TileState.CONNECTED
                    else -> TileState.DISCONNECTED
                }
                
                if (!isOperating) {
                    updateTileState(state)
                }
            }
        }
    }

    private fun updateTileState(state: TileState) {
        val tile = qsTile ?: return

        when (state) {
            TileState.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "已连接"
                tile.subtitle = getRunningModeText()
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
            TileState.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "已断开"
                tile.subtitle = null
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
            TileState.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "连接中..."
                tile.subtitle = null
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
            TileState.DISCONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "断开中..."
                tile.subtitle = null
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
            TileState.UNAVAILABLE -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "未配置"
                tile.subtitle = "点击打开应用"
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
        }

        tile.updateTile()
    }

    private fun getRunningModeText(): String {
        return when (val mode = clashManager.runningMode.value) {
            is RunningMode.Tun -> "VPN 模式"
            is RunningMode.Http -> "HTTP 模式"
            is RunningMode.None -> ""
        }
    }

    private enum class TileState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        DISCONNECTING,
        UNAVAILABLE
    }
}
