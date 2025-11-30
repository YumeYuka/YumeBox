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

import android.content.Context
import android.net.ProxyInfo
import android.os.Build
import android.util.Log
import java.net.Proxy

object SystemProxyHelper {

    private const val TAG = "SystemProxyHelper"

    fun clearSystemProxy(context: Context) {
        try {
            Log.d(TAG, "清理系统代理设置")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                clearSystemProxyQ(context)
            } else {
                clearSystemProxyLegacy(context)
            }
            Log.d(TAG, "系统代理设置已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理系统代理失败: ${e.message}", e)
        }
    }

    @Suppress("NewApi")
    private fun clearSystemProxyQ(context: Context) {
        try {
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            if (proxyHost != null || proxyPort != null) {
                System.clearProperty("http.proxyHost")
                System.clearProperty("http.proxyPort")
                System.clearProperty("https.proxyHost")
                System.clearProperty("https.proxyPort")
                System.clearProperty("socksProxyHost")
                System.clearProperty("socksProxyPort")
                Log.d(TAG, "已清除系统属性中的代理设置")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android 10+ 代理清除失败: ${e.message}", e)
        }
    }

    private fun clearSystemProxyLegacy(context: Context) {
        try {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
            Log.d(TAG, "已清除旧版本系统属性中的代理设置")
        } catch (e: Exception) {
            Log.e(TAG, "旧版本代理清除失败: ${e.message}", e)
        }
    }

    fun hasActiveProxy(): Boolean {
        return try {
            val httpHost = System.getProperty("http.proxyHost")
            val httpsHost = System.getProperty("https.proxyHost")
            val socksHost = System.getProperty("socksProxyHost")

            !httpHost.isNullOrEmpty() || !httpsHost.isNullOrEmpty() || !socksHost.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentProxyInfo(): String {
        return try {
            val httpHost = System.getProperty("http.proxyHost")
            val httpPort = System.getProperty("http.proxyPort")
            val httpsHost = System.getProperty("https.proxyHost")
            val httpsPort = System.getProperty("https.proxyPort")
            val socksHost = System.getProperty("socksProxyHost")
            val socksPort = System.getProperty("socksProxyPort")

            buildString {
                if (!httpHost.isNullOrEmpty()) {
                    append("HTTP: $httpHost:$httpPort")
                }
                if (!httpsHost.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append("HTTPS: $httpsHost:$httpsPort")
                }
                if (!socksHost.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append("SOCKS: $socksHost:$socksPort")
                }
                if (isEmpty()) {
                    append("无代理设置")
                }
            }
        } catch (e: Exception) {
            "获取代理信息失败: ${e.message}"
        }
    }
}