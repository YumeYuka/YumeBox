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

package plus.yumeyuka.yumebox.common.util

import android.util.Log
import kotlinx.coroutines.delay
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.repository.ProxyConnectionService
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.data.store.NetworkSettingsStorage
import plus.yumeyuka.yumebox.data.store.ProfilesStore

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        proxyConnectionService: ProxyConnectionService,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        profilesStore: ProfilesStore,
        clashManager: ClashManager,
        isBootCompleted: Boolean = false
    ) {
        try {
            val automaticRestart = appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                Log.d(TAG, "自动启动已禁用")
                return
            }

            if (clashManager.isRunning.value) {
                Log.d(TAG, "代理已在运行，跳过自动启动")
                return
            }

            val profileId = getProfileToStart(profilesStore)
            if (profileId == null) {
                Log.w(TAG, "没有可用的配置文件，无法自动启动")
                return
            }

            if (isBootCompleted) {
                Log.d(TAG, "开机自启：延迟 3 秒后启动...")
                delay(3000)
            }

            val proxyMode = networkSettingsStorage.proxyMode.value
            Log.d(TAG, "自动启动代理: profileId=$profileId, mode=$proxyMode")
            
            val result = proxyConnectionService.startDirect(
                profileId = profileId,
                mode = proxyMode
            )
            
            if (result.isSuccess) {
                Log.d(TAG, "自动启动代理成功")
            } else {
                Log.e(TAG, "自动启动代理失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "自动启动代理失败: ${e.message}", e)
        }
    }

    private fun getProfileToStart(profilesStore: ProfilesStore): String? {
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            val lastUsedProfile = profilesStore.getAllProfiles().find { it.id == lastUsedId }
            if (lastUsedProfile != null) {
                Log.d(TAG, "使用上次使用的配置: ${lastUsedProfile.name}")
                return lastUsedId
            }
        }

        val enabledProfile = profilesStore.getAllProfiles().find { it.enabled }
        if (enabledProfile != null) {
            Log.d(TAG, "使用已启用的配置: ${enabledProfile.name}")
            return enabledProfile.id
        }

        val firstProfile = profilesStore.getAllProfiles().firstOrNull()
        if (firstProfile != null) {
            Log.d(TAG, "使用第一个配置: ${firstProfile.name}")
            return firstProfile.id
        }

        return null
    }
}
