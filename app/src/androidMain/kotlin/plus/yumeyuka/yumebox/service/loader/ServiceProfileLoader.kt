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

package plus.yumeyuka.yumebox.service.loader

import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.model.ProfileType
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import timber.log.Timber

class ServiceProfileLoader(
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore
) {
    companion object {
        private const val TAG = "ServiceProfileLoader"
        private const val QUICK_START_THRESHOLD_MS = 2 * 60 * 60 * 1000L
    }
    
    suspend fun loadIfNeeded(
        profileId: String,
        willUseTunMode: Boolean,
        quickStart: Boolean = false
    ): Result<Profile> {
        val profile = profilesStore.getAllProfiles().find { it.id == profileId }
            ?: return Result.failure(ProfileNotFoundException(profileId))
        
        val currentProfile = clashManager.currentProfile.value
        if (currentProfile != null && currentProfile.id == profile.id) {
            Timber.tag(TAG).d("当前配置已加载: ${profile.name}")
            return Result.success(profile)
        }
        
        Timber.tag(TAG).d("正在加载配置: ${profile.name}")
        
        val useQuickStart = shouldUseQuickStart(profile, quickStart)
        val loadResult = clashManager.loadProfile(
            profile,
            forceDownload = false,
            willUseTunMode = willUseTunMode,
            quickStart = useQuickStart
        )
        
        return if (loadResult.isSuccess) {
            if (useQuickStart) {
                Timber.tag(TAG).d("使用快速启动模式，providers将在后台更新")
            }
            Result.success(profile)
        } else {
            Result.failure(
                ProfileLoadException(
                    profileId,
                    loadResult.exceptionOrNull()?.message ?: "未知错误"
                )
            )
        }
    }
    
    private fun shouldUseQuickStart(profile: Profile, requestQuickStart: Boolean): Boolean {
        if (!requestQuickStart) return false
        if (profile.type != ProfileType.FILE) return false
        
        val twoHoursAgo = System.currentTimeMillis() - QUICK_START_THRESHOLD_MS
        return profile.lastUpdatedAt?.let { it < twoHoursAgo } ?: false
    }
}

class ProfileNotFoundException(profileId: String) : Exception("未找到配置文件: $profileId")

class ProfileLoadException(profileId: String, reason: String) : Exception("配置加载失败 [$profileId]: $reason")
