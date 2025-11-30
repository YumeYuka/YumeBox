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

package plus.yumeyuka.yumebox.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import plus.yumeyuka.yumebox.MainActivity
import plus.yumeyuka.yumebox.R
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.common.util.formatSpeed
import plus.yumeyuka.yumebox.common.util.formatTotal
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.domain.model.TrafficData

class ServiceNotificationManager(
    private val service: Service,
    private val config: Config
) {
    private data class NotificationData(
        val now: TrafficData,
        val total: TrafficData,
        val currentProfile: Profile?,
        val showTraffic: Boolean
    )
    data class Config(
        val notificationId: Int,
        val channelId: String,
        val channelName: String,
        val stopAction: String
    )

    private val notificationManager: NotificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 Clash 服务运行状态"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun create(
        title: String,
        content: String,
        isConnected: Boolean,
        smallIconRes: Int = R.drawable.ic_logo_service
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            service, 0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            service, 1,
            Intent(service, service.javaClass).apply { action = config.stopAction },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIconRes)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isConnected)
            .apply {
                if (isConnected) {
                    addAction(android.R.drawable.ic_menu_close_clear_cancel, "断开", stopPendingIntent)
                }
            }
            .build()
    }

    fun update(title: String, content: String, isConnected: Boolean) {
        notificationManager.notify(config.notificationId, create(title, content, isConnected))
    }

    fun startTrafficUpdate(
        scope: CoroutineScope,
        clashManager: ClashManager,
        appSettings: AppSettingsStorage
    ): Job = scope.launch {
        combine(
            clashManager.trafficNow,
            clashManager.trafficTotal,
            clashManager.currentProfile,
            appSettings.showTrafficNotification.state
        ) { now, total, currentProfile, showTraffic ->
            NotificationData(now, total, currentProfile, showTraffic)
        }
            .collect { (now, total, currentProfile, showTraffic) ->
                val profileName = currentProfile?.name ?: "未知配置"
                if (showTraffic) {
                    val speedStr = "↓ ${formatSpeed(now.download)} ↑ ${formatSpeed(now.upload)}"
                    val totalStr = "总计: ${formatTotal(total.download + total.upload)}"
                    update("已连接: $profileName", "$speedStr | $totalStr", true)
                } else {
                    update("已连接", profileName, true)
                }
            }
    }

    companion object {
        val VPN_CONFIG = Config(
            notificationId = 1001,
            channelId = "clash_vpn_service",
            channelName = "Clash VPN Service",
            stopAction = "STOP"
        )

        val HTTP_CONFIG = Config(
            notificationId = 1002,
            channelId = "clash_http_service",
            channelName = "Clash HTTP Service",
            stopAction = "STOP"
        )
    }
}
