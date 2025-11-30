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

package plus.yumeyuka.yumebox.data.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.model.ProxyMode
import plus.yumeyuka.yumebox.data.store.NetworkSettingsStorage
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import plus.yumeyuka.yumebox.domain.model.RunningMode
import plus.yumeyuka.yumebox.service.ClashHttpService
import plus.yumeyuka.yumebox.service.ClashVpnService
import timber.log.Timber

class ProxyConnectionService(
    private val context: Context,
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore,
    private val networkSettingsStorage: NetworkSettingsStorage
) {
    companion object {
        private const val TAG = "ProxyConnectionService"
    }

    suspend fun prepareAndStart(
        profileId: String,
        forceTunMode: Boolean? = null
    ): Result<Intent?> {
        return try {
            Timber.tag(TAG).d("准备启动代理: profileId=$profileId, forceTunMode=$forceTunMode")

            val proxyMode = determineProxyMode(forceTunMode)
            Timber.tag(TAG).d("选定代理模式: $proxyMode")

            if (proxyMode == ProxyMode.Tun) {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    Timber.tag(TAG).d("需要 VPN 授权")
                    return Result.success(prepareIntent)
                }
            }

            startProxyInternal(profileId, proxyMode)

            Result.success(null)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动代理失败")
            Result.failure(e)
        }
    }

    suspend fun startDirect(
        profileId: String,
        mode: ProxyMode
    ): Result<Unit> {
        return try {
            Timber.tag(TAG).d("直接启动代理: profileId=$profileId, mode=$mode")
            
            val profile = profilesStore.getAllProfiles().find { it.id == profileId }
            if (profile == null) {
                Timber.tag(TAG).e("未找到配置文件: $profileId")
                return Result.failure(IllegalArgumentException("配置文件不存在"))
            }

            val currentProfile = clashManager.currentProfile.value
            if (currentProfile == null || currentProfile.id != profile.id) {
                Timber.tag(TAG).d("配置未预加载或不匹配，快速加载: ${profile.name}")
                val loadResult = clashManager.loadProfile(
                    profile = profile,
                    forceDownload = false,
                    willUseTunMode = (mode == ProxyMode.Tun),
                    quickStart = true
                )
                
                if (loadResult.isFailure) {
                    Timber.tag(TAG).e("配置加载失败: ${loadResult.exceptionOrNull()?.message}")
                    return Result.failure(loadResult.exceptionOrNull() ?: Exception("配置加载失败"))
                }
            } else {
                Timber.tag(TAG).d("配置已预加载，直接启动: ${profile.name}")
            }

            profilesStore.updateLastUsedProfileId(profileId)

            when (mode) {
                ProxyMode.Tun -> {
                    Timber.tag(TAG).d("启动 VPN 服务")
                    ClashVpnService.start(context, profileId)
                }
                ProxyMode.Http -> {
                    Timber.tag(TAG).d("启动 HTTP 服务")
                    ClashHttpService.start(context, profileId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动代理服务失败: ${e.message}")
            Result.failure(e)
        }
    }

    fun stop(currentMode: RunningMode) {
        try {
            Timber.tag(TAG).d("停止代理服务: mode=$currentMode")
            when (currentMode) {
                is RunningMode.Tun -> ClashVpnService.stop(context)
                is RunningMode.Http -> ClashHttpService.stop(context)
                is RunningMode.None -> Timber.tag(TAG).w("当前没有运行的代理服务")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "停止代理失败")
            throw e
        }
    }

    private fun determineProxyMode(forceTunMode: Boolean?): ProxyMode {
        return if (forceTunMode != null) {
            if (forceTunMode) ProxyMode.Tun else ProxyMode.Http
        } else {
            networkSettingsStorage.proxyMode.value
        }
    }

    private suspend fun startProxyInternal(profileId: String, proxyMode: ProxyMode) {
        Timber.tag(TAG).d("启动代理服务: profileId=$profileId, mode=$proxyMode")
        
        val result = startDirect(
            profileId = profileId,
            mode = proxyMode
        )

        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("启动服务失败")
        }
    }
}
