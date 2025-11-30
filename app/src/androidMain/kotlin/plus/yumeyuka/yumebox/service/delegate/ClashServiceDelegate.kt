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

package plus.yumeyuka.yumebox.service.delegate

import android.app.Service
import kotlinx.coroutines.*
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import plus.yumeyuka.yumebox.service.loader.ServiceProfileLoader
import plus.yumeyuka.yumebox.service.notification.ServiceNotificationManager

class ClashServiceDelegate(
    private val service: Service,
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore,
    private val appSettingsStorage: AppSettingsStorage,
    private val notificationConfig: ServiceNotificationManager.Config
) {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val profileLoader by lazy { ServiceProfileLoader(clashManager, profilesStore) }
    val notificationManager by lazy { ServiceNotificationManager(service, notificationConfig) }
    
    private var notificationJob: Job? = null
    var currentProfileId: String? = null
        private set

    fun initialize() {
        notificationManager.createChannel()
    }

    suspend fun loadProfileIfNeeded(
        profileId: String,
        willUseTunMode: Boolean,
        quickStart: Boolean = false
    ): Result<Profile> {
        return profileLoader.loadIfNeeded(profileId, willUseTunMode, quickStart)
            .onSuccess { currentProfileId = profileId }
    }

    fun showErrorNotification(title: String, content: String) {
        service.startForeground(
            notificationConfig.notificationId,
            notificationManager.create(title, content, false)
        )
        serviceScope.launch {
            delay(3000)
            service.stopSelf()
        }
    }

    fun startNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = notificationManager.startTrafficUpdate(
            serviceScope, clashManager, appSettingsStorage
        )
    }

    fun stopNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = null
    }

    fun cleanup() {
        stopNotificationUpdate()
        currentProfileId = null
        serviceScope.cancel()
    }
}
