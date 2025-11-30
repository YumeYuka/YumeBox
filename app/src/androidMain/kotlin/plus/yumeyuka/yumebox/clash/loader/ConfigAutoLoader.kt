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

package plus.yumeyuka.yumebox.clash.loader

import kotlinx.coroutines.*
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import plus.yumeyuka.yumebox.domain.model.RunningMode
import timber.log.Timber

class ConfigAutoLoader(
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore
) {
    companion object { private const val TAG = "ConfigAutoLoader" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun getRecommendedProfile(): plus.yumeyuka.yumebox.data.model.Profile? {
        val allProfiles = profilesStore.getAllProfiles()
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            allProfiles.find { it.id == lastUsedId }?.let { return it }
        }
        allProfiles.find { it.enabled }?.let { return it }
        return allProfiles.firstOrNull()
    }

    suspend fun reloadConfig(profileId: String? = null): Result<String> {
        return try {
            val profile = if (profileId != null) {
                profilesStore.getAllProfiles().find { it.id == profileId }
            } else {
                getRecommendedProfile()
            }

            if (profile == null) return Result.failure(IllegalArgumentException("配置不存在"))

            Timber.tag(TAG).d("手动重新加载配置: ${profile.name}")

            val willUseTun = clashManager.runningMode.value is RunningMode.Tun
            val result = clashManager.loadProfile(
                profile = profile,
                forceDownload = false,
                willUseTunMode = willUseTun
            )

            if (result.isSuccess) {
                delay(1000)
                clashManager.refreshProxyGroups()
                Timber.tag(TAG).d("配置重新加载成功")
            }

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "手动重新加载配置失败: ${e.message}")
            Result.failure(e)
        }
    }

    fun cleanup() { scope.cancel(); Timber.tag(TAG).d("ConfigAutoLoader 已清理") }

    suspend fun loadProfileIfNeeded(
        profileId: String,
        skipConfigLoad: Boolean,
        willUseTunMode: Boolean,
        quickStart: Boolean = false
    ): Result<plus.yumeyuka.yumebox.data.model.Profile> {
        return try {
            val profile = profilesStore.getAllProfiles().find { it.id == profileId }
                ?: return Result.failure(Exception("未找到配置文件: $profileId"))

            Timber.tag(TAG).d("正在加载配置: ${profile.name}")

            val useQuickStart = if (quickStart && profile.type == plus.yumeyuka.yumebox.data.model.ProfileType.FILE) {
                val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000
                profile.lastUpdatedAt?.let { it < twoHoursAgo } ?: false
            } else {
                false
            }

            val result = clashManager.loadProfile(
                profile,
                forceDownload = false,
                willUseTunMode = willUseTunMode,
                quickStart = useQuickStart
            )

            if (result.isFailure) {
                return Result.failure(Exception("配置加载失败: ${result.exceptionOrNull()?.message}"))
            }

            if (useQuickStart) {
                Timber.tag(TAG).d("使用快速启动模式，providers将在后台更新")
            }

            Result.success(profile)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "加载配置失败: ${e.message}")
            Result.failure(e)
        }
    }
}
