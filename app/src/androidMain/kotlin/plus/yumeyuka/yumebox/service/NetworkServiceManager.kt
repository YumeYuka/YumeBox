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
import android.net.VpnService
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import plus.yumeyuka.yumebox.data.model.ProxyMode
import plus.yumeyuka.yumebox.data.model.TunStack
import plus.yumeyuka.yumebox.data.model.AccessControlMode
import plus.yumeyuka.yumebox.data.store.NetworkSettingsStorage
import plus.yumeyuka.yumebox.data.store.MMKVProvider
import com.tencent.mmkv.MMKV

class NetworkServiceManager(
    private val context: Context
) : DefaultLifecycleObserver {

    private val networkSettingsStorage = NetworkSettingsStorage(
        MMKVProvider().getMMKV("network_settings")
    )
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _serviceState = MutableStateFlow(ServiceState.Stopped)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _proxyMode = MutableStateFlow(ProxyMode.Tun)
    val proxyMode: StateFlow<ProxyMode> = _proxyMode.asStateFlow()

    private var currentServiceType: ServiceType? = null

    enum class ServiceState {
        Starting,
        Running,
        Stopping,
        Stopped
    }

    enum class ServiceType {
        VPN,
        HTTP_PROXY
    }

    fun startService(proxyMode: ProxyMode) {
        when (proxyMode) {
            ProxyMode.Tun -> startVpnService()
            ProxyMode.Http -> startProxyService()
        }
        _proxyMode.value = proxyMode
    }

    fun stopService() {
        stopCurrentService()
    }

    fun restartService() {
        val currentMode = _proxyMode.value
        stopService()
        startService(currentMode)
    }

    private fun startVpnService() {
        if (_serviceState.value == ServiceState.Running && currentServiceType == ServiceType.VPN) {
            return
        }

        scope.launch {
            try {
                _serviceState.value = ServiceState.Starting
                stopCurrentService()

                val vpnIntent = VpnService.prepare(context)
                if (vpnIntent != null) {
                    _serviceState.value = ServiceState.Stopped
                    return@launch
                }

                val intent = Intent(context, ClashVpnService::class.java).apply {
                    action = ClashVpnService.ACTION_START
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                currentServiceType = ServiceType.VPN
                _serviceState.value = ServiceState.Running
            } catch (e: Exception) {
                _serviceState.value = ServiceState.Stopped
                currentServiceType = null
            }
        }
    }

    private fun startProxyService() {
        if (_serviceState.value == ServiceState.Running && currentServiceType == ServiceType.HTTP_PROXY) {
            return
        }

        scope.launch {
            try {
                _serviceState.value = ServiceState.Starting
                stopCurrentService()

                val intent = Intent(context, ClashHttpService::class.java).apply {
                    action = ClashHttpService.ACTION_START
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                currentServiceType = ServiceType.HTTP_PROXY
                _serviceState.value = ServiceState.Running
            } catch (e: Exception) {
                _serviceState.value = ServiceState.Stopped
                currentServiceType = null
            }
        }
    }

    private fun stopCurrentService() {
        if (_serviceState.value == ServiceState.Stopped) {
            return
        }

        scope.launch {
            try {
                _serviceState.value = ServiceState.Stopping

                val serviceClass = when (currentServiceType) {
                    ServiceType.VPN -> ClashVpnService::class.java
                    ServiceType.HTTP_PROXY -> ClashHttpService::class.java
                    null -> null
                }

                serviceClass?.let { clazz ->
                    val intent = Intent(context, clazz).apply {
                        action = when (currentServiceType) {
                            ServiceType.VPN -> ClashVpnService.ACTION_STOP
                            ServiceType.HTTP_PROXY -> ClashHttpService.ACTION_STOP
                            else -> null
                        }
                    }
                    context.stopService(intent)
                }

                currentServiceType = null
                _serviceState.value = ServiceState.Stopped
            } catch (e: Exception) {
                _serviceState.value = ServiceState.Stopped
                currentServiceType = null
            }
        }
    }

    fun getCurrentServiceConfig(): ServiceConfig {
        return ServiceConfig(
            bypassPrivateNetwork = networkSettingsStorage.bypassPrivateNetwork.value,
            dnsHijacking = networkSettingsStorage.dnsHijack.value,
            allowBypass = networkSettingsStorage.allowBypass.value,
            allowIpv6 = networkSettingsStorage.enableIPv6.value,
            systemProxy = networkSettingsStorage.systemProxy.value,
            tunStackMode = networkSettingsStorage.tunStack.value.name,
            accessControlMode = networkSettingsStorage.accessControlMode.value
        )
    }

    fun updateServiceConfig(config: ServiceConfig) {
        networkSettingsStorage.bypassPrivateNetwork.set(config.bypassPrivateNetwork)
        networkSettingsStorage.dnsHijack.set(config.dnsHijacking)
        networkSettingsStorage.allowBypass.set(config.allowBypass)
        networkSettingsStorage.enableIPv6.set(config.allowIpv6)
        networkSettingsStorage.systemProxy.set(config.systemProxy)
        networkSettingsStorage.tunStack.set(
            runCatching { TunStack.valueOf(config.tunStackMode) }.getOrDefault(TunStack.System)
        )
        networkSettingsStorage.accessControlMode.set(config.accessControlMode)

        if (_serviceState.value == ServiceState.Running) {
            restartService()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopService()
        super.onDestroy(owner)
    }

    data class ServiceConfig(
        val bypassPrivateNetwork: Boolean,
        val dnsHijacking: Boolean,
        val allowBypass: Boolean,
        val allowIpv6: Boolean,
        val systemProxy: Boolean,
        val tunStackMode: String,
        val accessControlMode: AccessControlMode
    )
}