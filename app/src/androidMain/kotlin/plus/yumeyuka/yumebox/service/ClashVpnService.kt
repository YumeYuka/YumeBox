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

import android.content.Context
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import plus.yumeyuka.yumebox.clash.config.ClashConfiguration
import plus.yumeyuka.yumebox.clash.config.VpnRouteConfig
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.model.AccessControlMode
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.data.store.NetworkSettingsStorage
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import plus.yumeyuka.yumebox.service.delegate.ClashServiceDelegate
import plus.yumeyuka.yumebox.service.notification.ServiceNotificationManager
import timber.log.Timber

class ClashVpnService : VpnService() {

    companion object {
        private const val TAG = "ClashVpnService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val EXTRA_PROFILE_ID = "profile_id"

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettings: NetworkSettingsStorage by inject()

    private val delegate by lazy {
        ClashServiceDelegate(
            this, clashManager, profilesStore, appSettingsStorage,
            ServiceNotificationManager.VPN_CONFIG
        )
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null

    override fun onCreate() {
        super.onCreate()
        delegate.initialize()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ServiceNotificationManager.VPN_CONFIG.notificationId,
            delegate.notificationManager.create("正在连接...", "正在建立连接", false)
        )

        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    startVpn(profileId)
                } else {
                    Timber.tag(TAG).e("未提供配置文件 ID")
                    stopSelf()
                }
            }
            ACTION_STOP -> stopVpn()
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startVpn(profileId: String) {
        delegate.serviceScope.launch {
            try {
                val profile = delegate.loadProfileIfNeeded(
                    profileId = profileId,
                    willUseTunMode = true,
                    quickStart = true
                ).getOrElse { error ->
                    Timber.tag(TAG).e("配置加载失败: ${error.message}")
                    delegate.showErrorNotification("启动失败", error.message ?: "配置加载失败")
                    return@launch
                }

                vpnInterface = withContext(Dispatchers.IO) { establishVpnInterface() }
                    ?: run {
                        Timber.tag(TAG).e("VPN 接口建立失败")
                        delegate.showErrorNotification("启动失败", "无法建立 VPN 连接")
                        return@launch
                    }

                val pfd = vpnInterface!!
                val rawFd = pfd.detachFd()
                tunFd = rawFd

                runCatching { pfd.close() }
                vpnInterface = null

                val config = ClashConfiguration.TunConfig()
                val tunDns = if (networkSettings.dnsHijack.value) config.dns else "0.0.0.0"
                val tunConfig = config.copy(
                    stack = networkSettings.tunStack.value.name.lowercase(),
                    dns = tunDns,
                    dnsHijacking = networkSettings.dnsHijack.value
                )

                clashManager.startTunMode(
                    fd = rawFd,
                    config = tunConfig,
                    markSocket = { protect(it) }
                )

                delegate.startNotificationUpdate()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "VPN 启动失败")
                delegate.showErrorNotification("启动失败", e.message ?: "未知错误")
            }
        }
    }

    private fun establishVpnInterface(): ParcelFileDescriptor? = runCatching {
        val config = ClashConfiguration.TunConfig()
        Builder().apply {
            setSession("YumeBox VPN")
            setMtu(config.mtu)
            setBlocking(false)
            addAddress(config.gateway, 30)

            if (networkSettings.enableIPv6.value) {
                addAddress(VpnRouteConfig.TUN_GATEWAY6, VpnRouteConfig.TUN_SUBNET_PREFIX6)
            }

            if (networkSettings.bypassPrivateNetwork.value) {
                VpnRouteConfig.BYPASS_PRIVATE_ROUTES.forEach { cidr ->
                    val (addr, prefix) = VpnRouteConfig.parseCidr(cidr)
                    addRoute(addr, prefix)
                }
                if (networkSettings.enableIPv6.value) {
                    VpnRouteConfig.BYPASS_PRIVATE_ROUTES_V6.forEach { cidr ->
                        val (addr, prefix) = VpnRouteConfig.parseCidr(cidr)
                        addRoute(addr, prefix)
                    }
                }
                addRoute(config.dns, 32)
                if (networkSettings.enableIPv6.value) addRoute(VpnRouteConfig.TUN_DNS6, 128)
            } else {
                addRoute("0.0.0.0", 0)
                if (networkSettings.enableIPv6.value) addRoute("::", 0)
            }

            val accessControlPackages = networkSettings.accessControlPackages.value
            when (networkSettings.accessControlMode.value) {
                AccessControlMode.allow_all -> {}
                AccessControlMode.allow_selected -> {
                    (accessControlPackages + packageName).forEach { runCatching { addAllowedApplication(it) } }
                }
                AccessControlMode.reject_selected -> {
                    (accessControlPackages - packageName).forEach { runCatching { addDisallowedApplication(it) } }
                }
            }

            addDnsServer(config.dns)
            if (networkSettings.enableIPv6.value) addDnsServer(VpnRouteConfig.TUN_DNS6)
            if (networkSettings.allowBypass.value) allowBypass()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (networkSettings.systemProxy.value) {
                    val exclusionList = if (networkSettings.bypassPrivateNetwork.value) {
                        VpnRouteConfig.HTTP_PROXY_LOCAL_LIST + VpnRouteConfig.HTTP_PROXY_BLACK_LIST
                    } else {
                        VpnRouteConfig.HTTP_PROXY_BLACK_LIST
                    }
                    runCatching { setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", 7890, exclusionList)) }
                }
                setMetered(false)
            }
        }.establish()
    }.getOrNull()

    private fun stopVpn() {
        delegate.stopNotificationUpdate()
        clashManager.stop()
        tunFd = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        delegate.cleanup()
        super.onDestroy()
    }
}
